package com.rygent.monitor.ui.screens.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rygent.monitor.domain.model.Process
import com.rygent.monitor.ui.theme.DarkSurface
import com.rygent.monitor.ui.theme.PrimaryBlue
import com.rygent.monitor.ui.theme.TextGray
import java.util.Locale

@Composable
fun ProcessList(
    processes: List<Process>,
    onProcessClick: (Process) -> Unit,
    modifier: Modifier = Modifier
) {
    val sorted = processes.sortedByDescending { it.cpuPercent }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sorted) { process ->
            ProcessItem(process, onClick = { onProcessClick(process) })
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun ProcessItem(process: Process, onClick: () -> Unit) {
    com.rygent.monitor.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    process.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = com.rygent.monitor.ui.theme.TextWhite
                )
                Text(
                    "PID: ${process.pid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.rygent.monitor.ui.theme.TextGray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.getDefault(), "%.1f%% CPU", process.cpuPercent),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (process.cpuPercent > 50) com.rygent.monitor.ui.theme.WarningOrange else com.rygent.monitor.ui.theme.PrimaryBlue
                )
                Text(
                    formatBytes(process.memoryBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = com.rygent.monitor.ui.theme.TextGray
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024
    if (kb < 1024) return "${kb}KB"
    val mb = kb / 1024
    if (mb < 1024) return "${mb}MB"
    val gb = mb.toFloat() / 1024
    return String.format(Locale.getDefault(), "%.1fGB", gb)
}
