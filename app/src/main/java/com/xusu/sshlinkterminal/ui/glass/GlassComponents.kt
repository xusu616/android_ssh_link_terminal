package com.xusu.sshlinkterminal.ui.glass

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xusu.sshlinkterminal.data.ChartWindowRule
import com.xusu.sshlinkterminal.data.ColorBag
import com.xusu.sshlinkterminal.ui.theme.contrastOnColor
import com.xusu.sshlinkterminal.ui.theme.topSafeInsetsPadding
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

internal val GlassSideMenuWidth = 276.dp
internal val NarrowRailWidth = 68.dp

fun <T> bouncySpringSpec(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

@Composable
fun Modifier.glassHaze(
    hazeState: HazeState,
    blurIntensity: Float?,
    darkTint: Color = Color(0xFF090D16).copy(alpha = 0.34f),
    lightTint: Color = Color.White.copy(alpha = 0.28f),
    darkFallback: Color = Color(0xFF0F172A).copy(alpha = 0.72f),
    lightFallback: Color = Color.White.copy(alpha = 0.84f)
): Modifier {
    val isDark = isSystemInDarkTheme()
    return this.hazeEffect(state = hazeState) {
        inputScale = HazeInputScale.Fixed(0.5f)
        blurEffect {
            blurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            blurIntensity?.let { blurRadius = it.dp }
            noiseFactor = 0.05f
            colorEffects = listOf(HazeColorEffect.tint(if (isDark) darkTint else lightTint))
            fallbackTint = HazeColorEffect.tint(if (isDark) darkFallback else lightFallback)
        }
    }
}

@Stable
class SideMenuState {
    var isOpen by mutableStateOf(false)
        private set
    var detailRoute by mutableStateOf<String?>(null)
        private set

    fun open() {
        isOpen = true
    }

    fun close() {
        isOpen = false
        detailRoute = null
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun openDetail(route: String) {
        detailRoute = route
    }

    fun closeDetail() {
        detailRoute = null
    }
}

@Composable
fun rememberSideMenuState(): SideMenuState = remember { SideMenuState() }

@Composable
fun ResponsiveSideMenuLayout(
    state: SideMenuState,
    railPadding: Dp,
    blurRadius: Float,
    colorBag: ColorBag,
    springAnimation: Boolean,
    showWallpaper: Boolean,
    onNavigate: (String) -> Unit,
    drawerHeader: @Composable ColumnScope.() -> Unit,
    drawerItems: List<SideMenuItem>,
    mainContent: @Composable (startPadding: Dp) -> Unit,
    detailContent: @Composable (route: String, expanded: Boolean, onBack: () -> Unit) -> Unit
) {
    val drawerHazeState = rememberHazeState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 600.dp
        val mainStartPadding by animateDpAsState(
            targetValue = if (expanded && state.isOpen) GlassSideMenuWidth else railPadding,
            animationSpec = if (state.isOpen && springAnimation) {
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            } else tween(280),
            label = "sideMenuStartPadding"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(drawerHazeState)
        ) {
            mainContent(mainStartPadding)
        }

        AnimatedVisibility(
            visible = state.isOpen,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(260))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = state::close
                    )
            )
        }

        AnimatedVisibility(
            visible = state.isOpen,
            enter = slideInHorizontally(
                animationSpec = if (springAnimation) bouncySpringSpec() else tween(260),
                initialOffsetX = { -it }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(240),
                targetOffsetX = { -it }
            )
        ) {
            GlassSidebarBox(
                hazeState = drawerHazeState,
                blurRadius = blurRadius,
                showWallpaper = showWallpaper,
                onDismiss = state::close
            ) {
                drawerHeader()
                drawerItems.forEach { item ->
                    SideMenuRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        colorBag = colorBag,
                        selected = state.detailRoute == item.route,
                        onClick = {
                            if (expanded) {
                                state.openDetail(item.route)
                            } else {
                                state.close()
                                onNavigate(item.route)
                            }
                        }
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(start = if (expanded) GlassSideMenuWidth else 0.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
        ) {
            AnimatedContent(
                targetState = state.detailRoute,
                transitionSpec = {
                    val enter = slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))
                    val exit = slideOutHorizontally(tween(280)) { if (targetState == null) it else -it / 4 } + fadeOut(tween(280))
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                modifier = Modifier.fillMaxSize(),
                label = "sideDetailTransition"
            ) { route ->
                if (route != null) {
                    BackHandler(enabled = true) { state.closeDetail() }
                    detailContent(route, expanded, state::closeDetail)
                }
            }
        }
    }
}

data class SideMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

internal val LocalGlassSidebarLightMask = compositionLocalOf { false }

internal fun glassSidebarPrimaryText(lightMask: Boolean): Color =
    if (lightMask) Color(0xFF0F172A) else Color.White

internal fun glassSidebarSecondaryText(lightMask: Boolean): Color =
    if (lightMask) Color(0xFF334155) else Color.White.copy(alpha = 0.74f)

@Composable
fun GlassSidebarBox(
    hazeState: HazeState,
    blurRadius: Float,
    showWallpaper: Boolean,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightMask = showWallpaper && !isSystemInDarkTheme()
    val shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    Box(
        modifier = Modifier
            .width(GlassSideMenuWidth)
            .fillMaxHeight()
            .clip(shape)
            .then(
                if (showWallpaper) {
                    Modifier.glassHaze(
                        hazeState = hazeState,
                        blurIntensity = blurRadius.coerceAtLeast(22f),
                        darkTint = Color(0xFF0A101E).copy(alpha = 0.74f),
                        darkFallback = Color(0xFF0B1120).copy(alpha = 0.94f)
                    )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to if (isLightMask) Color(0xFF0F172A).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.18f),
                    1f to if (isLightMask) Color(0xFF0F172A).copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f)
                ),
                shape = shape
            )
            // Option rows consume their own clicks; tapping blank/header areas dismisses the drawer.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        CompositionLocalProvider(LocalGlassSidebarLightMask provides isLightMask) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .topSafeInsetsPadding()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                content = content
            )
        }
    }
}

@Composable
fun SideMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colorBag: ColorBag,
    selected: Boolean,
    onClick: () -> Unit
) {
    val lightMask = LocalGlassSidebarLightMask.current
    val iconBg = if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant
    val iconFg = if (colorBag.useM3Color) MaterialTheme.colorScheme.onPrimary else contrastOnColor(iconBg)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    if (colorBag.useM3Color) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    else colorBag.vibrant.copy(alpha = 0.24f)
                } else Color.Transparent
            )
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconFg,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = glassSidebarPrimaryText(lightMask)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = glassSidebarSecondaryText(lightMask)
            )
        }
    }
}

@Composable
fun VerticalTimeText(timeText: String, textColor: Color = Color.White) {
    val parts = timeText.split(":")
    val hour = parts.getOrElse(0) { "00" }
    val minute = parts.getOrElse(1) { "00" }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = hour,
            color = textColor,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = 6.dp)
        )
        Text(
            text = ":",
            color = textColor.copy(alpha = 0.8f),
            fontWeight = FontWeight.W900,
            fontSize = 26.sp,
            modifier = Modifier
                .rotate(90f)
                .offset(y = (-2).dp)
        )
        Text(
            text = minute,
            color = textColor,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (-8).dp)
        )
    }
}

@Composable
fun BatteryMeterIcon(
    progress: Float,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    batteryColor: Color = Color.White,
    chargingColor: Color = Color(0xFF4ADE80)
) {
    Canvas(modifier = modifier.size(width = 46.dp, height = 16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
        val headWidth = 4.dp.toPx()
        val headHeight = 6.dp.toPx()
        val headLeft = size.width - headWidth
        val headTop = (size.height - headHeight) / 2f

        drawRoundRect(
            color = batteryColor,
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        val bodyWidth = size.width - headWidth - 3.dp.toPx()
        val bodyHeight = size.height
        drawRoundRect(
            color = batteryColor,
            topLeft = Offset.Zero,
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        val pad = 3.5.dp.toPx()
        val innerMaxWidth = bodyWidth - pad * 2
        val innerMaxHeight = bodyHeight - pad * 2
        val currentFillWidth = innerMaxWidth * progress.coerceIn(0f, 1f)
        val fillColor = when {
            isCharging -> chargingColor
            progress > 0.2f -> batteryColor
            else -> Color(0xFFFF9800)
        }
        if (currentFillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(pad, pad),
                size = Size(currentFillWidth, innerMaxHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun GuardianControlButton(
    isRunning: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    accentColor: Color = Color(0xFF5EEAD4),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFFF87171) else accentColor,
        animationSpec = bouncySpringSpec(),
        label = "guardianBtnColor"
    )
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, buttonColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = buttonColor.copy(alpha = 0.12f),
            contentColor = buttonColor
        )
    ) {
        Text(
            text = if (isRunning) activeLabel else inactiveLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ThreeStateWindowSwitch(
    state: ChartWindowRule,
    onStateChange: (ChartWindowRule) -> Unit,
    colorBag: ColorBag,
    modifier: Modifier = Modifier
) {
    val trackWidth = 86.dp
    val trackHeight = 24.dp
    val padding = 2.dp
    val options = listOf(
        ChartWindowRule.MINUTE to "M",
        ChartWindowRule.HOUR to "H",
        ChartWindowRule.DAY to "D"
    )
    val slotWidth = (trackWidth - padding * 2) / options.size
    val selectedIndex = options.indexOfFirst { it.first == state }.coerceAtLeast(0)
    val animatedOffset by animateDpAsState(
        targetValue = slotWidth * selectedIndex,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "windowPillOffset"
    )
    val trackColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.secondaryContainer else colorBag.vibrant
    val inactiveLabelColor = contrastOnColor(trackColor).copy(alpha = 0.72f)
    val textStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .background(trackColor, CircleShape)
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            options.forEach { (rule, label) ->
                Box(
                    modifier = Modifier
                        .width(slotWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (rule != state) onStateChange(rule)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = inactiveLabelColor, style = textStyle)
                }
            }
        }
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(slotWidth)
                .fillMaxHeight()
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(options[selectedIndex].second, color = Color(0xFF0F172A), style = textStyle)
        }
    }
}

@Composable
fun NarrowGlassRail(
    timeText: String,
    batteryProgress: Float,
    isCharging: Boolean,
    glassEnabled: Boolean,
    guardHealthy: Boolean,
    colorBag: ColorBag,
    onToggleMenu: () -> Unit,
    onToggleGlass: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenGuardCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val btnBg = if (colorBag.useM3Color) MaterialTheme.colorScheme.secondaryContainer else colorBag.vibrant
    val btnFg = if (colorBag.useM3Color) MaterialTheme.colorScheme.onSecondaryContainer else contrastOnColor(btnBg)
    val rotation by animateFloatAsState(
        targetValue = if (glassEnabled) 180f else 0f,
        animationSpec = bouncySpringSpec(),
        label = "glassToggleRot"
    )

    Column(
        modifier = modifier
            .width(NarrowRailWidth)
            .fillMaxHeight()
            .topSafeInsetsPadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VerticalTimeText(timeText = timeText, textColor = Color.White)
            Spacer(Modifier.height(10.dp))
            BatteryMeterIcon(progress = batteryProgress, isCharging = isCharging)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalIconButton(
                onClick = onToggleMenu,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = btnBg, contentColor = btnFg)
            ) {
                Icon(Icons.Rounded.Menu, contentDescription = "菜单")
            }

            FilledTonalIconButton(
                onClick = onOpenTerminal,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = btnBg, contentColor = btnFg)
            ) {
                Icon(Icons.Rounded.Terminal, contentDescription = "打开终端")
            }

            FilledTonalIconButton(
                onClick = onToggleGlass,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = btnBg, contentColor = btnFg)
            ) {
                Icon(
                    imageVector = if (glassEnabled) Icons.Rounded.BlurOn else Icons.Rounded.AutoAwesome,
                    contentDescription = "切换玻璃背景",
                    modifier = Modifier.rotate(rotation)
                )
            }

            OutlinedIconButton(
                onClick = onOpenGuardCard,
                modifier = Modifier.size(50.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (guardHealthy) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                )
            ) {
                Icon(
                    imageVector = if (guardHealthy) Icons.Rounded.Shield else Icons.Rounded.WarningAmber,
                    contentDescription = "守护体检",
                    tint = if (guardHealthy) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
