package com.rygent.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        color = GlassWhite,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun PremiumProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PrimaryBlue,
    height: Dp = 10.dp
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
    }
}

@Composable
fun GlowIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SparklineGraph(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    lineWidth: Float = 5f
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        
        val path = androidx.compose.ui.graphics.Path()
        val maxValue = (data.maxOrNull() ?: 100f).coerceAtLeast(1f)
        val w = size.width
        val h = size.height
        
        data.forEachIndexed { index, value ->
            val x = index * (w / (data.size - 1))
            val y = h - (value / maxValue * h)
            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }
        
        // Draw path stroke
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = lineWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Add a subtle gradient fill below the line
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
            )
        )
    }
}

@Composable
fun OfflineBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WarningOrange.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Device is offline",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                    color = WarningOrange
                )
                Text(
                    "Data could not be loaded. Connect device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
            Button(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange.copy(alpha = 0.2f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text("Refresh", fontSize = 11.sp, color = WarningOrange, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
