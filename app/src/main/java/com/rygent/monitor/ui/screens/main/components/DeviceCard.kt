package com.rygent.monitor.ui.screens.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.DeviceStatus
import com.rygent.monitor.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

@Composable
fun DeviceCard(
    device: Device,
    modifier: Modifier = Modifier,
    onItemClick: (Device) -> Unit,
    onDeleteClick: (Device) -> Unit,
    onEditClick: (Device) -> Unit = {}
) {
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showHelpDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Device?", color = TextWhite) },
            text = { Text("Are you sure you want to remove '${device.name}'?", color = TextGray) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick(device)
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = PrimaryBlue)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = WarningOrange)
                    Spacer(Modifier.width(8.dp))
                    Text("Connection Guide", color = TextWhite, fontWeight = FontWeight.Bold) 
                }
            },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("If device is offline, try the following steps:", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    BulletPoint("Ensure the monitor agent is running on the target computer.")
                    BulletPoint("Check if the IP Address or Hostname is correct.")
                    BulletPoint("Ensure the firewall allows connections on port ${device.port}.")
                    BulletPoint("Ensure the mobile device and computer are on the same network.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    com.rygent.monitor.ui.components.GlassCard(
        onClick = { onItemClick(device) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Header: Icon, Name, Menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            com.rygent.monitor.ui.components.GlowIcon(
                imageVector = if (device.name.contains("Server", true)) Icons.Default.Dns else Icons.Default.Computer,
                color = if (device.status == DeviceStatus.ONLINE) PrimaryBlue else TextGray.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (device.status == DeviceStatus.ONLINE) TextWhite else TextWhite.copy(alpha = 0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (device.status == DeviceStatus.ONLINE) SuccessGreen else ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = if (device.status == DeviceStatus.ONLINE) {
                            val displayAddr = if (device.ipAddress.contains("trycloudflare.com")) "Remote \uD83C\uDF10" else device.ipAddress
                            "Online • $displayAddr"
                        } else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.status == DeviceStatus.ONLINE) SuccessGreen else ErrorRed,
                        maxLines = 1
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextGray
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Configuration", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            onEditClick(device)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorRed) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (device.status == DeviceStatus.ONLINE) {
            // CPU Load
            com.rygent.monitor.ui.components.PremiumProgressBar(
                progress = device.cpuUsage / 100f,
                color = if (device.cpuUsage > 80) WarningOrange else PrimaryBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CPU Load", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Text("${device.cpuUsage}%", style = MaterialTheme.typography.labelSmall, color = TextWhite, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RAM Usage
            com.rygent.monitor.ui.components.PremiumProgressBar(
                progress = device.ramUsage / 100f,
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RAM Usage", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Text("${device.ramUsage}%", style = MaterialTheme.typography.labelSmall, color = TextWhite, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer: Latency & Uptime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.latency}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = device.uptime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
            }
        } else {
            // OFFLINE STATE: Dimmed Header and Action Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                // Skeleton bars for "missing data"
                com.rygent.monitor.ui.components.SkeletonLoader(
                    modifier = Modifier.fillMaxWidth(0.7f).height(12.dp),
                    baseColor = DarkSurface,
                    highlightColor = DarkBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                com.rygent.monitor.ui.components.SkeletonLoader(
                    modifier = Modifier.fillMaxWidth(0.5f).height(12.dp),
                    baseColor = DarkSurface,
                    highlightColor = DarkBackground
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onItemClick(device) }, // Re-check connection
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                    ) {
                        Text("Try connecting", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("View guide", fontSize = 11.sp, color = TextGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = PrimaryBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextGray, fontSize = 12.sp)
    }
}

@Composable
fun MetricBar(
    label: String,
    value: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
            Text(
                text = "$value%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = value / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.background // Darker track
        )
    }
}
