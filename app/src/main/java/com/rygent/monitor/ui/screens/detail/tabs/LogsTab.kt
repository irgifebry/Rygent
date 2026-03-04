package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val source: String,
    val message: String
)

enum class LogLevel { INFO, WARN, ERROR, DEBUG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTab(
    device: Device,
    modifier: Modifier = Modifier
) {
    if (device.status == com.rygent.monitor.domain.model.DeviceStatus.OFFLINE) {
        LogsTabSkeleton()
        return
    }

    val logs = remember(device) { generateLogs(device) }
    var filterLevel by remember { mutableStateOf<LogLevel?>(null) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, filterLevel) {
        if (filterLevel == null) logs
        else logs.filter { it.level == filterLevel }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header row with filter and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "System Logs",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log Level Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = filterLevel == null,
                onClick = { filterLevel = null },
                label = { Text("All", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = TextWhite,
                    containerColor = DarkSurface,
                    labelColor = TextGray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                ),
                modifier = Modifier.height(30.dp)
            )
            LogLevel.entries.forEach { level ->
                val chipColor = logLevelColor(level)
                FilterChip(
                    selected = filterLevel == level,
                    onClick = { filterLevel = level },
                    label = { Text(level.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = TextWhite,
                        containerColor = DarkSurface,
                        labelColor = TextGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal-style log viewer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0E14))
                .padding(2.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredLogs) { log ->
                    LogLine(log)
                }
            }
        }
    }
}

@Composable
fun LogLine(log: LogEntry) {
    val levelColor = logLevelColor(log.level)
    val levelTag = when (log.level) {
        LogLevel.INFO -> "INF"
        LogLevel.WARN -> "WRN"
        LogLevel.ERROR -> "ERR"
        LogLevel.DEBUG -> "DBG"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = log.timestamp,
            color = TextGray.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(70.dp)
        )
        // Level tag
        Text(
            text = "[$levelTag]",
            color = levelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp)
        )
        // Source
        Text(
            text = log.source,
            color = SecondaryBlue,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        // Message
        Text(
            text = log.message,
            color = when (log.level) {
                LogLevel.ERROR -> ErrorRed.copy(alpha = 0.9f)
                LogLevel.WARN -> WarningOrange.copy(alpha = 0.9f)
                else -> TextWhite.copy(alpha = 0.8f)
            },
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun logLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.INFO -> SuccessGreen
    LogLevel.WARN -> WarningOrange
    LogLevel.ERROR -> ErrorRed
    LogLevel.DEBUG -> TextGray
}

private fun generateLogs(device: Device): List<LogEntry> {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val now = sdf.format(Date())
    val logs = mutableListOf<LogEntry>()

    logs.add(LogEntry(now, LogLevel.INFO, "system", "Device monitoring session started"))
    val connDisplay = if (device.ipAddress.contains("trycloudflare.com")) "Remote Tunnel" else "${device.ipAddress}:${device.port}"
    logs.add(LogEntry(now, LogLevel.INFO, "network", "Connected to $connDisplay"))
    logs.add(LogEntry(now, LogLevel.DEBUG, "api", "GET /system-info -> 200 OK"))
    logs.add(LogEntry(now, LogLevel.INFO, "cpu", "CPU usage: ${device.cpuUsage}% (${device.cpuUsagePerCore?.size ?: 0} cores)"))
    logs.add(LogEntry(now, LogLevel.INFO, "memory", "RAM usage: ${device.ramUsage}%"))

    device.hardware?.cpuTemp?.let { temp ->
        if (temp > 70) {
            logs.add(LogEntry(now, LogLevel.WARN, "thermal", "CPU temp elevated: ${temp.toInt()}°C"))
        } else {
            logs.add(LogEntry(now, LogLevel.INFO, "thermal", "CPU temp: ${temp.toInt()}°C"))
        }
    }

    device.diskUsage?.let { disk ->
        if (disk > 80) {
            logs.add(LogEntry(now, LogLevel.WARN, "storage", "Disk usage high: $disk%"))
        } else {
            logs.add(LogEntry(now, LogLevel.INFO, "storage", "Disk usage: $disk%"))
        }
    }

    device.hardware?.diskPartitions?.forEach { part ->
        logs.add(LogEntry(now, LogLevel.DEBUG, "storage", "${part.device} mounted at ${part.mountpoint} (${part.fstype}) - ${part.percent}%"))
    }

    device.battery?.let { bat ->
        val plugged = if (bat.powerPlugged) "plugged" else "unplugged"
        logs.add(LogEntry(now, LogLevel.INFO, "power", "Battery: ${bat.percent.toInt()}% ($plugged)"))
        if (bat.percent < 20 && !bat.powerPlugged) {
            logs.add(LogEntry(now, LogLevel.WARN, "power", "Battery low, consider plugging in"))
        }
    }

    device.gpus?.forEach { gpu ->
        logs.add(LogEntry(now, LogLevel.INFO, "gpu", "${gpu.name}: ${gpu.usage}% load, ${gpu.temp}°C"))
    }

    val processCount = device.processes?.size ?: 0
    logs.add(LogEntry(now, LogLevel.INFO, "proc", "Tracking $processCount processes"))

    device.processes?.filter { it.cpuPercent > 50 }?.forEach { proc ->
        logs.add(LogEntry(now, LogLevel.WARN, "proc", "High CPU: ${proc.name} (PID:${proc.pid}) at ${String.format("%.1f", proc.cpuPercent)}%"))
    }

    device.system?.let { sys ->
        logs.add(LogEntry(now, LogLevel.DEBUG, "sys", "Kernel: ${sys.kernel ?: "unknown"}"))
        if (sys.rootAccess) {
            logs.add(LogEntry(now, LogLevel.INFO, "sys", "Root access: enabled"))
        }
    }

    device.network?.interfaces?.forEach { iface ->
        val status = if (iface.up) "UP" else "DOWN"
        logs.add(LogEntry(now, LogLevel.DEBUG, "net", "${iface.name}: ${iface.ip} ($status)"))
    }

    if (device.cpuUsage > 90) {
        logs.add(LogEntry(now, LogLevel.ERROR, "cpu", "CPU OVERLOAD: ${device.cpuUsage}% - system may be unresponsive"))
    }
    if (device.ramUsage > 95) {
        logs.add(LogEntry(now, LogLevel.ERROR, "memory", "MEMORY CRITICAL: ${device.ramUsage}% - OOM killer may activate"))
    }

    logs.add(LogEntry(now, LogLevel.INFO, "system", "Uptime: ${device.uptime}"))
    logs.add(LogEntry(now, LogLevel.DEBUG, "sync", "Latency: ${device.latency}ms"))
    logs.add(LogEntry(now, LogLevel.INFO, "system", "Monitoring active..."))

    return logs
}

@Composable
fun LogsTabSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.rygent.monitor.ui.components.SkeletonLoader(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            baseColor = DarkSurface,
            highlightColor = androidx.compose.ui.graphics.Color.DarkGray
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                 com.rygent.monitor.ui.components.SkeletonLoader(
                    modifier = Modifier.width(60.dp).height(30.dp),
                    baseColor = DarkSurface,
                    highlightColor = androidx.compose.ui.graphics.Color.DarkGray
                )
            }
        }
        com.rygent.monitor.ui.components.SkeletonLoader(
            modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
            baseColor = Color(0xFF0A0E14),
            highlightColor = androidx.compose.ui.graphics.Color.DarkGray.copy(alpha = 0.5f)
        )
    }
}
