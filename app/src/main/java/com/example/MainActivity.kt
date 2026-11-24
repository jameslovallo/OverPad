package com.example

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.ControlType
import com.example.overlay.OverPadOverlayService
import com.example.overlay.OverlayGridEditorScreen
import com.example.overlay.OverlaySettingsHud
import com.example.ui.OverPadViewModel
import com.example.ui.gamepad.FpsRightLookZoneSurface
import com.example.ui.gamepad.GamepadControlNode
import com.example.ui.gamepad.LeftMoveZoneSurface
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.SandboxScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuOnboardingWizard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OverPadTheme
import kotlin.math.roundToInt

enum class OverPadRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CONTROLS("controls", "Controls", Icons.Filled.Gamepad, Icons.Outlined.Gamepad, "nav_controls"),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

private const val ROUTE_EDITOR = "editor"
private const val ROUTE_SANDBOX = "sandbox"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncMultiWindowState(isCurrentlyInMultiWindow())
        setContent {
            OverPadTheme {
                OverPadAppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncMultiWindowState(isCurrentlyInMultiWindow())
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        syncMultiWindowState(isInMultiWindowMode)
    }

    private fun isCurrentlyInMultiWindow(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInMultiWindowMode else false
    }

    private fun syncMultiWindowState(inMultiWindow: Boolean) {
        val app = application as? OverPadApplication ?: return
        val changed = app.isAppInMultiWindow.value != inMultiWindow
        app.setAppInMultiWindow(inMultiWindow)
        if (changed && app.overlayState.value.isRunning) {
            OverPadOverlayService.refresh(this)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverPadAppRoot(viewModel: OverPadViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: OverPadRoute.CONTROLS.route
    val showTopLevelNav = currentRoute == OverPadRoute.CONTROLS.route ||
        currentRoute == OverPadRoute.SETTINGS.route

    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val loadedUserSettings by viewModel.loadedUserSettings.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val isAppInMultiWindow by viewModel.isAppInMultiWindow.collectAsStateWithLifecycle()
    val xInputState by viewModel.xInputState.collectAsStateWithLifecycle()
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val editingPresetId by viewModel.editingPresetId.collectAsStateWithLifecycle()
    val selectedControl by viewModel.selectedControl.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val showSplitViewOverlay = overlayState.isRunning && isAppInMultiWindow

    LaunchedEffect(backupStatus, currentRoute) {
        val status = backupStatus ?: return@LaunchedEffect
        if (currentRoute != OverPadRoute.SETTINGS.route) {
            snackbarHostState.showSnackbar(status.text)
            viewModel.clearBackupStatus()
        }
    }

    var showOnboardingManually by rememberSaveable { mutableStateOf(false) }
    var dismissedInitialOnboarding by rememberSaveable { mutableStateOf(false) }
    val shouldAutoShowOnboarding =
        loadedUserSettings?.let { !it.hasCompletedOnboarding } == true && !dismissedInitialOnboarding
    val showOnboarding = showOnboardingManually || shouldAutoShowOnboarding

    fun closeOnboarding() {
        dismissedInitialOnboarding = true
        showOnboardingManually = false
        viewModel.setOnboardingCompleted(true)
    }

    BackHandler(enabled = showOnboarding) {
        closeOnboarding()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showTopLevelNav && !useNavigationRail) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            OverPadRoute.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    if (showTopLevelNav && useNavigationRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            OverPadRoute.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = OverPadRoute.CONTROLS.route,
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(OverPadRoute.CONTROLS.route) {
                            ControlsScreen(
                                overlayState = overlayState,
                                presets = presets,
                                shizukuStatus = shizukuStatus,
                                hasOverlayPermission = hasOverlayPermission,
                                isAppInMultiWindow = isAppInMultiWindow,
                                onStartOverlay = { viewModel.startOverlay(context) },
                                onStopOverlay = { viewModel.stopOverlay(context) },
                                onSelectPreset = viewModel::selectActivePreset,
                                onCreatePreset = { basePreset, name ->
                                    viewModel.createAndSelectPreset(basePreset, name)
                                },
                                onEditCurrentPreset = { preset ->
                                    viewModel.selectPresetForEditing(preset.id)
                                    navController.navigate(ROUTE_EDITOR) {
                                        launchSingleTop = true
                                    }
                                },
                                onImportProfiles = { uri -> viewModel.importProfilesFromUri(context, uri) }
                            )
                        }

                        composable(OverPadRoute.SETTINGS.route) {
                            SettingsScreen(
                                presets = presets,
                                bindings = bindings,
                                installedApps = installedApps,
                                isLoadingApps = isLoadingApps,
                                userSettings = userSettings,
                                hasOverlayPermission = hasOverlayPermission,
                                hasUsagePermission = hasUsagePermission,
                                shizukuStatus = shizukuStatus,
                                driverCapability = viewModel.driverCapability,
                                backupStatus = backupStatus,
                                onOpenOnboarding = { showOnboardingManually = true },
                                onRequestShizukuPermission = viewModel::requestShizukuPermission,
                                onReconnectShizuku = viewModel::reconnectShizuku,
                                onOpenShizukuApp = { viewModel.openShizukuApp(context) },
                                onDownloadShizuku = { viewModel.openShizukuDownload(context) },
                                onToggleAutoSwitch = viewModel::setAutoSwitchByAppUsage,
                                onBindApp = viewModel::bindAppToPreset,
                                onUnbindApp = viewModel::unbindApp,
                                onRefreshApps = viewModel::loadInstalledApps,
                                onSetHaptics = viewModel::setHapticFeedback,
                                onSetUdpBridge = viewModel::setUdpBridge,
                                onSetNotificationQuickAction = viewModel::setNotificationQuickActionEnabled,
                                onExportProfiles = { uri -> viewModel.exportProfilesToUri(context, uri) },
                                onImportProfiles = { uri -> viewModel.importProfilesFromUri(context, uri) },
                                onDismissBackupStatus = viewModel::clearBackupStatus,
                                onRefreshPermissions = { viewModel.refreshPermissions(context) },
                                onOpenSandbox = {
                                    navController.navigate(ROUTE_SANDBOX) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_EDITOR) {
                            LayoutEditorScreen(
                                presets = presets,
                                editingPresetId = editingPresetId,
                                selectedControl = selectedControl,
                                antiDeadzone = userSettings.fpsAntiDeadzone,
                                onUpdateAntiDeadzone = viewModel::setFpsAntiDeadzone,
                                onSelectPresetToEdit = viewModel::selectPresetForEditing,
                                onSelectControl = viewModel::selectControl,
                                onUpdatePreset = viewModel::updatePreset,
                                onDuplicatePreset = viewModel::duplicatePreset,
                                onResetBuiltInPreset = viewModel::resetBuiltInPreset,
                                onDeleteCustomPreset = viewModel::deleteCustomPreset,
                                onExportPreset = { presetId, uri ->
                                    viewModel.exportSinglePresetToUri(context, presetId, uri)
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_SANDBOX) {
                            SandboxScreen(
                                presets = presets,
                                activePreset = overlayState.activePreset,
                                xInputState = xInputState,
                                xInputEngine = viewModel.xInputEngine,
                                hapticsEnabled = userSettings.hapticFeedbackEnabled,
                                antiDeadzone = userSettings.fpsAntiDeadzone,
                                onSelectPreset = viewModel::selectActivePreset,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        if (showSplitViewOverlay) {
            BackHandler {
                when {
                    overlayState.isOverlayEditMode -> viewModel.exitOverlayEditMode()
                    overlayState.isOverlaySettingsOpen -> viewModel.closeOverlaySettings()
                    else -> viewModel.stopOverlay(context)
                }
            }
            SplitViewOverlaySurface(
                overlayState = overlayState,
                presets = presets,
                userSettings = userSettings,
                viewModel = viewModel,
                onStopOverlay = { viewModel.stopOverlay(context) }
            )
        }

        if (showOnboarding) {
            ShizukuOnboardingWizard(
                shizukuStatus = shizukuStatus,
                hasOverlayPermission = hasOverlayPermission,
                hasUsagePermission = hasUsagePermission,
                onRefreshStatus = { viewModel.refreshPermissions(context) },
                onOpenShizuku = { viewModel.openShizukuApp(context) },
                onDownloadShizuku = { viewModel.openShizukuDownload(context) },
                onRequestShizukuPermission = viewModel::requestShizukuPermission,
                onReconnectBridge = viewModel::reconnectShizuku,
                onFinish = ::closeOnboarding
            )
        }
    }
}

@Composable
private fun SplitViewOverlaySurface(
    overlayState: OverlayRuntimeState,
    presets: List<com.example.model.ControllerPreset>,
    userSettings: com.example.data.OverPadUserSettings,
    viewModel: OverPadViewModel,
    onStopOverlay: () -> Unit
) {
    OverPadTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("split_view_overlay_surface")
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val canvasScale = minOf(maxWidth / 760.dp, maxHeight / 420.dp).coerceIn(0.65f, 1.0f)

                if (!overlayState.isOverlaySettingsOpen && !overlayState.isOverlayEditMode) {
                    LeftMoveZoneSurface(
                        preset = overlayState.activePreset,
                        engine = viewModel.xInputEngine,
                        showGuideOverlay = false,
                        hapticsEnabled = userSettings.hapticFeedbackEnabled,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                    )

                    FpsRightLookZoneSurface(
                        preset = overlayState.activePreset,
                        engine = viewModel.xInputEngine,
                        showGuideOverlay = false,
                        antiDeadzone = userSettings.fpsAntiDeadzone,
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd)
                    )

                    overlayState.activePreset.controls
                        .filter { it.visible || !it.type.canHide }
                        .forEach { placement ->
                            val scaled = placement.copy(scale = placement.scale * canvasScale)
                            val wPx = with(density) { scaled.effectiveWidthDp().dp.toPx() }
                            val hPx = with(density) { scaled.effectiveHeightDp().dp.toPx() }
                            val offsetX = (placement.xFraction * widthPx - wPx / 2f).roundToInt()
                            val offsetY = (placement.yFraction * heightPx - hPx / 2f).roundToInt()

                            Box(modifier = Modifier.offset { IntOffset(offsetX, offsetY) }) {
                                GamepadControlNode(
                                    placement = scaled,
                                    preset = overlayState.activePreset,
                                    engine = viewModel.xInputEngine,
                                    interactive = true,
                                    hapticsEnabled = userSettings.hapticFeedbackEnabled,
                                    antiDeadzone = userSettings.fpsAntiDeadzone,
                                    onSettingsClick = if (placement.type == ControlType.SETTINGS) {
                                        { viewModel.openOverlaySettings() }
                                    } else null
                                )
                            }
                        }
                }

                if (overlayState.isOverlaySettingsOpen) {
                    OverlaySettingsHud(
                        runtimeState = overlayState,
                        allPresets = presets,
                        antiDeadzone = userSettings.fpsAntiDeadzone,
                        onUpdateAntiDeadzone = viewModel::setFpsAntiDeadzone,
                        onSelectPreset = viewModel::selectActivePreset,
                        onBindForegroundApp = viewModel::bindOverlayAppToPreset,
                        onUpdatePreset = viewModel::updatePreset,
                        onEnterEditMode = viewModel::enterOverlayEditMode,
                        onOpenMainApp = onStopOverlay,
                        onStopOverlay = onStopOverlay,
                        onDismiss = viewModel::closeOverlaySettings
                    )
                }

                if (overlayState.isOverlayEditMode) {
                    OverlayGridEditorScreen(
                        preset = overlayState.activePreset,
                        onUpdatePreset = viewModel::updatePreset,
                        onFinishEdit = viewModel::exitOverlayEditMode
                    )
                }
            }
        }
    }
}
