package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.ui.theme.*

data class AlertItem(
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: String,
    val icon: ImageVector
)

enum class AlertSeverity { CRITICAL, WARNING, INFO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsTab(
    device: Device,
    runawayAlerts: List<com.rygent.monitor.domain.model.RunawayAlert> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (device.status == com.rygent.monitor.domain.model.DeviceStatus.OFFLINE) {
        AlertsTabSkeleton()
        return
    }

    // Generate alerts based on device state
    val alerts = remember(device, runawayAlerts) { generateAlerts(device, runawayAlerts) }

    var filterSeverity by remember { mutableStateOf<AlertSeverity?>(null) }

    val filteredAlerts = remember(alerts, filterSeverity) {
        if (filterSeverity == null) alerts
        else alerts.filter { it.severity == filterSeverity }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = filterSeverity == null,
                onClick = { filterSeverity = null },
                label = { Text("All") },
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
            AlertSeverity.entries.forEach { sev ->
                val chipColor = when (sev) {
                    AlertSeverity.CRITICAL -> ErrorRed
                    AlertSeverity.WARNING -> WarningOrange
                    AlertSeverity.INFO -> PrimaryBlue
                }
                FilterChip(
                    selected = filterSeverity == sev,
                    onClick = { filterSeverity = sev },
                    label = { Text(sev.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
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

        if (filteredAlerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("All clear!", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("No alerts at this time", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredAlerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: AlertItem) {
    val severityColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> ErrorRed
        AlertSeverity.WARNING -> WarningOrange
        AlertSeverity.INFO -> PrimaryBlue
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Timeline dot + line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(severityColor, severityColor.copy(alpha = 0.6f))
                            ),
                            shape = CircleShape
                        )
                        .border(2.dp, GlassWhite, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(severityColor.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            alert.icon,
                            contentDescription = null,
                            tint = severityColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = alert.title,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Severity badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(severityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.severity.name,
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.message,
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.timestamp,
                    color = TextGray.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun generateAlerts(device: Device, runawayAlerts: List<com.rygent.monitor.domain.model.RunawayAlert> = emptyList()): List<AlertItem> {
    val alerts = mutableListOf<AlertItem>()
    val now = "Just now"

    // Process Runaway Alerts (Phase 4)
    runawayAlerts.forEach { alert ->
        alerts.add(
            AlertItem(
                title = "Runaway Process",
                message = alert.message,
                severity = AlertSeverity.CRITICAL,
                timestamp = now,
                icon = Icons.Default.Warning
            )
        )
    }

    // CPU alerts
    if (device.cpuUsage > 90) {
        alerts.add(AlertItem("CPU Critical", "CPU usage is at ${device.cpuUsage}%. System may become unresponsive.", AlertSeverity.CRITICAL, now, Icons.Default.Warning))
    } else if (device.cpuUsage > 70) {
        alerts.add(AlertItem("CPU High", "CPU usage is at ${device.cpuUsage}%. Consider closing unused applications.", AlertSeverity.WARNING, now, Icons.Default.Speed))
    }

    // RAM alerts
    if (device.ramUsage > 90) {
        alerts.add(AlertItem("Memory Critical", "RAM usage at ${device.ramUsage}%. Risk of OOM kills.", AlertSeverity.CRITICAL, now, Icons.Default.Memory))
    } else if (device.ramUsage > 75) {
        alerts.add(AlertItem("Memory High", "RAM usage at ${device.ramUsage}%. Memory pressure detected.", AlertSeverity.WARNING, now, Icons.Default.Memory))
    }

    // Disk alerts
    device.diskUsage?.let { disk ->
        if (disk > 90) {
            alerts.add(AlertItem("Disk Full", "Disk usage at $disk%. Free up space immediately.", AlertSeverity.CRITICAL, now, Icons.Default.Storage))
        } else if (disk > 80) {
            alerts.add(AlertItem("Disk Warning", "Disk usage at $disk%. Consider cleanup.", AlertSeverity.WARNING, now, Icons.Default.Storage))
        }
        Unit
    }

    // Temperature alerts
    device.hardware?.cpuTemp?.let { temp ->
        if (temp > 85) {
            alerts.add(AlertItem("Thermal Critical", "CPU temperature at ${temp.toInt()}°C. Throttling likely.", AlertSeverity.CRITICAL, now, Icons.Default.Thermostat))
        } else if (temp > 70) {
            alerts.add(AlertItem("Thermal Warning", "CPU temperature at ${temp.toInt()}°C.", AlertSeverity.WARNING, now, Icons.Default.Thermostat))
        }
        Unit
    }

    // Battery alerts
    device.battery?.let { bat ->
        if (bat.percent < 10 && !bat.powerPlugged) {
            alerts.add(AlertItem("Battery Critical", "Battery at ${bat.percent.toInt()}%. Connect charger.", AlertSeverity.CRITICAL, now, Icons.Default.BatteryAlert))
        } else if (bat.percent < 20 && !bat.powerPlugged) {
            alerts.add(AlertItem("Battery Low", "Battery at ${bat.percent.toInt()}%.", AlertSeverity.WARNING, now, Icons.Default.BatteryAlert))
        }
        Unit
    }

    // General info
    if (alerts.isEmpty()) {
        alerts.add(AlertItem("System Healthy", "All metrics are within normal ranges.", AlertSeverity.INFO, now, Icons.Default.CheckCircle))
    }

    val connDisplay = if (device.ipAddress.contains("trycloudflare.com")) "Remote Tunnel" else "${device.ipAddress}:${device.port}"
    alerts.add(AlertItem("Device Connected", "Monitoring active • $connDisplay", AlertSeverity.INFO, "Active", Icons.Default.Wifi))

    return alerts
}

@Composable
fun AlertsTabSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                com.rygent.monitor.ui.components.SkeletonLoader(
                    modifier = Modifier.width(70.dp).height(32.dp),
                    baseColor = DarkSurface,
                    highlightColor = Color.DarkGray
                )
            }
        }
        
        repeat(3) {
            com.rygent.monitor.ui.components.SkeletonLoader(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                baseColor = DarkSurface,
                highlightColor = Color.DarkGray
            )
        }
    }
}
