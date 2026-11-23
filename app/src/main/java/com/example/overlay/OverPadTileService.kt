package com.example.overlay

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.OperPadApplication
import com.example.R

/**
 * Notification-pane Quick Settings tile to toggle the OverPad virtual controller
 * directly without leaving an active game or cloud-streaming session.
 */
class OverPadTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? OperPadApplication
        val isRunning = app?.overlayState?.value?.isRunning == true

        if (isRunning) {
            app?.shizukuBridge?.setOverlayActive(false)
            OperPadOverlayService.stop(this)
            updateTileState(active = false, subtitleText = "Off")
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launchAndCollapse(permIntent, requestCode = 10)
            return
        }

        val startedDirectly = runCatching {
            app?.shizukuBridge?.refreshStatusAndAutoConnect()
            OperPadOverlayService.start(this)
        }.isSuccess

        if (startedDirectly) {
            val presetName = app?.overlayState?.value?.activePreset?.name ?: "Active"
            updateTileState(active = true, subtitleText = presetName)
        } else {
            // Fallback for Android 14+ OEMs that restrict FGS start directly from TileService
            val toggleIntent = Intent(this, QuickOverlayToggleActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            launchAndCollapse(toggleIntent, requestCode = 11)
        }
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        val app = application as? OperPadApplication
        val runtime = app?.overlayState?.value
        val isRunning = runtime?.isRunning == true
        val canDraw = Settings.canDrawOverlays(this)

        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_gamepad)
        tile.state = when {
            !canDraw -> Tile.STATE_INACTIVE
            isRunning -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !canDraw -> "Setup needed"
                isRunning -> runtime?.activePreset?.name ?: "Active"
                else -> "Off"
            }
        }
        tile.contentDescription = if (isRunning) {
            "Disable OverPad virtual controller"
        } else {
            "Enable OverPad virtual controller"
        }
        tile.updateTile()
    }

    private fun updateTileState(active: Boolean, subtitleText: String) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_gamepad)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitleText
        }
        tile.updateTile()
    }

    private fun launchAndCollapse(intent: Intent, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
