package com.rygent.monitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AreaChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    fillColor: Color = Color.Blue.copy(alpha = 0.2f)
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)
        val maxVal = data.maxOrNull() ?: 1f
        val normalize = { v: Float -> height - (v / maxVal * height) }

        val path = Path().apply {
            moveTo(0f, normalize(data.first()))
            data.forEachIndexed { index, value ->
                lineTo(index * spacing, normalize(value))
            }
        }
        
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
