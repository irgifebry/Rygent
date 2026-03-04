package com.rygent.monitor.ui.screens.detail.tabs

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.domain.model.Device
import com.rygent.monitor.domain.model.SpeedTestResult
import com.rygent.monitor.ui.theme.*

@Composable
fun AnalyticsTab(
    isOnline: Boolean,
    speedTestResult: SpeedTestResult?,
    isSpeedTestRunning: Boolean,
    onRunSpeedTest: () -> Unit
) {
    if (!isOnline) {
        AnalyticsTabSkeleton()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Connectivity & Speed Test ────────────────────────
        SectionHeader4("NETWORK PERFORMANCE", Icons.Default.Wifi, onAction = if (!isSpeedTestRunning) onRunSpeedTest else null, actionLabel = if (isSpeedTestRunning) "Testing..." else "Run Test")

        if (isSpeedTestRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Running speedtest on the agent...", color = TextGray, fontSize = 13.sp)
                    Text("This may take 15-30 seconds", color = TextGray.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
        } else if (speedTestResult != null) {
            SpeedTestCard(speedTestResult)
        } else {
            EmptyCard2("Tap 'Run Test' to measure connection speed from the agent to the internet.")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Section Header ───────────────────────────────────────────

@Composable
private fun SectionHeader4(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: (() -> Unit)? = null,
    actionLabel: String = "Refresh"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        if (onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = PrimaryBlue, fontSize = 12.sp)
            }
        } else {
            TextButton(onClick = {}, enabled = false) {
                Text(actionLabel, color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

// ── SpeedTest Card ──────────────────────────────────────────

@Composable
private fun SpeedTestCard(result: SpeedTestResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ping
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.NetworkPing, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Ping", color = TextGray, fontSize = 12.sp)
                Text(String.format("%.1f ms", result.ping), color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.width(1.dp).height(30.dp), color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1.5f)) {
                Text("Sponsor", color = TextGray, fontSize = 12.sp)
                Text(result.sponsor, color = TextWhite, fontSize = 14.sp, maxLines = 1)
                Text("Server: ${result.server}", color = TextGray, fontSize = 10.sp, maxLines = 1)
            }
            
        }

        Divider(color = Color.White.copy(alpha = 0.06f))

        // Download/Upload speeds
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SpeedMetric("Download", result.downloadBits, SuccessGreen, Icons.Default.Download)
            Divider(modifier = Modifier.width(1.dp).height(50.dp), color = Color.White.copy(alpha = 0.1f))
            SpeedMetric("Upload", result.uploadBits, PrimaryBlue, Icons.Default.Upload)
        }
    }
}

@Composable
private fun SpeedMetric(label: String, bitsPerSec: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val megabits = bitsPerSec / 1000 / 1000
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = TextGray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            String.format("%.1f", megabits),
            color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black
        )
        Text("Mbps", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCard2(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun AnalyticsTabSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.rygent.monitor.ui.components.SkeletonLoader(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            baseColor = DarkSurface,
            highlightColor = androidx.compose.ui.graphics.Color.DarkGray
        )
    }
}
