package com.rygent.monitor.ui.screens.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.ui.theme.PrimaryBlue
import com.rygent.monitor.ui.theme.TextGray

@Composable
fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    label: String,
    currentValue: String,
    color: Color = PrimaryBlue,
    drawLabels: Boolean = true
) {
    Column(modifier = modifier) {
        if (drawLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextGray)
                Text(currentValue, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Canvas(modifier = if (drawLabels) Modifier.fillMaxWidth().height(150.dp) else Modifier.fillMaxSize()) {
            if (data.isEmpty()) return@Canvas
            
            val path = Path()
            val fillPath = Path()
            
            val width = size.width
            val height = size.height
            val xStep = if (data.size > 1) width / (data.size - 1) else 0f
            val maxVal = 100f
            
            data.forEachIndexed { index, value ->
                val x = index * xStep
                // Ensure y is within bounds [0, height]
                val normalizedValue = value.coerceIn(0f, maxVal)
                val y = height - (normalizedValue / maxVal * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                
                if (index == data.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }
            
            // Draw Area Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
            
            // Draw Line
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Add point at end
            val lastX = (data.size - 1) * xStep
            val lastY = height - (data.last() / maxVal * height)
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

