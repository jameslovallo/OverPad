package com.example.overlay

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.example.OperPadApplication

/**
 * Zero-UI translucent trampoline Activity used only as an Android 14+ fallback if a device's
 * Quick Settings tile click is blocked from starting the foreground service directly.
 */
class QuickOverlayToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.canDrawOverlays(this)) {
            val app = application as? OperPadApplication
            app?.shizukuBridge?.refreshStatusAndAutoConnect()
            OperPadOverlayService.start(this)
        }
        finish()
        disableTransitions()
    }

    private fun disableTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
