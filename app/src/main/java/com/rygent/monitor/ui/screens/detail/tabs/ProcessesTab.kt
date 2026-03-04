package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.Process
import com.rygent.monitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessesTab(
    device: Device,
    processHistory: List<com.rygent.monitor.domain.model.ProcessHistoryEntry> = emptyList(),
    modifier: Modifier = Modifier,
    onProcessClick: (Process) -> Unit = {}
) {
    if (device.status == com.rygent.monitor.domain.model.DeviceStatus.OFFLINE) {
        ProcessesTabSkeleton()
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("CPU") } // CPU, RAM, NAME
    
    val processes = device.processes ?: emptyList()
    
    val filteredProcesses = remember(processes, searchQuery, sortBy) {
        processes.filter { 
            it.name.contains(searchQuery, ignoreCase = true) 
        }.sortedWith(
            when (sortBy) {
                "CPU" -> compareByDescending { it.cpuPercent }
                "RAM" -> compareByDescending { it.memoryBytes }
                else -> compareBy { it.name }
            }
        )
    }

    // Precalculate max CPU over 24h per process PID
    val maxCpuHistoryMap = remember(processHistory) {
        val map = mutableMapOf<Int, Double>()
        processHistory.forEach { entry ->
            entry.processes.forEach { p ->
                val currentMax = map[p.pid] ?: 0.0
                if (p.cpuPercent > currentMax) {
                    map[p.pid] = p.cpuPercent
                }
            }
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search processes...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = PrimaryBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            shape = MaterialTheme.shapes.medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sort Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("CPU", "RAM", "NAME").forEach { label ->
                FilterChip(
                    selected = sortBy == label,
                    onClick = { sortBy = label },
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Process List
        if (filteredProcesses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No processes found", color = TextGray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB or bottom nav
            ) {
                items(filteredProcesses) { process ->
                    ProcessItem(
                        process = process, 
                        maxCpu = maxCpuHistoryMap[process.pid],
                        onClick = { onProcessClick(process) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessItem(
    process: Process,
    maxCpu: Double?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient Icon Background
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(GradientBlue),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = process.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.name,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(GlassWhite, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PID: ${process.pid}",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (maxCpu != null && maxCpu > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "24h Peak: ${String.format("%.1f", maxCpu)}%",
                            color = TextGray.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Badge(
                    text = "${String.format("%.1f", process.cpuPercent)}% CPU",
                    color = if (process.cpuPercent > 50) ErrorRed else if (process.cpuPercent > 20) WarningOrange else SuccessGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Badge(
                    text = "${process.memoryBytes / 1024 / 1024} MB",
                    color = SecondaryBlue
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProcessesTabSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.rygent.monitor.ui.components.SkeletonLoader(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            baseColor = DarkSurface,
            highlightColor = androidx.compose.ui.graphics.Color.DarkGray
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                com.rygent.monitor.ui.components.SkeletonLoader(
                    modifier = Modifier.width(60.dp).height(32.dp),
                    baseColor = DarkSurface,
                    highlightColor = androidx.compose.ui.graphics.Color.DarkGray
                )
            }
        }
        
        repeat(6) {
            com.rygent.monitor.ui.components.SkeletonLoader(
                modifier = Modifier.fillMaxWidth().height(70.dp),
                baseColor = DarkSurface,
                highlightColor = androidx.compose.ui.graphics.Color.DarkGray
            )
        }
    }
}
