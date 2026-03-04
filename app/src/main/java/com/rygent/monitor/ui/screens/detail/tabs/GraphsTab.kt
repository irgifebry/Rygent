package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.ui.components.InteractiveLineChart
import com.rygent.monitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphsTab(
    device: Device,
    cpuHistory: List<Float> = emptyList(),
    ramHistory: List<Float> = emptyList(),
    gpuHistory: List<Float> = emptyList()
) {
    if (device.status == com.rygent.monitor.domain.model.DeviceStatus.OFFLINE) {
        GraphsTabSkeleton()
        return
    }

    var selectedTimeRange by remember { mutableStateOf("1H") }
    
    // Filter data based on selection (Mock logic since we only have session history)
    val displayCpu = remember(cpuHistory, selectedTimeRange) { 
        if (selectedTimeRange == "1H") cpuHistory.takeLast(60) else cpuHistory 
    }
    val displayRam = remember(ramHistory, selectedTimeRange) { 
        if (selectedTimeRange == "1H") ramHistory.takeLast(60) else ramHistory 
    }
    val displayGpu = remember(gpuHistory, selectedTimeRange) { 
        if (selectedTimeRange == "1H") gpuHistory.takeLast(60) else gpuHistory 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("1H", "6H", "24H").forEach { label ->
                FilterChip(
                    selected = label == selectedTimeRange,
                    onClick = { selectedTimeRange = label },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = TextWhite,
                        containerColor = DarkSurface,
                        labelColor = TextGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
        
        // CPU Graph
        GraphCard(
            title = "CPU Usage",
            data = if (displayCpu.isEmpty()) listOf(0f) else displayCpu,
            color = if ((displayCpu.lastOrNull() ?: 0f) > 80) WarningOrange else PrimaryBlue
        )

        // GPU Graph
        if (!device.gpus.isNullOrEmpty()) {
            GraphCard(
                title = "GPU Usage",
                data = if (displayGpu.isEmpty()) listOf(0f) else displayGpu,
                color = SuccessGreen
            )
        }
        
        // RAM Graph
        GraphCard(
            title = "RAM Usage",
            data = if (displayRam.isEmpty()) listOf(0f) else displayRam,
            color = SecondaryBlue
        )
    }
}

@Composable
fun GraphCard(
    title: String,
    data: List<Float>,
    color: Color
) {
    var scrubbingValue by remember { mutableStateOf<Float?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, shape = MaterialTheme.shapes.medium)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(title, color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = scrubbingValue?.let { "${it.toInt()}%" } ?: "${data.last().toInt()}%",
                color = if (scrubbingValue != null) PrimaryBlue else TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        InteractiveLineChart(
            data = data,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp), // Slightly shorter
            lineColor = color,
            fillColor = color.copy(alpha = 0.2f),
            onScrubbing = { scrubbingValue = it }
        )
        
        Spacer(modifier = Modifier.height(8.dp)) // Small bottom padding
    }
}

@Composable
fun GraphsTabSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                com.rygent.monitor.ui.components.SkeletonLoader(
                    modifier = Modifier.width(60.dp).height(32.dp),
                    baseColor = DarkSurface,
                    highlightColor = Color.DarkGray
                )
            }
        }
        
        repeat(2) {
            com.rygent.monitor.ui.components.SkeletonLoader(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                baseColor = DarkSurface,
                highlightColor = Color.DarkGray
            )
        }
    }
}
