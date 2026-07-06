package com.orbit.app.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 023 FR-023-002 — the sanitizer must never leak exception
 * messages (they can embed user content) and must bound record size.
 */
class CrashJournalSanitizerTest {

    @Test
    fun messagesAreDroppedEntirely() {
        val secret = "SELECT textContent FROM intent_envelope WHERE id='user-secret'"
        val t = IllegalStateException(secret, IllegalArgumentException("inner $secret"))
        val record = CrashJournal.sanitize(
            processName = "com.orbit.app:ml",
            threadName = "main",
            throwable = t,
            nowMillis = 1_700_000_000_000L,
            appVersion = "0.1-test",
        )
        assertFalse("record must not contain exception messages", record.contains("user-secret"))
        assertFalse(record.contains("SELECT"))
        assertTrue(record.contains("exception=java.lang.IllegalStateException"))
        assertTrue(record.contains("cause=java.lang.IllegalArgumentException"))
    }

    @Test
    fun framesAndCauseChainAreCapped() {
        var t: Throwable = RuntimeException("root")
        repeat(12) { t = RuntimeException("wrap $it", t) }
        val deep = Array(500) { StackTraceElement("C$it", "m", "F.kt", it) }
        t.stackTrace = deep

        val record = CrashJournal.sanitize("com.orbit.app", "worker", t, 0L, "v")
        val frameLines = record.lines().count { it.startsWith("  at ") }
        val causeLines = record.lines().count { it.startsWith("cause=") || it.startsWith("exception=") }
        assertTrue(
            "outermost frames capped at ${CrashJournal.MAX_FRAMES}, total bounded",
            frameLines <= CrashJournal.MAX_FRAMES * CrashJournal.MAX_CAUSE_DEPTH
        )
        assertEquals(CrashJournal.MAX_CAUSE_DEPTH, causeLines)
    }

    @Test
    fun formatIsDeterministicAndLineOriented() {
        val t = IllegalStateException("x")
        t.stackTrace = arrayOf(StackTraceElement("com.orbit.app.Foo", "bar", "Foo.kt", 42))
        val record = CrashJournal.sanitize("com.orbit.app:net", "binder-1", t, 123L, "1.0")
        val lines = record.lines()
        assertEquals("v=1", lines[0])
        assertEquals("at=123", lines[1])
        assertEquals("process=com.orbit.app:net", lines[2])
        assertEquals("thread=binder-1", lines[3])
        assertEquals("appVersion=1.0", lines[4])
        assertTrue(lines[5].startsWith("sdk="))
        assertEquals("exception=java.lang.IllegalStateException", lines[6])
        assertEquals("  at com.orbit.app.Foo.bar:42", lines[7])
    }
}
