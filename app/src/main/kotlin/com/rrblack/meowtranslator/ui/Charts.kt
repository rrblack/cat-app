package com.rrblack.meowtranslator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.rrblack.meowtranslator.core.analysis.Vocalization
import com.rrblack.meowtranslator.core.translate.Emotion
import kotlin.math.ln
import kotlin.math.roundToInt

@Composable
fun ProbabilityBar(label: String, probability: Double, color: Color = MaterialTheme.colorScheme.primary) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(pct(probability), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { probability.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
    }
}

/** Two bipolar meters: negative ↔ positive feeling, and calm ↔ excited. */
@Composable
fun EmotionMeter(emotion: Emotion) {
    Text(emotion.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Meter("Unhappy", "Happy", ((emotion.valence + 1) / 2).toFloat())
    Meter("Calm", "Excited", emotion.arousal.toFloat())
}

@Composable
private fun Meter(left: String, right: String, position: Float) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val dot = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val y = size.height / 2
                drawLine(track, Offset(0f, y), Offset(size.width, y), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
                val x = position.coerceIn(0f, 1f) * size.width
                drawCircle(dot, radius = 8.dp.toPx(), center = Offset(x.coerceIn(8.dp.toPx(), size.width - 8.dp.toPx()), y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(left, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(right, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The call's melody (pitch contour, log-frequency axis) drawn over its loudness envelope.
 */
@Composable
fun PitchChart(v: Vocalization, envelopeDb: DoubleArray) {
    val pitches = v.pitch
    val lo = pitches.minOf { it.hz }
    val hi = pitches.maxOf { it.hz }
    // Log-frequency axis with at least half an octave of range so small wobbles don't look dramatic.
    val center = (ln(lo) + ln(hi)) / 2
    val half = maxOf((ln(hi) - ln(lo)) / 2 * 1.15, ln(2.0) / 4)
    val yMin = center - half
    val yMax = center + half
    val t0 = v.startSec
    val t1 = v.endSec
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val grid = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(56.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("${kotlin.math.exp(yMax).roundToInt()} Hz", style = MaterialTheme.typography.labelSmall)
                Box(Modifier.height(84.dp))
                Text("${kotlin.math.exp(yMin).roundToInt()} Hz", style = MaterialTheme.typography.labelSmall)
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                fun x(t: Double) = ((t - t0) / (t1 - t0)).toFloat().coerceIn(0f, 1f) * size.width
                fun y(hz: Double) = (1 - ((ln(hz) - yMin) / (yMax - yMin))).toFloat() * size.height

                drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                drawLine(grid, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())

                // Loudness envelope as a soft background area (10 ms frames centred 25 ms in).
                val first = ((t0 - 0.025) / 0.01).toInt().coerceAtLeast(0)
                val last = ((t1 - 0.025) / 0.01).toInt().coerceAtMost(envelopeDb.size - 1)
                if (last > first) {
                    val peak = (first..last).maxOf { envelopeDb[it] }
                    val area = Path().apply {
                        moveTo(x(0.025 + first * 0.01), size.height)
                        for (i in first..last) {
                            val level = ((envelopeDb[i] - (peak - 40)) / 40).coerceIn(0.0, 1.0).toFloat()
                            lineTo(x(0.025 + i * 0.01), size.height * (1 - level))
                        }
                        lineTo(x(0.025 + last * 0.01), size.height)
                        close()
                    }
                    drawPath(area, fill)
                }

                // Pitch line; break it where frames are missing (unvoiced gaps).
                val path = Path()
                var prevT = Double.NaN
                for (p in pitches) {
                    val px = x(p.timeSec)
                    val py = y(p.hz)
                    if (prevT.isNaN() || p.timeSec - prevT > 0.035) path.moveTo(px, py) else path.lineTo(px, py)
                    prevT = p.timeSec
                }
                drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Text(
            "Melody (line) over loudness (shaded), %.1f s".format(t1 - t0),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
