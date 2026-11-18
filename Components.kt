package com.arenafit.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.arenafit.app.domain.Muscle

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    if (values.size < 2) {
        Text("Log at least 2 sessions to see a trend.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val max = values.max()
        val min = values.min()
        val range = (max - min).coerceAtLeast(1f)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height * 0.95f - (v - min) / range * size.height * 0.9f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 5f))
    }
}

@Composable
fun BarChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val slot = size.width / values.size.coerceAtLeast(1)
            values.forEachIndexed { i, v ->
                val h = v / max * size.height
                drawRect(
                    color,
                    topLeft = Offset(i * slot + slot * 0.2f, size.height - h),
                    size = Size(slot * 0.6f, h)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** Grid of muscle tiles colored red (fatigued) to green (recovered). */
@Composable
fun RecoveryMap(recovery: Map<Muscle, Float>, modifier: Modifier = Modifier) {
    val red = Color(0xFFE53935)
    val green = Color(0xFF43A047)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Muscle.values().toList().chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { m ->
                    val r = recovery[m] ?: 1f
                    Box(
                        Modifier.weight(1f).height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(lerp(red, green, r).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(m.label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                            Text("${(r * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun RouteCanvas(points: List<Pair<Double, Double>>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        if (points.size < 2) return@Canvas
        val minLat = points.minOf { it.first }
        val maxLat = points.maxOf { it.first }
        val minLng = points.minOf { it.second }
        val maxLng = points.maxOf { it.second }
        val spanLat = (maxLat - minLat).coerceAtLeast(1e-5)
        val spanLng = (maxLng - minLng).coerceAtLeast(1e-5)
        val path = Path()
        points.forEachIndexed { i, (lat, lng) ->
            val x = ((lng - minLng) / spanLng * size.width).toFloat()
            val y = (size.height - (lat - minLat) / spanLat * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 6f))
    }
}

@Composable
fun FullScreenCenter(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
