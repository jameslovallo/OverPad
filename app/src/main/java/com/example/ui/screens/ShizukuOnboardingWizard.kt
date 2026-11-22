package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.shizuku.ShizukuConnectionStage
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TriggerAmber

@Composable
fun ShizukuOnboardingWizard(
    shizukuStatus: ShizukuStatus,
    hasOverlayPermission: Boolean,
    hasUsagePermission: Boolean,
    onRefreshStatus: () -> Unit,
    onOpenShizuku: () -> Unit,
    onDownloadShizuku: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onReconnectBridge: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    val step1Done = shizukuStatus.isBinderAlive
    val step2Done = shizukuStatus.stage == ShizukuConnectionStage.CONNECTED
    val step3Done = hasOverlayPermission
    val step4Done = hasUsagePermission

    val completedCount = listOf(step1Done, step2Done, step3Done, step4Done).count { it }
    val progress by animateFloatAsState(
        targetValue = completedCount / 4f,
        label = "onboarding_progress"
    )
    val requiredReady = step2Done && step3Done

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        color = ObsidianBg
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONTROLLER BRIDGE SETUP",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Connect Shizuku via Wireless Debugging so games receive live gamepad input",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row {
                        IconButton(
                            onClick = onRefreshStatus,
                            modifier = Modifier.testTag("onboarding_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh status", tint = ElectricCyan)
                        }
                        IconButton(
                            onClick = onFinish,
                            modifier = Modifier.testTag("onboarding_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close setup", tint = TextSecondary)
                        }
                    }
                }

                // Progress Meter
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (completedCount == 4) "ALL SYSTEMS READY" else "SETUP PROGRESS",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (completedCount == 4) TacticalEmerald else TextPrimary
                            )
                            Text(
                                text = "$completedCount / 4 READY",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (completedCount == 4) TacticalEmerald else ElectricCyan
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (completedCount == 4) TacticalEmerald else ElectricCyan,
                            trackColor = SlateSurfaceElevated
                        )
                    }
                }

                // Scrollable Steps
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STEP 1: Install & Start Shizuku
                    OnboardingStepCard(
                        stepNumber = 1,
                        title = "Start Shizuku Service",
                        subtitle = when {
                            shizukuStatus.isBinderAlive -> "Shizuku service is active and reachable."
                            shizukuStatus.isShizukuInstalled -> "Shizuku is installed. Enable Wireless Debugging in Developer Options, pair in Shizuku, and tap Start."
                            else -> "Install Shizuku to inject XInput controller events into games without root or a PC."
                        },
                        icon = Icons.Default.Usb,
                        isDone = step1Done,
                        isActive = !step1Done
                    ) {
                        if (!shizukuStatus.isShizukuInstalled) {
                            Button(
                                onClick = onDownloadShizuku,
                                modifier = Modifier.testTag("onboarding_get_shizuku_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Get Shizuku")
                            }
                        } else if (!shizukuStatus.isBinderAlive) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ObsidianBg,
                                    border = BorderStroke(1.dp, HairlineBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "Quick Wireless Debugging Steps:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricCyan
                                        )
                                        Text(
                                            "1. Open Developer Options and enable Wireless Debugging.\n" +
                                                "2. Open Shizuku -> Pairing -> enter the pairing code.\n" +
                                                "3. Tap Start in Shizuku, then return to OverPad.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onOpenShizuku,
                                        modifier = Modifier.testTag("onboarding_open_shizuku_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Open Shizuku")
                                    }
                                    OutlinedButton(
                                        onClick = { openDeveloperOptions(context) },
                                        modifier = Modifier.testTag("onboarding_dev_options_button")
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Dev Options")
                                    }
                                }
                            }
                        }
                    }

                    // STEP 2: Grant Shizuku Permission & Bind Injector
                    OnboardingStepCard(
                        stepNumber = 2,
                        title = "Authorize OverPad Bridge",
                        subtitle = when (shizukuStatus.stage) {
                            ShizukuConnectionStage.CONNECTED -> shizukuStatus.modeLabel
                            ShizukuConnectionStage.CONNECTING -> "Permission granted — binding privileged XInput injector..."
                            ShizukuConnectionStage.PERMISSION_REQUIRED -> "Allow OverPad to inject gamepad key & thumbstick events via Shizuku."
                            else -> "Waiting for Shizuku service in Step 1."
                        },
                        icon = Icons.Default.Security,
                        isDone = step2Done,
                        isActive = step1Done && !step2Done
                    ) {
                        if (shizukuStatus.isBinderAlive && !shizukuStatus.isPermissionGranted) {
                            Button(
                                onClick = onRequestShizukuPermission,
                                modifier = Modifier.testTag("onboarding_grant_shizuku_button")
                            ) {
                                Text("Authorize OverPad")
                            }
                        } else if (shizukuStatus.isBinderAlive && shizukuStatus.isPermissionGranted && !shizukuStatus.isServiceConnected) {
                            OutlinedButton(
                                onClick = onReconnectBridge,
                                modifier = Modifier.testTag("onboarding_bind_shizuku_button")
                            ) {
                                Text("Connect Injector Service")
                            }
                        }
                        shizukuStatus.lastError?.let { err ->
                            Text(err, style = MaterialTheme.typography.labelSmall, color = TriggerAmber)
                        }
                    }

                    // STEP 3: Display Over Other Apps
                    OnboardingStepCard(
                        stepNumber = 3,
                        title = "Display Over Other Apps",
                        subtitle = if (step3Done) {
                            "Overlay permission granted."
                        } else {
                            "Allows OverPad to float thumbsticks, triggers, and FPS touch-look on top of games."
                        },
                        icon = Icons.Default.Layers,
                        isDone = step3Done,
                        isActive = step2Done && !step3Done
                    ) {
                        if (!step3Done) {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("onboarding_grant_overlay_button")
                            ) {
                                Text("Enable Overlay")
                            }
                        }
                    }

                    // STEP 4: Usage Access (Optional)
                    OnboardingStepCard(
                        stepNumber = 4,
                        title = "App Usage Auto-Switch (Optional)",
                        subtitle = if (step4Done) {
                            "Usage Access enabled — saved presets will auto-load per game."
                        } else {
                            "Lets OverPad detect which game is running and switch to its saved layout automatically."
                        },
                        icon = Icons.Default.QueryStats,
                        isDone = step4Done,
                        isActive = step3Done && !step4Done
                    ) {
                        if (!step4Done) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                },
                                modifier = Modifier.testTag("onboarding_grant_usage_button")
                            ) {
                                Text("Enable Usage Access")
                            }
                        }
                    }
                }

                // Bottom Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(if (requiredReady) "Done" else "Skip for Now", color = TextSecondary)
                    }
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("onboarding_continue_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (requiredReady) TacticalEmerald else ElectricCyan,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (requiredReady) "Launch OverPad Ready" else "Continue to Hub",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepCard(
    stepNumber: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDone: Boolean,
    isActive: Boolean,
    actionContent: @Composable () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isDone -> TacticalEmerald.copy(alpha = 0.65f)
            isActive -> ElectricCyan
            else -> HairlineBorder
        },
        label = "step_border"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> TacticalEmerald.copy(alpha = 0.2f)
                            isActive -> ElectricCyan.copy(alpha = 0.2f)
                            else -> SlateSurfaceElevated
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = TacticalEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) ElectricCyan else TextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDone) TacticalEmerald else if (isActive) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                actionContent()
            }
        }
    }
}

private fun openDeveloperOptions(context: Context) {
    val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val launched = runCatching { context.startActivity(devIntent) }.isSuccess
    if (!launched) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
