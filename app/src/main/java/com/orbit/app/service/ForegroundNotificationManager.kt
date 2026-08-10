package com.orbit.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.orbit.app.R

/**
 * Creates and manages the foreground service notification.
 * Channel: orbit_overlay, IMPORTANCE_LOW, no sound/vibrate/badge.
 */
class ForegroundNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "orbit_overlay"
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_NAME = "Orbit Overlay"
        private const val CHANNEL_DESCRIPTION = "Keeps Orbit's floating overlay running"
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(): Notification {
        val stopIntent = Intent(OrbitOverlayService.ACTION_STOP_OVERLAY).apply {
            setPackage(context.packageName)
            setClass(context, OrbitOverlayService::class.java)
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Orbit Active")
            .setContentText("Tap bubble to capture clipboard")
            .setSmallIcon(R.drawable.ic_orbit_notification)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_orbit_notification,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
}
