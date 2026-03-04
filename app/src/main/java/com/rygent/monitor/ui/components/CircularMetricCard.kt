package com.rygent.monitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.ui.theme.*

@Composable
fun CircularMetricCard(
    title: String,
    value: String,
    percentage: Float, // 0.0 to 1.0
    subtitle: String,
    color: Color = PrimaryBlue,
    secondaryValue: String? = null,
    modifier: Modifier = Modifier,
    radius: Dp = 60.dp,
    strokeWidth: Dp = 12.dp,
    onClick: (() -> Unit)? = null
) {
    val animatedPercentage by animateFloatAsState(targetValue = percentage, animationSpec = androidx.compose.animation.core.tween(1000))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            secondaryValue?.let {
                Text(
                    text = it,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(radius * 2)) {
                // Background Circle
                drawArc(
                    color = DarkSurface.copy(alpha = 0.5f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                
                // Progress Arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedPercentage,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle, // e.g. "GB" or "%"
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Mini chart placeholder or additional text
        Text("Active", color = TextGray, fontSize = 12.sp)
    }
}
