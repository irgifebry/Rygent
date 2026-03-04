package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.ui.components.AreaChart
import com.rygent.monitor.ui.components.CircularMetricCard
import com.rygent.monitor.ui.theme.*

@Composable
fun DashboardTab(
    device: com.rygent.monitor.domain.model.Device,
    modifier: Modifier = Modifier,
    dlHistory: List<Float> = emptyList(),
    ulHistory: List<Float> = emptyList(),
    onMetricClick: (String) -> Unit = {},
    onTabChange: (Int) -> Unit = {}
) {
    if (device.status == com.rygent.monitor.domain.model.DeviceStatus.OFFLINE) {
        DashboardTabSkeleton()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1: CPU & GPU (Grid Layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Card
            MetricRowCard(
                title = "CPU Usage",
                value = device.cpuUsage,
                percentage = device.cpuUsage / 100f,
                subtitleLeft = device.cpuTemp?.let { String.format("%.0f°C", it) } ?: "Active",
                subtitleRight = "${device.hardware?.cores ?: 0} Cores",
                color = PrimaryBlue,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onMetricClick("CPU") }
            ) {
                // Secondary row for per-core breakdown
                if (device.cpuUsagePerCore != null && device.cpuUsagePerCore.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        device.cpuUsagePerCore.forEach { usage ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(PrimaryBlue.copy(alpha = (usage / 100f).toFloat().coerceIn(0.15f, 1f)))
                            )
                        }
                    }
                }
            }
            
            // GPU Card (Integrated Utilization + VRAM if available)
            val gpu = device.gpus?.firstOrNull()
            if (gpu != null) {
                MetricRowCard(
                    title = "GPU Usage",
                    value = gpu.usage,
                    percentage = gpu.usage / 100f,
                    subtitleLeft = if (gpu.temp > 0) "${gpu.temp}°C" else "Active",
                    subtitleRight = gpu.name.split(" ").lastOrNull() ?: "GPU",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onMetricClick("GPU") }
                ) {
                    // Integrated VRAM bar in the GPU card
                    if (gpu.memory > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VRAM", color = TextGray, fontSize = 10.sp)
                            Text("${gpu.memory}%", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        com.rygent.monitor.ui.components.PremiumProgressBar(
                            progress = gpu.memory / 100f,
                            color = SuccessGreen,
                            height = 4.dp
                        )
                    }
                }
            } else {
                // Empty placeholder if no GPU
                Box(modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }
        
        // Row 2: RAM (Full Width)
        val ramTotalBytes = device.hardware?.ramTotal ?: 0.0
        val ramUsedBytes = device.hardware?.ramUsed ?: 0.0
        val ramTotalGB = ramTotalBytes / (1024.0 * 1024.0 * 1024.0)
        val ramUsedGB = ramUsedBytes / (1024.0 * 1024.0 * 1024.0)
        
        MetricRowCard(
            title = "RAM Usage",
            value = device.ramUsage,
            percentage = device.ramUsage / 100f,
            subtitleLeft = "Total: ${String.format("%.1f", ramTotalGB)} GB",
            subtitleRight = "Used: ${String.format("%.1f", ramUsedGB)} GB",
            color = SecondaryBlue,
            onClick = { onMetricClick("RAM") }
        )
        
        // Storage Card
        StorageCard(device = device, onTabChange = onTabChange)
        
        // Network Card
        NetworkCard(dlHistory = dlHistory, ulHistory = ulHistory)
    }
}

@Composable
fun MetricRowCard(
    title: String,
    value: Int,
    percentage: Float,
    subtitleLeft: String,
    subtitleRight: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    com.rygent.monitor.ui.components.GlassCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TextGray, fontSize = 14.sp)
            Text("${value}%", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        com.rygent.monitor.ui.components.PremiumProgressBar(
            progress = percentage,
            color = color,
            modifier = Modifier.padding(bottom = 12.dp),
            height = 8.dp
        )
        
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(subtitleLeft, color = TextGray, fontSize = 12.sp)
            Text(subtitleRight, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        content()
    }
}

@Composable
fun DashboardTabSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.rygent.monitor.ui.components.SkeletonLoader(Modifier.weight(1f).height(120.dp), baseColor = DarkSurface, highlightColor = Color.DarkGray)
            com.rygent.monitor.ui.components.SkeletonLoader(Modifier.weight(1f).height(120.dp), baseColor = DarkSurface, highlightColor = Color.DarkGray)
        }
        com.rygent.monitor.ui.components.SkeletonLoader(Modifier.fillMaxWidth().height(100.dp), baseColor = DarkSurface, highlightColor = Color.DarkGray)
        com.rygent.monitor.ui.components.SkeletonLoader(Modifier.fillMaxWidth().height(100.dp), baseColor = DarkSurface, highlightColor = Color.DarkGray)
        com.rygent.monitor.ui.components.SkeletonLoader(Modifier.fillMaxWidth().height(160.dp), baseColor = DarkSurface, highlightColor = Color.DarkGray)
    }
}

@Composable
fun StorageCard(device: com.rygent.monitor.domain.model.Device, onTabChange: (Int) -> Unit = {}) {
    val partitions = device.hardware?.diskPartitions ?: emptyList()
    val totalGlobal = partitions.sumOf { it.total }
    val freeGlobal = partitions.sumOf { it.free }
    val usedPercent = if (totalGlobal > 0) ((totalGlobal - freeGlobal).toDouble() / totalGlobal * 100).toInt() else 0

    MetricRowCard(
        title = "Storage",
        value = usedPercent,
        percentage = usedPercent / 100f,
        subtitleLeft = "Total: ${String.format("%.1f", totalGlobal / 1e9)} GB",
        subtitleRight = "Free: ${String.format("%.1f", freeGlobal / 1e9)} GB",
        color = if (usedPercent > 85) ErrorRed else if (usedPercent > 70) WarningOrange else SuccessGreen,
        onClick = { onTabChange(2) }
    )
}

@Composable
fun NetworkCard(dlHistory: List<Float>, ulHistory: List<Float>) {
     com.rygent.monitor.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Network Activity", color = TextGray, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(PrimaryBlue))
                    Spacer(Modifier.width(4.dp))
                    Text("DL", color = TextGray, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SuccessGreen))
                    Spacer(Modifier.width(4.dp))
                    Text("UL", color = TextGray, fontSize = 10.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.height(100.dp).fillMaxWidth()) {
            AreaChart(
                data = if (dlHistory.isEmpty()) listOf(0f) else dlHistory,
                modifier = Modifier.fillMaxSize(),
                lineColor = PrimaryBlue,
                fillColor = PrimaryBlue.copy(alpha = 0.2f)
            )
            AreaChart(
                data = if (ulHistory.isEmpty()) listOf(0f) else ulHistory,
                modifier = Modifier.fillMaxSize(),
                lineColor = SuccessGreen,
                fillColor = SuccessGreen.copy(alpha = 0.1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Download", color = TextGray, fontSize = 12.sp)
                val currentDl = dlHistory.lastOrNull() ?: 0f
                Text(
                    if (currentDl < 1e-6) "0 KB/s"
                    else if (currentDl < 1) String.format("%.1f KB/s", currentDl * 1024) 
                    else String.format("%.1f MB/s", currentDl), 
                    color = PrimaryBlue, 
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Upload", color = TextGray, fontSize = 12.sp)
                val currentUl = ulHistory.lastOrNull() ?: 0f
                Text(
                   if (currentUl < 1e-6) "0 KB/s"
                    else if (currentUl < 1) String.format("%.1f KB/s", currentUl * 1024) 
                    else String.format("%.1f MB/s", currentUl), 
                    color = SuccessGreen, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
