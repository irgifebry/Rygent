package com.rygent.monitor.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rygent.monitor.ui.screens.main.components.DeviceCard
import com.rygent.monitor.ui.components.GlassCard
import com.rygent.monitor.ui.components.GlowIcon
import com.rygent.monitor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onAddDeviceClick: (String?) -> Unit = {},
    onDeviceClick: (String) -> Unit = {}
) {
    val devices by viewModel.devices.collectAsState()
    var showScanner by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var deviceToDelete by remember { mutableStateOf<com.rygent.monitor.domain.model.Device?>(null) }
    var deviceToEdit by remember { mutableStateOf<com.rygent.monitor.domain.model.Device?>(null) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        } else {
            android.widget.Toast.makeText(context, "Camera permission needed to scan QR", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Rygent",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Text(
                            text = "SYSTEM MONITORING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
                            color = PrimaryBlue
                        )
                    }
                },
                actions = {
                    val isRefreshing by viewModel.isRefreshing.collectAsState()
                    IconButton(onClick = { viewModel.refreshAll() }, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextWhite)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = TextWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (devices.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, 
                            android.Manifest.permission.CAMERA
                        )
                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            showScanner = true
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    containerColor = PrimaryBlue,
                    contentColor = TextWhite,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Device", fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (devices.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "System Overview",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Wifi,
                            label = "Connected",
                            value = "${devices.count { it.status == com.rygent.monitor.domain.model.DeviceStatus.ONLINE }} Devices",
                            color = SuccessGreen
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.List,
                            label = "Avg RAM Load",
                            value = "${devices.filter { it.status == com.rygent.monitor.domain.model.DeviceStatus.ONLINE }.takeIf { it.isNotEmpty() }?.map { it.ramUsage }?.average()?.toInt() ?: 0}%",
                            color = PrimaryBlue
                        )
                    }
                }
                
                item {
                    Text(
                        text = "Your Devices",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onItemClick = { onDeviceClick(device.id) },
                        onDeleteClick = { deviceToDelete = device },
                        onEditClick = { deviceToEdit = device }
                    )
                }
            } else {
                // REDESIGNED EMPTY STATE
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 60.dp), // Adjust for FAB position
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(PrimaryBlue.copy(alpha = 0.15f), Color.Transparent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Grid-like background effect
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
                                val step = 20.dp.toPx()
                                for (i in 0..5) {
                                    drawLine(TextGray.copy(alpha = 0.1f), start = androidx.compose.ui.geometry.Offset(i * step, 0f), end = androidx.compose.ui.geometry.Offset(i * step, size.height))
                                    drawLine(TextGray.copy(alpha = 0.1f), start = androidx.compose.ui.geometry.Offset(0f, i * step), end = androidx.compose.ui.geometry.Offset(size.width, i * step))
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = PrimaryBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(80.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "No Devices Connected",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = TextWhite
                        )
                        
                        Text(
                            text = "Start by adding your first device to monitor its system performance in real-time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { 
                                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.CAMERA
                                )
                                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    showScanner = true
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .height(56.dp)
                                .padding(horizontal = 32.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add First Device", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }

            item {
                if (deviceToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { deviceToDelete = null },
                        title = { Text("Delete Device", color = TextWhite) },
                        text = { Text("Are you sure you want to delete '${deviceToDelete?.name}'? This will remove all history for this device.", color = TextGray) },
                        confirmButton = {
                            TextButton(onClick = {
                            deviceToDelete?.let { device -> viewModel.deleteDevice(device) }
                                deviceToDelete = null
                            }) {
                                Text("Delete", color = ErrorRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { deviceToDelete = null }) {
                                Text("Cancel", color = TextGray)
                            }
                        },
                        containerColor = DarkBackground,
                        tonalElevation = 0.dp
                    )
                }

                // Edit Device Dialog in MainScreen
                if (deviceToEdit != null) {
                    var editName by remember { mutableStateOf(deviceToEdit!!.name) }
                    var editIp by remember { mutableStateOf(deviceToEdit!!.ipAddress) }
                    var editPort by remember { mutableStateOf(deviceToEdit!!.port.toString()) }
                    var editToken by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { deviceToEdit = null },
                        title = { Text("Edit Device Configuration", color = TextWhite, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Device Name") },
                                    singleLine = true,
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = Color(0xFF2D333F),
                                        focusedLabelColor = PrimaryBlue,
                                        unfocusedLabelColor = TextGray,
                                        cursorColor = PrimaryBlue,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editIp,
                                    onValueChange = { editIp = it },
                                    label = { Text("IP Address / Hostname") },
                                    singleLine = true,
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = Color(0xFF2D333F),
                                        focusedLabelColor = PrimaryBlue,
                                        unfocusedLabelColor = TextGray,
                                        cursorColor = PrimaryBlue,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editPort,
                                    onValueChange = { editPort = it },
                                    label = { Text("Port") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = Color(0xFF2D333F),
                                        focusedLabelColor = PrimaryBlue,
                                        unfocusedLabelColor = TextGray,
                                        cursorColor = PrimaryBlue,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editToken,
                                    onValueChange = { editToken = it },
                                    label = { Text("Access Token (leave blank to keep current)") },
                                    singleLine = true,
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = Color(0xFF2D333F),
                                        focusedLabelColor = PrimaryBlue,
                                        unfocusedLabelColor = TextGray,
                                        cursorColor = PrimaryBlue,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val portInt = editPort.toIntOrNull() ?: deviceToEdit!!.port
                                viewModel.updateDeviceConfig(
                                    deviceToEdit!!.id,
                                    editName.trim(),
                                    editIp.trim(),
                                    portInt,
                                    editToken.ifBlank { null }
                                )
                                deviceToEdit = null
                            }) {
                                Text("Save", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { deviceToEdit = null }) {
                                Text("Cancel", color = TextGray)
                            }
                        },
                        containerColor = DarkBackground,
                        tonalElevation = 0.dp
                    )
                }
            }
            

            item { 
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        
    var isProcessing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showScanner) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showScanner = false 
                    isProcessing = false
                },
                containerColor = Color.Black,
                dragHandle = { BottomSheetDefaults.DragHandle(color = TextGray) },
                modifier = Modifier.fillMaxHeight(0.8f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isProcessing) {
                        com.rygent.monitor.ui.components.QRCodeScanner(
                            onCodeScanned = { code ->
                                if (isProcessing) return@QRCodeScanner
                                isProcessing = true
                                
                                try {
                                    // QR scanned — navigate to AddDevice screen with code
                                    onAddDeviceClick(code)
                                    showScanner = false
                                } catch (e: Exception) {
                                    isProcessing = false
                                    android.widget.Toast.makeText(context, "Invalid QR Format", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        // processing indicator
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                    
                    // Overlay Text
                    Text(
                        "Scan QR Code",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    GlassCard(
        modifier = modifier
    ) {
        GlowIcon(
            imageVector = icon,
            color = color
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextWhite
        )
    }
}
