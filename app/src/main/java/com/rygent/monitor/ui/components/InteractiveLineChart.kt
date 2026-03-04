package com.rygent.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.ui.theme.*

@Composable
fun InteractiveLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryBlue,
    fillColor: Color = PrimaryBlue.copy(alpha = 0.2f),
    maxValue: Float = 100f,
    onScrubbing: (Float?) -> Unit = {}
) {
    if (data.size < 2) return

    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pacing = size.width / (data.size - 1)
                            val index = Math.round(offset.x / pacing).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = index
                            onScrubbing(data[index])
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pacing = size.width / (data.size - 1)
                            val index = Math.round(change.position.x / pacing).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = index
                            onScrubbing(data[index])
                        },
                        onDragEnd = { 
                            selectedIndex = null
                            onScrubbing(null)
                        },
                        onDragCancel = { 
                            selectedIndex = null
                            onScrubbing(null)
                        }
                    )
                }
                .pointerInput(data) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width
                            val pacing = width / (data.size - 1)
                            val index = Math.round(offset.x / pacing).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = index
                            onScrubbing(data[index])
                            tryAwaitRelease()
                            selectedIndex = null
                            onScrubbing(null)
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1)
            
            val normalize = { v: Float -> height - (v / maxValue * height) }
            
            // Draw Grid Lines (Subtle)
            val gridColor = Color.White.copy(alpha = 0.05f)
            drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f))

            val path = Path().apply {
                data.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = normalize(value)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
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
                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Draw selected point indicator
            selectedIndex?.let { index ->
                val x = index * spacing
                val y = normalize(data[index])
                
                drawLine(
                    color = lineColor.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
                
                drawCircle(
                    color = TextWhite,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(2.dp.toPx())
                )
            }
        }
    }
}
