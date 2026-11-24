package com.example.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.OverPadApplication
import com.example.model.ControlPlacement
import com.example.model.ControlType
import com.example.model.ControllerPreset
import com.example.model.DefaultPresets
import com.example.ui.gamepad.FpsRightLookZoneSurface
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.LeftMoveZoneSurface
import com.example.ui.theme.OverPadTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OverPadOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var app: OverPadApplication
    private val lifecycleOwner = OverlayLifecycleOwner()

    private val activeControlViews = mutableListOf<ComposeView>()
    private var orientationAnchorView: View? = null
    private var leftMoveZoneView: ComposeView? = null
    private var fpsZoneView: ComposeView? = null
    private var hudOverlayView: ComposeView? = null
    private var editOverlayView: ComposeView? = null

    private var usageMonitorJob: Job? = null
    private var orientationWaitJob: Job? = null
    private var orientationWaitTimedOut = false
    private var lastRenderedSignature: String? = null
    private var lastHudWindowSize: Pair<Int, Int>? = null
    private var lastEditWindowSize: Pair<Int, Int>? = null
    private var manualOverridePackage: String? = null
    private var cachedPresets: List<ControllerPreset> = DefaultPresets.all
    private var pendingPresetSave: ControllerPreset? = null
    private var presetSaveJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as OverPadApplication
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()

        startForegroundCompat(app.overlayState.value.activePreset.name)

        if (!Settings.canDrawOverlays(this) && !app.isAppInMultiWindow.value) {
            stopSelf()
            return
        }

        app.updateOverlayState { it.copy(isRunning = true, isOverlaySettingsOpen = false, isOverlayEditMode = false) }
        app.shizukuBridge.setOverlayActive(true)
        OverlayNotificationHelper.requestTileUpdate(this)

        serviceScope.launch {
            app.repository.presetsFlow.collect { list ->
                if (list.isNotEmpty()) {
                    cachedPresets = list
                    val state = app.overlayState.value
                    val hasPendingSave = pendingPresetSave != null || presetSaveJob?.isActive == true
                    if (!hasPendingSave && !state.isOverlayEditMode && !state.isOverlaySettingsOpen) {
                        val currentId = state.activePreset.id
                        val updated = list.firstOrNull { it.id == currentId }
                        if (updated != null && updated != state.activePreset) {
                            app.updateOverlayState { it.copy(activePreset = updated) }
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            var lastPresetName: String? = null
            combine(app.overlayState, app.isAppInMultiWindow) { state, inMultiWindow ->
                state to inMultiWindow
            }.collect { (state, inMultiWindow) ->
                syncOverlayWindows(
                    preset = state.activePreset,
                    isSettingsOpen = state.isOverlaySettingsOpen,
                    isEditMode = state.isOverlayEditMode,
                    isInMultiWindow = inMultiWindow
                )
                if (state.isRunning && state.activePreset.name != lastPresetName) {
                    lastPresetName = state.activePreset.name
                    OverlayNotificationHelper.updateActiveNotificationIfPossible(
                        this@OverPadOverlayService,
                        state.activePreset.name
                    )
                    OverlayNotificationHelper.requestTileUpdate(this@OverPadOverlayService)
                }
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
            ACTION_START -> {
                if (!Settings.canDrawOverlays(this) && !app.isAppInMultiWindow.value) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                app.shizukuBridge.refreshStatusAndAutoConnect()
                orientationWaitTimedOut = false
                orientationWaitJob?.cancel()
                lastRenderedSignature = null
                val state = app.overlayState.value
                syncOverlayWindows(
                    preset = state.activePreset,
                    isSettingsOpen = state.isOverlaySettingsOpen,
                    isEditMode = state.isOverlayEditMode,
                    isInMultiWindow = app.isAppInMultiWindow.value
                )
                OverlayNotificationHelper.requestTileUpdate(this)
            }
            ACTION_REFRESH -> {
                if (!Settings.canDrawOverlays(this) && !app.isAppInMultiWindow.value) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                lastRenderedSignature = null
                val state = app.overlayState.value
                syncOverlayWindows(
                    preset = state.activePreset,
                    isSettingsOpen = state.isOverlaySettingsOpen,
                    isEditMode = state.isOverlayEditMode,
                    isInMultiWindow = app.isAppInMultiWindow.value
                )
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationWaitJob?.cancel()
        orientationWaitJob = null
        lastRenderedSignature = null
        val state = app.overlayState.value
        syncOverlayWindows(
            preset = state.activePreset,
            isSettingsOpen = state.isOverlaySettingsOpen,
            isEditMode = state.isOverlayEditMode,
            isInMultiWindow = app.isAppInMultiWindow.value
        )
    }

    override fun onDestroy() {
        usageMonitorJob?.cancel()
        orientationWaitJob?.cancel()
        clearAllOverlayViews(removeOrientationAnchor = true)
        app.xInputEngine.resetAll()
        app.shizukuBridge.setOverlayActive(false)
        app.updateOverlayState {
            it.copy(
                isRunning = false,
                isOverlaySettingsOpen = false,
                isOverlayEditMode = false
            )
        }
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        app.syncQuickActionNotification()
        OverlayNotificationHelper.requestTileUpdate(this)
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
                        val binding = app.database.overPadDao().getBindingForPackage(currentPkg)
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
        isEditMode: Boolean,
        isInMultiWindow: Boolean = app.isAppInMultiWindow.value
    ) {
        if (isInMultiWindow) {
            orientationWaitJob?.cancel()
            orientationWaitJob = null
            orientationWaitTimedOut = false
            lastRenderedSignature = null
            clearAllOverlayViews(removeOrientationAnchor = true)
            return
        }

        if (!Settings.canDrawOverlays(this)) return

        ensureLandscapeOrientationAnchor()

        val (screenW, screenH) = getScreenDimensionsPx()
        if (screenW < screenH && !orientationWaitTimedOut) {
            clearAllOverlayViews(removeOrientationAnchor = false)
            lastRenderedSignature = null
            if (orientationWaitJob?.isActive != true) {
                orientationWaitJob = serviceScope.launch {
                    delay(350L)
                    orientationWaitTimedOut = true
                    lastRenderedSignature = null
                    val current = app.overlayState.value
                    syncOverlayWindows(
                        preset = current.activePreset,
                        isSettingsOpen = current.isOverlaySettingsOpen,
                        isEditMode = current.isOverlayEditMode,
                        isInMultiWindow = app.isAppInMultiWindow.value
                    )
                }
            }
            return
        }

        if (isEditMode) {
            val targetSize = screenW to screenH
            if (editOverlayView != null && lastEditWindowSize == targetSize) {
                return
            }
            lastRenderedSignature = null
            clearAllOverlayViews(removeOrientationAnchor = false)
            attachEditOverlay(screenW, screenH)
            return
        }

        if (isSettingsOpen) {
            val targetSize = screenW to screenH
            if (hudOverlayView != null && lastHudWindowSize == targetSize) {
                return
            }
            lastRenderedSignature = null
            clearAllOverlayViews(removeOrientationAnchor = false)
            attachSettingsHudWindow(screenW, screenH)
            return
        }

        val signature = buildString {
            append(preset.id).append('|')
            append(preset.opacity).append('|')
            append(preset.gridRows).append('|')
            append(preset.snapToGrid).append('|')
            append(preset.leftMoveZoneEnabled).append('|')
            append(preset.fpsRightZoneEnabled).append('|')
            append(preset.triggerDragAimEnabled).append('|')
            append(screenW).append('x').append(screenH).append('|')
            preset.controls.forEach {
                append(it.type.name).append(':').append(it.xFraction).append(',').append(it.yFraction)
                    .append(',').append(it.scale).append(',').append(it.visible)
                    .append(',').append(it.resolvedShape().name).append(',').append(it.toggleable).append(';')
            }
        }

        if (signature == lastRenderedSignature) return
        lastRenderedSignature = signature

        clearAllOverlayViews(removeOrientationAnchor = false)

        // 1. Optional Left-Side Tap-Anywhere Move Zone & Right-Side FPS Look Zone behind controls
        if (preset.leftMoveZoneEnabled) {
            attachLeftMoveZoneWindow(preset, screenW, screenH)
        }
        if (preset.fpsRightZoneEnabled) {
            attachFpsLookZoneWindow(preset, screenW, screenH)
        }

        // 2. Individual visible control windows (allows touches on empty space to pass through to underlying app)
        preset.controls.filter { it.visible || !it.type.canHide }.forEach { placement ->
            attachControlWindow(placement, preset, screenW, screenH)
        }
    }

    private fun ensureLandscapeOrientationAnchor() {
        if (orientationAnchorView != null) return
        val params = createOverlayLayoutParams(
            width = 1,
            height = 1,
            x = 0,
            y = 0
        ).apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        val anchor = View(this)
        runCatching {
            windowManager.addView(anchor, params)
            orientationAnchorView = anchor
        }
    }

    private fun attachLeftMoveZoneWindow(preset: ControllerPreset, screenW: Int, screenH: Int) {
        val halfW = screenW / 2
        val params = createOverlayLayoutParams(
            width = halfW,
            height = screenH,
            x = 0,
            y = 0
        )
        val view = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val settings by app.repository.userSettingsFlow.collectAsState(initial = null)
                OverPadTheme(darkTheme = true) {
                    LeftMoveZoneSurface(
                        preset = preset,
                        engine = app.xInputEngine,
                        showGuideOverlay = false,
                        hapticsEnabled = settings?.hapticFeedbackEnabled ?: true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        runCatching {
            windowManager.addView(view, params)
            leftMoveZoneView = view
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
                OverPadTheme(darkTheme = true) {
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
        val widthPx = (placement.effectiveWidthDp() * density).roundToInt().coerceAtLeast(1)
        val heightPx = (placement.effectiveHeightDp() * density).roundToInt().coerceAtLeast(1)
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
                OverPadTheme(darkTheme = true) {
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
                    onUpdatePreset = ::persistPresetFromOverlay,
                    onEnterEditMode = {
                        app.updateOverlayState {
                            it.copy(isOverlaySettingsOpen = false, isOverlayEditMode = true)
                        }
                    },
                    onOpenMainApp = {
                        app.updateOverlayState { it.copy(isOverlaySettingsOpen = false) }
                        val intent = Intent(this@OverPadOverlayService, MainActivity::class.java).apply {
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
            lastHudWindowSize = screenW to screenH
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
                    onUpdatePreset = ::persistPresetFromOverlay,
                    onFinishEdit = {
                        app.updateOverlayState { it.copy(isOverlayEditMode = false) }
                    }
                )
            }
        }
        runCatching {
            windowManager.addView(view, params)
            editOverlayView = view
            lastEditWindowSize = screenW to screenH
        }
    }

    private fun persistPresetFromOverlay(updated: ControllerPreset) {
        app.updateOverlayState { it.copy(activePreset = updated) }
        pendingPresetSave = updated
        if (presetSaveJob?.isActive != true) {
            presetSaveJob = serviceScope.launch {
                while (isActive) {
                    val next = pendingPresetSave ?: break
                    pendingPresetSave = null
                    app.repository.savePreset(next)
                }
            }
        }
    }

    private fun clearAllOverlayViews(removeOrientationAnchor: Boolean = true) {
        activeControlViews.forEach { runCatching { windowManager.removeViewImmediate(it) } }
        activeControlViews.clear()
        leftMoveZoneView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        leftMoveZoneView = null
        fpsZoneView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        fpsZoneView = null
        hudOverlayView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        hudOverlayView = null
        lastHudWindowSize = null
        editOverlayView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        editOverlayView = null
        lastEditWindowSize = null
        if (removeOrientationAnchor) {
            orientationAnchorView?.let { runCatching { windowManager.removeViewImmediate(it) } }
            orientationAnchorView = null
        }
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
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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

    private fun startForegroundCompat(presetName: String) {
        val notification = OverlayNotificationHelper.buildActiveNotification(this, presetName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                OverlayNotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(OverlayNotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_START = "com.example.overpad.START_OVERLAY"
        const val ACTION_STOP = "com.example.overpad.STOP_OVERLAY"
        const val ACTION_REFRESH = "com.example.overpad.REFRESH_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, OverPadOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverPadOverlayService::class.java))
        }

        fun refresh(context: Context) {
            val intent = Intent(context, OverPadOverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            runCatching { context.startService(intent) }
        }
    }
}
