package com.rygent.monitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rygent.monitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val cpuThreshold by viewModel.cpuThreshold.collectAsState()
    val ramThreshold by viewModel.ramThreshold.collectAsState()
    val pollingInterval by viewModel.pollingInterval.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            com.rygent.monitor.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "SYNC SETTINGS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                ThresholdSlider(
                    label = "Polling Interval",
                    value = pollingInterval.toInt(),
                    valueRange = 15f..120f,
                    suffix = " min",
                    onValueChange = { viewModel.updatePollingInterval(it.toLong()) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            com.rygent.monitor.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "NOTIFICATION THRESHOLDS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Set the usage percentage at which you want to receive an alert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                ThresholdSlider(
                    label = "CPU Usage Threshold",
                    value = cpuThreshold,
                    onValueChange = { viewModel.updateCpuThreshold(it.toInt()) }
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                ThresholdSlider(
                    label = "RAM Usage Threshold",
                    value = ramThreshold,
                    onValueChange = { viewModel.updateRamThreshold(it.toInt()) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Version 1.0.0 (Premium Build)",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ThresholdSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float> = 50f..100f,
    suffix: String = "%",
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
            Text("$value$suffix", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryBlue,
                activeTrackColor = PrimaryBlue,
                inactiveTrackColor = DarkBackground,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}



