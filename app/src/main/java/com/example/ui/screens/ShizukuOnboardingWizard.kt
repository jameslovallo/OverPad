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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.shizuku.ShizukuConnectionStage
import com.example.shizuku.ShizukuStatus

@OptIn(ExperimentalLayoutApi::class)
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
    var step1Skipped by rememberSaveable { mutableStateOf(false) }

    val step1Done = shizukuStatus.isBinderAlive
    val step1Satisfied = step1Done || step1Skipped
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
        color = MaterialTheme.colorScheme.background
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "OverPad Setup",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        IconButton(
                            onClick = onRefreshStatus,
                            modifier = Modifier.testTag("onboarding_refresh_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh status",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onFinish,
                            modifier = Modifier.testTag("onboarding_close_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close setup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                text = "Setup",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$completedCount / 4",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (completedCount == 4) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (completedCount == 4) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OnboardingStepCard(
                        stepNumber = 1,
                        title = "Shizuku Service",
                        subtitle = when {
                            shizukuStatus.isBinderAlive -> "Running"
                            step1Skipped -> "Skipped"
                            shizukuStatus.isShizukuInstalled -> "Start Shizuku via Wireless Debugging"
                            else -> "Install & start Shizuku"
                        },
                        icon = Icons.Default.Usb,
                        isDone = step1Done,
                        isActive = !step1Satisfied
                    ) {
                        if (!shizukuStatus.isBinderAlive) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                } else {
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

                                OutlinedButton(
                                    onClick = onRefreshStatus,
                                    modifier = Modifier.testTag("onboarding_recheck_shizuku_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Re-check")
                                }

                                if (!step1Skipped) {
                                    TextButton(
                                        onClick = { step1Skipped = true },
                                        modifier = Modifier.testTag("onboarding_skip_step1_button")
                                    ) {
                                        Text("Skip")
                                    }
                                }
                            }
                        }
                    }

                    OnboardingStepCard(
                        stepNumber = 2,
                        title = "Authorize Bridge",
                        subtitle = when (shizukuStatus.stage) {
                            ShizukuConnectionStage.CONNECTED -> shizukuStatus.modeLabel
                            ShizukuConnectionStage.CONNECTING -> "Connecting..."
                            ShizukuConnectionStage.PERMISSION_REQUIRED -> "Permission required"
                            else -> "Grant Shizuku access"
                        },
                        icon = Icons.Default.Security,
                        isDone = step2Done,
                        isActive = step1Satisfied && !step2Done
                    ) {
                        if (!shizukuStatus.isPermissionGranted) {
                            Button(
                                onClick = onRequestShizukuPermission,
                                modifier = Modifier.testTag("onboarding_grant_shizuku_button")
                            ) {
                                Text("Authorize")
                            }
                        } else if (!shizukuStatus.isServiceConnected) {
                            OutlinedButton(
                                onClick = onReconnectBridge,
                                modifier = Modifier.testTag("onboarding_bind_shizuku_button")
                            ) {
                                Text("Connect")
                            }
                        }
                        shizukuStatus.lastError?.let { err ->
                            Text(err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    OnboardingStepCard(
                        stepNumber = 3,
                        title = "Display Over Apps",
                        subtitle = if (step3Done) "Granted" else "Required for floating controls",
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
                                Text("Grant")
                            }
                        }
                    }

                    OnboardingStepCard(
                        stepNumber = 4,
                        title = "Usage Access (Optional)",
                        subtitle = if (step4Done) "Granted" else "Auto-switch app presets",
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
                                Text("Grant")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(if (requiredReady) "Done" else "Skip")
                    }
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("onboarding_continue_button")
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (requiredReady) "Finish" else "Continue")
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
            isDone -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "step_border"
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                            isDone -> MaterialTheme.colorScheme.secondaryContainer
                            isActive -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
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
                        tint = when {
                            isDone -> MaterialTheme.colorScheme.secondary
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
