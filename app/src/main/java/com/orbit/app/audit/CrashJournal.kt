package com.orbit.app.audit

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Spec 023 Phase A — local, sanitized crash capture (FR-023-001..005).
 *
 * Installed in EVERY Orbit process from [com.orbit.app.OrbitApplication].
 * On an uncaught exception it best-effort writes a sanitized record to
 * the app's private files dir, then unconditionally delegates to the
 * previously installed handler so the platform crash flow is untouched.
 *
 * Sanitization contract: exception MESSAGES are dropped entirely — they
 * can embed user content (SQL text, file paths, clipboard fragments).
 * Only class names, code frames, thread/process names, and version
 * metadata are recorded. No database access on the crash path: the DB
 * may be the thing that crashed.
 */
object CrashJournal {

    private const val TAG = "CrashJournal"
    private const val DIR_NAME = "crash-journal"
    private const val REPORTED_SUFFIX = ".reported"
    internal const val MAX_FRAMES = 40
    internal const val MAX_CAUSE_DEPTH = 5
    internal const val MAX_RETAINED_RECORDS = 20

    /** Install the handler wrapper. Idempotent per process. */
    fun install(context: Context, processName: String) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is JournalingHandler) return
        Thread.setDefaultUncaughtExceptionHandler(
            JournalingHandler(context.applicationContext, processName, previous)
        )
    }

    private class JournalingHandler(
        private val context: Context,
        private val processName: String,
        private val delegate: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                writeRecord(context, processName, thread.name, throwable)
            } catch (_: Throwable) {
                // Best-effort only — never let journaling interfere with
                // the crash flow (FR-023-001 / stop sign).
            }
            delegate?.uncaughtException(thread, throwable)
        }
    }

    internal fun writeRecord(
        context: Context,
        processName: String,
        threadName: String,
        throwable: Throwable,
        nowMillis: Long = System.currentTimeMillis(),
    ): File {
        val dir = journalDir(context)
        pruneOldest(dir)
        val body = sanitize(processName, threadName, throwable, nowMillis, appVersion(context))
        val file = File(dir, "crash-$nowMillis-${processName.substringAfterLast(':')}.txt")
        file.writeText(body)
        return file
    }

    /**
     * FR-023-002 — deterministic, line-oriented, message-free record.
     * Format (one field per line, `key=value`, then FRAMES/CAUSE blocks).
     */
    internal fun sanitize(
        processName: String,
        threadName: String,
        throwable: Throwable,
        nowMillis: Long,
        appVersion: String,
    ): String = buildString {
        appendLine("v=1")
        appendLine("at=$nowMillis")
        appendLine("process=$processName")
        appendLine("thread=$threadName")
        appendLine("appVersion=$appVersion")
        appendLine("sdk=${Build.VERSION.SDK_INT}")
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            appendLine(if (depth == 0) "exception=${current.javaClass.name}" else "cause=${current.javaClass.name}")
            current.stackTrace.take(MAX_FRAMES).forEach { frame ->
                appendLine("  at ${frame.className}.${frame.methodName}:${frame.lineNumber}")
            }
            current = current.cause?.takeIf { it !== current }
            depth++
        }
    }

    /**
     * FR-023-004 — drain unreported records into CRASH_DETECTED audit
     * rows. Called from the default process on launch, off the main
     * thread. Returns the number of records drained.
     */
    suspend fun drainToAudit(
        context: Context,
        auditLogDao: com.orbit.app.data.dao.AuditLogDao,
        auditWriter: AuditLogWriter,
    ): Int {
        val dir = journalDir(context)
        val pending = dir.listFiles { f -> f.isFile && !f.name.endsWith(REPORTED_SUFFIX) }
            ?.sortedBy { it.name }
            .orEmpty()
        var drained = 0
        for (file in pending) {
            try {
                val body = file.readText()
                val fields = body.lineSequence()
                    .filter { '=' in it && !it.startsWith("  ") }
                    .associate { it.substringBefore('=') to it.substringAfter('=') }
                val process = fields["process"] ?: "unknown"
                val exceptionClass = fields["exception"] ?: "unknown"
                val frameCount = body.lineSequence().count { it.startsWith("  at ") }
                val digest = sha256(body)
                auditLogDao.insert(
                    auditWriter.build(
                        action = com.orbit.app.data.model.AuditAction.CRASH_DETECTED,
                        description = "App process crashed: $process — ${exceptionClass.substringAfterLast('.')}",
                        envelopeId = null,
                        extraJson = org.json.JSONObject().apply {
                            put("process", process)
                            put("exceptionClass", exceptionClass)
                            put("frames", frameCount)
                            put("recordDigest", digest)
                        }.toString()
                    )
                )
                file.renameTo(File(dir, file.name + REPORTED_SUFFIX))
                drained++
            } catch (t: Throwable) {
                Log.w(TAG, "failed to drain crash record ${file.name}", t)
            }
        }
        return drained
    }

    internal fun journalDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** FR-023-005 — keep at most [MAX_RETAINED_RECORDS] records on disk. */
    private fun pruneOldest(dir: File) {
        val files = dir.listFiles { f -> f.isFile }?.sortedBy { it.name }.orEmpty()
        val excess = files.size - (MAX_RETAINED_RECORDS - 1)
        if (excess > 0) files.take(excess).forEach { it.delete() }
    }

    private fun appVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Throwable) {
        "unknown"
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
