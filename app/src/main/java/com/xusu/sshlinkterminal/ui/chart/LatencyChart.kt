package com.xusu.sshlinkterminal.ui.chart

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.Component
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.xusu.sshlinkterminal.data.ChartWindowRule
import com.xusu.sshlinkterminal.data.RttSample
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private val AXIS_TIME_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
private val AXIS_TIME_MINUTES = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val AXIS_DATE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.US)

private fun formatLatencyAxisTime(value: Double, spanSec: Double): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(value.toLong()), ZoneId.systemDefault())
    val fmt = when {
        spanSec <= 120.0 -> AXIS_TIME_SECONDS
        spanSec <= 24.0 * 3600.0 -> AXIS_TIME_MINUTES
        else -> AXIS_DATE_TIME
    }
    return dt.format(fmt)
}

private class StableTimeAxisItemPlacer(
    private val labelCount: Int,
    private val edgeInsetFraction: Double
) : HorizontalAxis.ItemPlacer by HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = false) {

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float
    ): List<Double> {
        val start = visibleXRange.start.coerceAtLeast(fullXRange.start)
        val end = visibleXRange.endInclusive.coerceAtMost(fullXRange.endInclusive)
        val length = end - start
        if (!start.isFinite() || !end.isFinite() || length <= 0.0) return emptyList()
        val count = labelCount.coerceAtLeast(2)
        val inset = edgeInsetFraction.coerceIn(0.0, 0.45)
        val usable = 1.0 - inset * 2.0
        return List(count) { idx ->
            val p = idx.toDouble() / (count - 1).toDouble()
            start + length * (inset + usable * p)
        }
    }

    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float
    ): Float = 0f

    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float
    ): Float = 0f
}

private class PeakLatencyMarker(
    private val halo: Component,
    private val dot: Component,
    private val dotSize: Dp,
    private val haloSize: Dp,
    private val expectedY: Double
) : CartesianMarker {
    override fun drawOverLayers(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>
    ) {
        val dotHalf = with(context.density) { dotSize.toPx() } / 2f
        val haloHalf = with(context.density) { haloSize.toPx() } / 2f
        targets.forEach { target ->
            val lineTarget = target as? LineCartesianLayerMarkerTarget ?: return@forEach
            val point = lineTarget.points.firstOrNull { abs(it.entry.y - expectedY) < 0.5 } ?: return@forEach
            halo.draw(context, target.canvasX - haloHalf, point.canvasY - haloHalf, target.canvasX + haloHalf, point.canvasY + haloHalf)
            dot.draw(context, target.canvasX - dotHalf, point.canvasY - dotHalf, target.canvasX + dotHalf, point.canvasY + dotHalf)
        }
    }
}

@Composable
fun SshLatencyChart(
    samples: List<RttSample>,
    windowRule: ChartWindowRule,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    animateAxisSwitch: Boolean = true
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    var lastWindowRule by remember { mutableStateOf(windowRule) }
    var isWindowTransition by remember { mutableStateOf(false) }
    if (windowRule != lastWindowRule) {
        lastWindowRule = windowRule
        isWindowTransition = true
    }
    LaunchedEffect(isWindowTransition) {
        if (isWindowTransition) {
            delay(420L)
            isWindowTransition = false
        }
    }
    val displaySamples = remember(samples, windowRule) {
        when {
            samples.size >= 2 -> {
                val latestTs = samples.last().timestampSec
                val horizonSec = when (windowRule) {
                    ChartWindowRule.MINUTE -> 60L
                    ChartWindowRule.HOUR -> 3_600L
                    ChartWindowRule.DAY -> 86_400L
                }
                val windowed = samples.filter { it.timestampSec >= latestTs - horizonSec }
                when (windowRule) {
                    ChartWindowRule.MINUTE -> {
                        if (windowed.size >= 2) windowed else samples.takeLast(minOf(samples.size, 6))
                    }
                    ChartWindowRule.HOUR -> {
                        val base = if (windowed.size >= 2) windowed else samples
                        base.takeLast(120)
                    }
                    ChartWindowRule.DAY -> samples
                }
            }
            samples.size == 1 -> {
                val first = samples.first()
                listOf(
                    first.copy(timestampSec = first.timestampSec - 5L),
                    first
                )
            }
            else -> {
                val now = System.currentTimeMillis() / 1000L
                listOf(
                    RttSample(timestampSec = now - 60L, rttMs = 0L, activeSessions = 0),
                    RttSample(timestampSec = now, rttMs = 0L, activeSessions = 0)
                )
            }
        }
    }

    val peakSample = remember(displaySamples) {
        displaySamples.maxByOrNull { it.rttMs }
    }

    LaunchedEffect(displaySamples) {
        val xs = displaySamples.map { it.timestampSec.toDouble() }
        val ys = displaySamples.map { it.rttMs.toDouble() }
        var runningSum = 0.0
        val rollingAvgLine = displaySamples.mapIndexed { idx, sample ->
            runningSum += sample.rttMs.toDouble()
            runningSum / (idx + 1).toDouble()
        }
        modelProducer.runTransaction {
            lineSeries {
                series(xs, ys)
                series(xs, rollingAvgLine)
            }
        }
    }

    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val xFormatter = remember {
        CartesianValueFormatter { context, value, _ ->
            formatLatencyAxisTime(value, context.ranges.xLength)
        }
    }
    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            "${value.roundToInt()} ms"
        }
    }
    val rangeProvider = remember {
        object : CartesianLayerRangeProvider {
            override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double = 0.0

            override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
                maxOf(5.0, ceil(maxY / 5.0) * 5.0)
        }
    }
    val itemPlacer = remember(windowRule) {
        when (windowRule) {
            ChartWindowRule.MINUTE -> StableTimeAxisItemPlacer(labelCount = 3, edgeInsetFraction = 0.18)
            ChartWindowRule.HOUR -> StableTimeAxisItemPlacer(labelCount = 3, edgeInsetFraction = 0.18)
            ChartWindowRule.DAY -> StableTimeAxisItemPlacer(labelCount = 3, edgeInsetFraction = 0.18)
        }
    }

    val peakHalo = rememberShapeComponent(fill = Fill(accentColor.copy(alpha = 0.22f)), shape = CircleShape)
    val peakDot = rememberShapeComponent(fill = Fill(accentColor), shape = CircleShape)
    val peakMarker = remember(peakHalo, peakDot, peakSample) {
        PeakLatencyMarker(
            halo = peakHalo,
            dot = peakDot,
            dotSize = 7.dp,
            haloSize = 18.dp,
            expectedY = peakSample?.rttMs?.toDouble() ?: Double.NaN
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                rangeProvider = rangeProvider,
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(accentColor)),
                        stroke = LineCartesianLayer.LineStroke.Continuous(
                            thickness = 2.5.dp,
                            cap = StrokeCap.Round
                        ),
                        areaFill = LineCartesianLayer.AreaFill.double(
                            topFill = Fill(
                                Brush.verticalGradient(
                                    0f to accentColor.copy(alpha = 0.34f),
                                    0.75f to accentColor.copy(alpha = 0.08f),
                                    1f to Color.Transparent
                                )
                            ),
                            bottomFill = Fill.Transparent
                        ),
                        interpolator = LineCartesianLayer.Interpolator.cubic(curvature = 0.35f)
                    ),
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(secondaryColor.copy(alpha = 0.72f))),
                        stroke = LineCartesianLayer.LineStroke.Continuous(
                            thickness = 1.6.dp,
                            cap = StrokeCap.Round
                        ),
                        interpolator = LineCartesianLayer.Interpolator.cubic(curvature = 0.35f)
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                guideline = null,
                label = rememberAxisLabelComponent(
                    overflow = TextOverflow.Visible,
                    style = TextStyle(fontSize = 10.sp, color = axisLabelColor)
                ),
                size = BaseAxis.Size.Fixed(54.dp),
                valueFormatter = yFormatter
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null,
                guideline = null,
                label = rememberAxisLabelComponent(
                    overflow = TextOverflow.Visible,
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = axisLabelColor)
                ),
                itemPlacer = itemPlacer,
                valueFormatter = xFormatter
            ),
            layerPadding = { CartesianLayerPadding() },
            persistentMarkers = {
                if (peakSample != null && peakSample.rttMs > 0L) {
                    peakMarker.at(peakSample.timestampSec.toDouble())
                }
            }
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        // Animate axis compression/expansion on M/H/D switches when enabled, while keeping live sample appends jitter-free.
        animationSpec = if (animateAxisSwitch && isWindowTransition) {
            tween(durationMillis = 360, easing = FastOutSlowInEasing)
        } else {
            null
        },
        animateIn = animateAxisSwitch
    )
}
