package com.example.overlay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.MainActivity
import com.example.R

object OverlayNotificationHelper {
    const val CHANNEL_ID = "operpad_overlay_channel"
    const val NOTIFICATION_ID = 4107

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OverPad Controller",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quick toggle and active status for the OverPad virtual controller."
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PermissionChecker.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun buildActiveNotification(context: Context, presetName: String): Notification {
        ensureNotificationChannel(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, OperPadOverlayService::class.java).apply {
                action = OperPadOverlayService.ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_gamepad)
            .setContentTitle("OverPad Active")
            .setContentText(presetName)
            .setContentIntent(openAppIntent)
            .addAction(0, "Disable", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildStandbyNotification(context: Context, presetName: String): Notification {
        ensureNotificationChannel(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val startServiceIntent = Intent(context, OperPadOverlayService::class.java).apply {
            action = OperPadOverlayService.ACTION_START
        }
        val enableIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                2,
                startServiceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                context,
                2,
                startServiceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_gamepad)
            .setContentTitle("OverPad Ready")
            .setContentText(presetName)
            .setContentIntent(openAppIntent)
            .addAction(0, "Enable", enableIntent)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateActiveNotificationIfPossible(context: Context, presetName: String) {
        if (!hasNotificationPermission(context)) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            nm.notify(NOTIFICATION_ID, buildActiveNotification(context, presetName))
        }
    }

    fun syncStandbyNotification(
        context: Context,
        isOverlayRunning: Boolean,
        presetName: String,
        quickActionEnabled: Boolean
    ) {
        if (isOverlayRunning) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (quickActionEnabled && Settings.canDrawOverlays(context) && hasNotificationPermission(context)) {
            runCatching {
                nm.notify(NOTIFICATION_ID, buildStandbyNotification(context, presetName))
            }
        } else {
            runCatching {
                nm.cancel(NOTIFICATION_ID)
            }
        }
    }

    fun requestTileUpdate(context: Context) {
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, OverPadTileService::class.java)
            )
        }
    }
}
