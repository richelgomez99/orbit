package com.orbit.app.net

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.orbit.app.net.ipc.INetworkGateway

/**
 * Spec 022 — kicks off a BYOM model download by binding :net and calling
 * the oneway [INetworkGateway.startModelDownload]. The download itself
 * runs in :net and reports to [ModelDownloadStore]; this only fires the
 * request, then unbinds shortly after (the oneway call has been queued).
 *
 * Callers (a ViewModel, or the debug receiver) poll ModelDownloadStore
 * for progress — they do not hold this binding open.
 */
object ModelDownloadTrigger {

    private const val TAG = "ModelDownloadTrigger"

    fun start(context: Context, modelId: String, url: String, expectedBytes: Long = 0L) {
        val appCtx = context.applicationContext
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                runCatching {
                    INetworkGateway.Stub.asInterface(service)
                        .startModelDownload(modelId, url, expectedBytes)
                    Log.i(TAG, "startModelDownload dispatched id=$modelId")
                }.onFailure { Log.w(TAG, "startModelDownload dispatch failed", it) }
                // Intentionally hold the binding: unbinding now would let :net
                // be destroyed and cancel the multi-minute download coroutine
                // on its service scope. BIND_AUTO_CREATE keeps :net alive for
                // the app-process lifetime. The production model-store UI will
                // hold this via a ViewModel and unbind on COMPLETE/FAILED
                // (polled from ModelDownloadStore); this debug/one-shot path
                // lets the app session own it.
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }
        val bound = appCtx.bindService(
            Intent(appCtx, NetworkGatewayService::class.java),
            conn,
            Context.BIND_AUTO_CREATE,
        )
        if (!bound) Log.w(TAG, "bindService(NetworkGatewayService) failed for download")
    }
}
