package com.xusu.sshlinkterminal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xusu.sshlinkterminal.data.ColorBag
import com.xusu.sshlinkterminal.data.GuardStatus
import com.xusu.sshlinkterminal.ui.glass.bouncySpringSpec

@Composable
fun GuardStatusOverlay(
    visible: Boolean,
    guardStatus: GuardStatus,
    colorBag: ColorBag,
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestBatteryWhitelist: () -> Unit,
    onRequestShizuku: () -> Unit,
    onApplyShizukuHarden: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val jumpRotation = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            jumpRotation.snapTo(-4f)
            jumpRotation.animateTo(0f, animationSpec = bouncySpringSpec())
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.86f, animationSpec = bouncySpringSpec()) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp)
                        .rotate(jumpRotation.value)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "后台驻留与 Shizuku 防杀体检",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "解决锁后台仍被系统回收导致 SSH/OAuth 中断",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        GuardCheckRow(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "通知栏保活权限",
                            subtitle = "维持前台服务通知与快捷重连面板",
                            passed = guardStatus.notificationsGranted,
                            actionLabel = "授权",
                            onAction = onRequestNotifications
                        )

                        GuardCheckRow(
                            icon = Icons.Rounded.BatteryChargingFull,
                            title = "忽略电池优化",
                            subtitle = "避免系统休眠时切断网络连接",
                            passed = guardStatus.batteryOptimizationIgnored,
                            actionLabel = "豁免",
                            onAction = onRequestBatteryWhitelist
                        )

                        GuardCheckRow(
                            icon = Icons.Rounded.Security,
                            title = when {
                                !guardStatus.shizukuInstalled -> "Shizuku 未安装"
                                !guardStatus.shizukuRunning -> "Shizuku 服务未启动"
                                !guardStatus.shizukuPermissionGranted -> "Shizuku 待授权"
                                else -> "Shizuku 守护已就绪"
                            },
                            subtitle = "用于写入 Doze 白名单、AppOps 放行与 Daemon 看门狗",
                            passed = guardStatus.shizukuPermissionGranted,
                            actionLabel = if (guardStatus.shizukuRunning) "授权" else "检测",
                            onAction = onRequestShizuku
                        )

                        GuardCheckRow(
                            icon = Icons.Rounded.Shield,
                            title = "Shizuku Doze / Standby / AppOps 加固",
                            subtitle = guardStatus.lastReport.ifBlank { "将进程设为白名单并启动 Daemon 看门狗" },
                            passed = guardStatus.dozeWhitelisted == true &&
                                guardStatus.standbyBucketActive != false &&
                                guardStatus.appOpsBackgroundAllowed == true &&
                                guardStatus.watchdogActive,
                            actionLabel = "执行加固",
                            onAction = onApplyShizukuHarden
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onApplyShizukuHarden,
                                modifier = Modifier.weight(1f),
                                enabled = guardStatus.shizukuPermissionGranted
                            ) {
                                Text("一键 Shizuku 全量加固")
                            }
                            OutlinedButton(onClick = onOpenAppSettings) {
                                Text("自启动/省电")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("完成")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuardCheckRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    passed: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (passed) Color(0xFF4ADE80).copy(alpha = 0.12f)
                else Color(0xFFFBBF24).copy(alpha = 0.12f)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (passed) Icons.Rounded.CheckCircle else icon,
            contentDescription = null,
            tint = if (passed) Color(0xFF4ADE80) else Color(0xFFFBBF24),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedContent(
                targetState = passed,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "guardBadgeAnim"
            ) { ok ->
                Text(
                    text = if (ok) "已保护" else "待配置",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (ok) Color(0xFF22C55E) else Color(0xFFF59E0B)
                )
            }
        }
        if (!passed) {
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
