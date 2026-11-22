package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.OperPadViewModel
import com.example.ui.screens.AppProfilesScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.SandboxScreen
import com.example.ui.screens.ShizukuOnboardingWizard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OperPadTheme
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextSecondary

enum class OperPadRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HUB("hub", "Hub", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_hub"),
    EDITOR("editor", "Studio", Icons.Filled.GridOn, Icons.Outlined.GridOn, "nav_studio"),
    APPS("apps", "Apps", Icons.Filled.Apps, Icons.Outlined.Apps, "nav_apps"),
    SANDBOX("sandbox", "Sandbox", Icons.Filled.Gamepad, Icons.Outlined.Gamepad, "nav_sandbox")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OperPadTheme(darkTheme = true) {
                OperPadAppRoot()
            }
        }
    }
}

@Composable
fun OperPadAppRoot(viewModel: OperPadViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: OperPadRoute.HUB.route

    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val loadedUserSettings by viewModel.loadedUserSettings.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val xInputState by viewModel.xInputState.collectAsStateWithLifecycle()
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val editingPresetId by viewModel.editingPresetId.collectAsStateWithLifecycle()
    val selectedControl by viewModel.selectedControl.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsStateWithLifecycle()

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

    fun navigateTo(route: String) {
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
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(containerColor = SlateSurface) {
                        OperPadRoute.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.16f)
                                )
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
            ) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = SlateSurface) {
                        OperPadRoute.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.16f)
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = OperPadRoute.HUB.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(OperPadRoute.HUB.route) {
                        HubScreen(
                            overlayState = overlayState,
                            presets = presets,
                            userSettings = userSettings,
                            hasOverlayPermission = hasOverlayPermission,
                            hasUsagePermission = hasUsagePermission,
                            shizukuStatus = shizukuStatus,
                            driverCapability = viewModel.driverCapability,
                            onStartOverlay = { viewModel.startOverlay(context) },
                            onStopOverlay = { viewModel.stopOverlay(context) },
                            onSelectPreset = viewModel::selectActivePreset,
                            onEditPreset = { preset ->
                                viewModel.selectPresetForEditing(preset.id)
                                navigateTo(OperPadRoute.EDITOR.route)
                            },
                            onOpenSandbox = { navigateTo(OperPadRoute.SANDBOX.route) },
                            onOpenOnboarding = { showOnboardingManually = true },
                            onRequestShizukuPermission = viewModel::requestShizukuPermission,
                            onReconnectShizuku = viewModel::reconnectShizuku,
                            onOpenShizukuApp = { viewModel.openShizukuApp(context) },
                            onDownloadShizuku = { viewModel.openShizukuDownload(context) },
                            onSetHaptics = viewModel::setHapticFeedback,
                            onSetUdpBridge = viewModel::setUdpBridge
                        )
                    }

                    composable(OperPadRoute.EDITOR.route) {
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
                            onDeleteCustomPreset = viewModel::deleteCustomPreset
                        )
                    }

                    composable(OperPadRoute.APPS.route) {
                        AppProfilesScreen(
                            installedApps = installedApps,
                            isLoadingApps = isLoadingApps,
                            presets = presets,
                            bindings = bindings,
                            autoSwitchEnabled = userSettings.autoSwitchByAppUsage,
                            hasUsagePermission = hasUsagePermission,
                            overlayState = overlayState,
                            onToggleAutoSwitch = viewModel::setAutoSwitchByAppUsage,
                            onBindApp = viewModel::bindAppToPreset,
                            onUnbindApp = viewModel::unbindApp,
                            onRefreshApps = viewModel::loadInstalledApps
                        )
                    }

                    composable(OperPadRoute.SANDBOX.route) {
                        SandboxScreen(
                            presets = presets,
                            activePreset = overlayState.activePreset,
                            xInputState = xInputState,
                            xInputEngine = viewModel.xInputEngine,
                            hapticsEnabled = userSettings.hapticFeedbackEnabled,
                            antiDeadzone = userSettings.fpsAntiDeadzone,
                            onSelectPreset = viewModel::selectActivePreset
                        )
                    }
                }
            }
        }
        }

        AnimatedVisibility(
            visible = showOnboarding,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
