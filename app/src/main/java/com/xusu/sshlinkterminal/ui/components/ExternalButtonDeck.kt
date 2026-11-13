package com.xusu.sshlinkterminal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xusu.sshlinkterminal.data.MacroButton

data class PhysicalKeySpec(
    val label: String,
    val sequence: String,
    val isModifier: Boolean = false
)

val DEFAULT_PHYSICAL_KEYS = listOf(
    PhysicalKeySpec("ESC", "\u001B"),
    PhysicalKeySpec("TAB", "\t"),
    PhysicalKeySpec("CTRL", "", isModifier = true),
    PhysicalKeySpec("ALT", "", isModifier = true),
    PhysicalKeySpec("↑", "\u001B[A"),
    PhysicalKeySpec("↓", "\u001B[B"),
    PhysicalKeySpec("←", "\u001B[D"),
    PhysicalKeySpec("→", "\u001B[C"),
    PhysicalKeySpec("|", "|"),
    PhysicalKeySpec("/", "/"),
    PhysicalKeySpec("~", "~"),
    PhysicalKeySpec("-", "-"),
    PhysicalKeySpec("HOME", "\u001B[H"),
    PhysicalKeySpec("END", "\u001B[F"),
    PhysicalKeySpec("⏎", "\r")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExternalButtonDeck(
    expanded: Boolean,
    ctrlLocked: Boolean,
    altLocked: Boolean,
    hapticEnabled: Boolean,
    macros: List<MacroButton>,
    accentColor: Color,
    onToggleExpanded: () -> Unit,
    onToggleCtrl: () -> Unit,
    onToggleAlt: () -> Unit,
    onSendSequence: (String) -> Unit,
    onRunMacro: (MacroButton) -> Unit,
    onEditMacro: (MacroButton) -> Unit,
    onOpenMacroEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    fun triggerHaptic() {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A101D).copy(alpha = 0.88f))
            .padding(top = 4.dp, bottom = 6.dp)
    ) {
        // Handle bar with collapse & macro editor shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = onToggleExpanded)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "折叠外置按键",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (expanded) "外置控制坞" else "展开外置按钮",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ctrlLocked || altLocked) {
                    Text(
                        text = buildString {
                            if (ctrlLocked) append("CTRL ")
                            if (altLocked) append("ALT")
                        }.trim(),
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onClick = onOpenMacroEditor)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "定制宏按钮",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("定制", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Row 1: Physical Modifier & Cursor Keys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DEFAULT_PHYSICAL_KEYS.forEach { key ->
                        val active = (key.label == "CTRL" && ctrlLocked) || (key.label == "ALT" && altLocked)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) accentColor.copy(alpha = 0.28f) else Color(0xFF1E293B).copy(alpha = 0.92f),
                            border = BorderStroke(
                                1.dp,
                                if (active) accentColor else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = {
                                        triggerHaptic()
                                        when (key.label) {
                                            "CTRL" -> onToggleCtrl()
                                            "ALT" -> onToggleAlt()
                                            else -> onSendSequence(key.sequence)
                                        }
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key.label,
                                    color = if (active) accentColor else Color(0xFFE2E8F0),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Row 2: Custom Color-Coded Macro Command Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    macros.forEach { macro ->
                        val pillColor = Color(macro.colorHex)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = pillColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, pillColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = {
                                        triggerHaptic()
                                        onRunMacro(macro)
                                    },
                                    onLongClick = {
                                        triggerHaptic()
                                        onEditMacro(macro)
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = macro.label,
                                    color = pillColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(onClick = onOpenMacroEditor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "新增快捷按钮",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
