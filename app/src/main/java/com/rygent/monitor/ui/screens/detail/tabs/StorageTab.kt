package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.SmartDrive
import com.rygent.monitor.domain.model.DiskIo
import com.rygent.monitor.domain.model.LargeFile
import com.rygent.monitor.ui.theme.*

@Composable
fun StorageTab(
    isOnline: Boolean,
    smartDrives: List<SmartDrive>,
    diskIo: List<DiskIo>,
    largeFiles: List<LargeFile>,
    isLoadingSmart: Boolean = false,
    isLoadingFiles: Boolean = false,
    onRefreshSmart: () -> Unit = {},
    onScanLargeFiles: () -> Unit = {}
) {
    var isIoExpanded by remember { mutableStateOf(true) }

    if (!isOnline) {
        StorageTabSkeleton()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Drive Health (S.M.A.R.T.) ────────────────────────
        SectionHeader3("DRIVE HEALTH", Icons.Default.FavoriteBorder, onAction = onRefreshSmart, actionLabel = if (isLoadingSmart) "Loading..." else "Scan")

        if (smartDrives.isEmpty() && !isLoadingSmart) {
            EmptyCard("Tap 'Scan' to check drive health via S.M.A.R.T.")
        }

        smartDrives.forEach { drive ->
            SmartDriveCard(drive)
        }

        // ── Real-time Disk I/O ───────────────────────────────
        SectionHeader3(
            title = "DISK I/O", 
            icon = Icons.Default.Speed,
            onToggle = { isIoExpanded = !isIoExpanded },
            isExpanded = isIoExpanded
        )

        if (isIoExpanded) {
            if (diskIo.isEmpty()) {
                EmptyCard("Waiting for I/O data...")
            }

            diskIo.forEach { io ->
                DiskIoCard(io)
            }
        }

        // ── Large File Finder ────────────────────────────────
        SectionHeader3("LARGE FILES", Icons.Default.FolderOpen, onAction = onScanLargeFiles, actionLabel = if (isLoadingFiles) "Scanning..." else "Scan")

        if (largeFiles.isEmpty() && !isLoadingFiles) {
            EmptyCard("Tap 'Scan' to find files over 50MB.")
        }

        largeFiles.forEach { file ->
            LargeFileRow(file)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Section Header ───────────────────────────────────────────

@Composable
private fun SectionHeader3(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: (() -> Unit)? = null,
    actionLabel: String = "Refresh",
    onToggle: (() -> Unit)? = null,
    isExpanded: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            
            if (onToggle != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        onAction?.let {
            TextButton(onClick = it) {
                Text(actionLabel, color = PrimaryBlue, fontSize = 12.sp)
            }
        }
    }
}

// ── S.M.A.R.T. Drive Card ───────────────────────────────────

@Composable
private fun SmartDriveCard(drive: SmartDrive) {
    val healthColor = when (drive.health) {
        "PASSED" -> SuccessGreen
        "FAILED" -> ErrorRed
        else -> WarningOrange
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Model + Health Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(drive.model, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(drive.device, color = TextGray, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .background(healthColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(drive.health, color = healthColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Divider(color = Color.White.copy(alpha = 0.06f))

        // Key Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("Type", drive.type, PrimaryBlue)
            StatChip("Temp", if (drive.temp > 0) "${drive.temp}°C" else "N/A", 
                if (drive.temp > 50) WarningOrange else SuccessGreen)
            StatChip("Capacity", formatBytes(drive.capacityBytes), SecondaryBlue)
        }

        // Detailed Metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("Power On", formatHours(drive.powerOnHours), TextGray)
            StatChip("Cycles", "${drive.powerCycleCount}", TextGray)
            if (drive.ssdLifeLeft >= 0) {
                StatChip("Life Left", "${drive.ssdLifeLeft}%", 
                    if (drive.ssdLifeLeft < 20) ErrorRed else SuccessGreen)
            } else {
                StatChip("Reallocated", "${drive.reallocatedSectors}", 
                    if (drive.reallocatedSectors > 0) ErrorRed else SuccessGreen)
            }
        }

        // SSD Life Progress Bar (if applicable)
        if (drive.ssdLifeLeft >= 0) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SSD Life", color = TextGray, fontSize = 11.sp)
                    Text("${drive.ssdLifeLeft}%", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                val animProgress by animateFloatAsState(
                    targetValue = drive.ssdLifeLeft / 100f,
                    animationSpec = tween(1000)
                )
                LinearProgressIndicator(
                    progress = animProgress,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (drive.ssdLifeLeft < 20) ErrorRed else SuccessGreen,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }
        }

        // Total Written
        if (drive.totalWrittenBytes > 0) {
            Text(
                "Total Written: ${formatBytes(drive.totalWrittenBytes)}",
                color = TextGray, fontSize = 11.sp
            )
        }
    }
}

// ── Disk I/O Card ────────────────────────────────────────────

@Composable
private fun DiskIoCard(io: DiskIo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(io.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Text(
                "${String.format("%.0f", io.readIOPS + io.writeIOPS)} IOPS",
                color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IoMetric("Read", io.readIOPS, io.readBytesPerSec, SuccessGreen)
            IoMetric("Write", io.writeIOPS, io.writeBytesPerSec, WarningOrange)
        }
    }
}

@Composable
private fun IoMetric(label: String, iops: Double, bytesPerSec: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 11.sp)
        Text(
            "${String.format("%.0f", iops)} IOPS",
            color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Text(
            formatBytesPerSec(bytesPerSec),
            color = TextGray, fontSize = 11.sp
        )
    }
}

// ── Large File Row ───────────────────────────────────────────

@Composable
private fun LargeFileRow(file: LargeFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val fileName = file.path.substringAfterLast("/")
            val dirPath = file.path.substringBeforeLast("/")
            Text(fileName, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dirPath, color = TextGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(formatBytes(file.sizeBytes), color = WarningOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ── UI Helpers ───────────────────────────────────────────────

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextGray, fontSize = 13.sp)
    }
}

// ── Format Utilities ─────────────────────────────────────────

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", value, units[unitIndex])
}

private fun formatBytesPerSec(bps: Double): String {
    if (bps <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var value = bps
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", value, units[unitIndex])
}

private fun formatHours(hours: Long): String {
    return when {
        hours < 24 -> "${hours}h"
        hours < 24 * 365 -> "${hours / 24}d"
        else -> String.format("%.1fy", hours / (24.0 * 365.0))
    }
}

@Composable
fun StorageTabSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            com.rygent.monitor.ui.components.SkeletonLoader(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                baseColor = DarkSurface,
                highlightColor = Color.DarkGray
            )
        }
    }
}
