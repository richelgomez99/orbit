package com.orbit.app.ai.ipc

import android.app.ActivityManager
import android.content.Context
import android.os.Process

/** Spec 022 M1 — tiny process-identity check so the router can decide
 *  in-process engine (:ml) vs. AIDL proxy (everywhere else). */
object OrbitProcess {
    fun isMl(context: Context): Boolean = currentName(context) == "${context.packageName}:ml"

    private fun currentName(context: Context): String {
        val pid = Process.myPid()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return context.packageName
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
            ?: context.packageName
    }
}
