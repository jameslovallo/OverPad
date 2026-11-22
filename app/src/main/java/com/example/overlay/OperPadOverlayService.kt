package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.OperPadApplication
import com.example.R
import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.ui.gamepad.FpsRightLookZoneSurface
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.theme.OperPadTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OperPadOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var app: OperPadApplication
    private val lifecycleOwner = OverlayLifecycleOwner()

    private val activeControlViews = mutableListOf<ComposeView>()
    private var fpsZoneView: ComposeView? = null
    private var hudOverlayView: ComposeView? = null
    private var editOverlayView: ComposeView? = null

    private var usageMonitorJob: Job? = null
    private var lastRenderedSignature: String? = null
    private var manualOverridePackage: String? = null
    private var cachedPresets: List<ControllerPreset> = DefaultPresets.all

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as OperPadApplication
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        createNotificationChannel()
        startForegroundCompat()

        app.updateOverlayState { it.copy(isRunning = true, isOverlaySettingsOpen = false, isOverlayEditMode = false) }

        serviceScope.launch {
            app.repository.presetsFlow.collect { list ->
                if (list.isNotEmpty()) {
                    cachedPresets = list
                    val currentId = app.overlayState.value.activePreset.id
                    val updated = list.firstOrNull { it.id == currentId }
                    if (updated != null && updated != app.overlayState.value.activePreset) {
                        app.updateOverlayState { it.copy(activePreset = updated) }
                    }
                }
            }
        }

        serviceScope.launch {
            app.overlayState.collect { state ->
                syncOverlayWindows(state.activePreset, state.isOverlaySettingsOpen, state.isOverlayEditMode)
            }
        }

        startUsagePolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> {
                lastRenderedSignature = null
                val state = app.overlayState.value
                syncOverlayWindows(state.activePreset, state.isOverlaySettingsOpen, state.isOverlayEditMode)
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lastRenderedSignature = null
        val state = app.overlayState.value
        syncOverlayWindows(state.activePreset, state.isOverlaySettingsOpen, state.isOverlayEditMode)
    }

    override fun onDestroy() {
        usageMonitorJob?.cancel()
        clearAllOverlayViews()
        app.xInputEngine.resetAll()
        app.updateOverlayState {
            it.copy(
                isRunning = false,
                isOverlaySettingsOpen = false,
                isOverlayEditMode = false
            )
        }
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startUsagePolling() {
        usageMonitorJob?.cancel()
        usageMonitorJob = serviceScope.launch {
            while (isActive) {
                val settings = app.repository.userSettingsFlow.first()
                if (settings.autoSwitchByAppUsage && app.foregroundAppMonitor.hasUsageAccessPermission()) {
                    val lastPkg = app.overlayState.value.detectedPackageName
                    val currentPkg = app.foregroundAppMonitor.detectCurrentForegroundPackage(lastPkg)
                    if (!currentPkg.isNullOrBlank() && currentPkg != packageName && currentPkg != lastPkg) {
                        manualOverridePackage = null
                        val label = app.foregroundAppMonitor.resolveAppLabel(currentPkg)
                        val binding = app.database.operPadDao().getBindingForPackage(currentPkg)
                        val targetPresetId = binding?.presetId ?: settings.defaultPresetId
                        val resolvedPreset = cachedPresets.firstOrNull { it.id == targetPresetId }
                            ?: app.overlayState.value.activePreset

                        app.updateOverlayState {
                            it.copy(
                                detectedPackageName = currentPkg,
                                detectedAppLabel = label,
                                autoMatchedByApp = binding != null,
                                activePreset = resolvedPreset
                            )
                        }
                    }
                }
                delay(800L)
            }
        }
    }

    private fun syncOverlayWindows(
        preset: ControllerPreset,
        isSettingsOpen: Boolean,
        isEditMode: Boolean
    ) {
        if (!Settings.canDrawOverlays(this)) return

        val (screenW, screenH) = getScreenDimensionsPx()
        val signature = buildString {
            append(preset.id).append('|')
            append(preset.opacity).append('|')
            append(preset.fpsRightZoneEnabled).append('|')
            append(preset.triggerDragAimEnabled).append('|')
            append(screenW).append('x').append(screenH).append('|')
            append(isSettingsOpen).append('|').append(isEditMode).append('|')
            preset.controls.forEach {
                append(it.type.name).append(':').append(it.xFraction).append(',').append(it.yFraction)
                    .append(',').append(it.scale).append(',').append(it.visible).append(';')
            }
        }

        if (signature == lastRenderedSignature) return
        lastRenderedSignature = signature

        clearAllOverlayViews()

        if (isEditMode) {
            attachEditOverlay(screenW, screenH)
            return
        }

        // 1. Optional Right-Side FPS Look Zone behind right-side controls
        if (preset.fpsRightZoneEnabled && !isSettingsOpen) {
            attachFpsLookZoneWindow(preset, screenW, screenH)
        }

        // 2. Individual visible control windows (allows touches on empty space to pass through to underlying app)
        if (!isSettingsOpen) {
            preset.controls.filter { it.visible || !it.type.canHide }.forEach { placement ->
                attachControlWindow(placement, preset, screenW, screenH)
            }
        }

        // 3. Optional Quick Settings HUD
        if (isSettingsOpen) {
            attachSettingsHudWindow(screenW, screenH)
        }
    }

    private fun attachFpsLookZoneWindow(preset: ControllerPreset, screenW: Int, screenH: Int) {
        val halfW = screenW / 2
        val params = createOverlayLayoutParams(
            width = halfW,
            height = screenH,
            x = halfW,
            y = 0
        )
        val view = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val settings by app.repository.userSettingsFlow.collectAsState(initial = null)
                OperPadTheme(darkTheme = true) {
                    FpsRightLookZoneSurface(
                        preset = preset,
                        engine = app.xInputEngine,
                        showGuideOverlay = false,
                        antiDeadzone = settings?.fpsAntiDeadzone
                            ?: com.example.xinput.TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        runCatching {
            windowManager.addView(view, params)
            fpsZoneView = view
        }
    }

    private fun attachControlWindow(
        placement: ControlPlacement,
        preset: ControllerPreset,
        screenW: Int,
        screenH: Int
    ) {
        val density = resources.displayMetrics.density
        val widthPx = (placement.type.baseWidthDp * placement.scale * density).roundToInt().coerceAtLeast(1)
        val heightPx = (placement.type.baseHeightDp * placement.scale * density).roundToInt().coerceAtLeast(1)
        val xPos = (placement.xFraction * screenW - widthPx / 2f).roundToInt()
        val yPos = (placement.yFraction * screenH - heightPx / 2f).roundToInt()

        val params = createOverlayLayoutParams(
            width = widthPx,
            height = heightPx,
            x = xPos,
            y = yPos
        )

        val view = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val settings by app.repository.userSettingsFlow.collectAsState(initial = null)
                OperPadTheme(darkTheme = true) {
                    GamepadControlNode(
                        placement = placement,
                        preset = preset,
                        engine = app.xInputEngine,
                        interactive = true,
                        hapticsEnabled = settings?.hapticFeedbackEnabled ?: true,
                        antiDeadzone = settings?.fpsAntiDeadzone
                            ?: com.example.xinput.TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
                        onSettingsClick = if (placement.type == ControlType.SETTINGS) {
                            {
                                app.xInputEngine.resetAll()
                                app.updateOverlayState { it.copy(isOverlaySettingsOpen = true) }
                            }
                        } else null
                    )
                }
            }
        }

        runCatching {
            windowManager.addView(view, params)
            activeControlViews.add(view)
        }
    }

    private fun attachSettingsHudWindow(screenW: Int, screenH: Int) {
        val params = createOverlayLayoutParams(
            width = screenW,
            height = screenH,
            x = 0,
            y = 0
        )
        val view = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val runtime by app.overlayState.collectAsState()
                val presets by app.repository.presetsFlow.collectAsState(initial = cachedPresets)
                val settings by app.repository.userSettingsFlow.collectAsState(initial = null)
                OverlaySettingsHud(
                    runtimeState = runtime,
                    allPresets = presets.ifEmpty { DefaultPresets.all },
                    antiDeadzone = settings?.fpsAntiDeadzone
                        ?: com.example.xinput.TouchMouseLookFilter.DEFAULT_ANTI_DEADZONE,
                    onUpdateAntiDeadzone = { boost ->
                        serviceScope.launch { app.repository.setFpsAntiDeadzone(boost) }
                    },
                    onSelectPreset = { selected ->
                        manualOverridePackage = runtime.detectedPackageName
                        app.updateOverlayState { it.copy(activePreset = selected, autoMatchedByApp = false) }
                        serviceScope.launch { app.repository.setDefaultPresetId(selected.id) }
                    },
                    onBindForegroundApp = { pkg, label, presetId ->
                        serviceScope.launch {
                            app.repository.bindAppToPreset(pkg, label, presetId)
                            app.updateOverlayState { it.copy(autoMatchedByApp = true) }
                        }
                    },
                    onUpdatePreset = { updated ->
                        app.updateOverlayState { it.copy(activePreset = updated) }
                        serviceScope.launch { app.repository.savePreset(updated) }
                    },
                    onEnterEditMode = {
                        app.updateOverlayState {
                            it.copy(isOverlaySettingsOpen = false, isOverlayEditMode = true)
                        }
                    },
                    onOpenMainApp = {
                        app.updateOverlayState { it.copy(isOverlaySettingsOpen = false) }
                        val intent = Intent(this@OperPadOverlayService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(intent)
                    },
                    onStopOverlay = { stopSelf() },
                    onDismiss = {
                        app.updateOverlayState { it.copy(isOverlaySettingsOpen = false) }
                    }
                )
            }
        }
        runCatching {
            windowManager.addView(view, params)
            hudOverlayView = view
        }
    }

    private fun attachEditOverlay(screenW: Int, screenH: Int) {
        val params = createOverlayLayoutParams(
            width = screenW,
            height = screenH,
            x = 0,
            y = 0
        )
        val view = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val runtime by app.overlayState.collectAsState()
                OverlayGridEditorScreen(
                    preset = runtime.activePreset,
                    onUpdatePreset = { updated ->
                        app.updateOverlayState { it.copy(activePreset = updated) }
                        serviceScope.launch { app.repository.savePreset(updated) }
                    },
                    onFinishEdit = {
                        app.updateOverlayState { it.copy(isOverlayEditMode = false) }
                    }
                )
            }
        }
        runCatching {
            windowManager.addView(view, params)
            editOverlayView = view
        }
    }

    private fun clearAllOverlayViews() {
        activeControlViews.forEach { runCatching { windowManager.removeViewImmediate(it) } }
        activeControlViews.clear()
        fpsZoneView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        fpsZoneView = null
        hudOverlayView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        hudOverlayView = null
        editOverlayView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        editOverlayView = null
    }

    private fun createOverlayLayoutParams(
        width: Int,
        height: Int,
        x: Int,
        y: Int
    ): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun getScreenDimensionsPx(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OverPad Controller Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the OverPad floating XInput controller active on top of games and apps."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun startForegroundCompat() {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OperPadOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("OverPad Overlay Active")
            .setContentText("Virtual XInput controller is running on top of apps.")
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "operpad_overlay_channel"
        private const val NOTIFICATION_ID = 4107
        const val ACTION_STOP = "com.example.operpad.STOP_OVERLAY"
        const val ACTION_REFRESH = "com.example.operpad.REFRESH_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, OperPadOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OperPadOverlayService::class.java))
        }
    }
}
