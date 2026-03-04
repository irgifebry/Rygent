package com.rygent.monitor.ui.screens.detail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.rygent.monitor.ui.components.SparklineGraph
import com.rygent.monitor.ui.components.GlassCard
import com.rygent.monitor.ui.components.PremiumProgressBar
import com.rygent.monitor.ui.components.GlowIcon
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.hilt.navigation.compose.hiltViewModel
import com.rygent.monitor.ui.theme.*
import com.rygent.monitor.domain.model.Process
import com.rygent.monitor.domain.model.DeviceStatus
import com.rygent.monitor.domain.model.DiskPartition
import com.rygent.monitor.ui.components.SegmentedControl
import com.rygent.monitor.ui.components.DeviceInfoHeader
import com.rygent.monitor.ui.screens.detail.tabs.DashboardTab
import com.rygent.monitor.ui.screens.detail.tabs.GraphsTab
import com.rygent.monitor.ui.screens.detail.tabs.ProcessesTab
import com.rygent.monitor.ui.screens.detail.tabs.AlertsTab
import com.rygent.monitor.ui.screens.detail.tabs.LogsTab
import com.rygent.monitor.ui.screens.detail.tabs.StorageTab
import com.rygent.monitor.ui.screens.detail.tabs.AnalyticsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val device by viewModel.device.collectAsState()
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val ramHistory by viewModel.ramHistory.collectAsState()
    val diskHistory by viewModel.diskHistory.collectAsState()
    val gpuHistory by viewModel.gpuHistory.collectAsState()
    val netDownloadHistory by viewModel.netDownloadHistory.collectAsState()
    val netUploadHistory by viewModel.netUploadHistory.collectAsState()
    
    // Phase 3
    val smartDrives by viewModel.smartDrives.collectAsState()
    val largeFiles by viewModel.largeFiles.collectAsState()
    val isLoadingSmart by viewModel.isLoadingSmart.collectAsState()
    val isLoadingFiles by viewModel.isLoadingFiles.collectAsState()

    // Phase 4
    val speedTestResult by viewModel.speedTestResult.collectAsState()
    val isSpeedTestRunning by viewModel.isSpeedTestRunning.collectAsState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var actionToConfirm by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // B4: Properly collect SharedFlow as one-shot events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            device?.name ?: "Loading...", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = TextWhite
                        )
                        device?.let {
                            val secondsAgo = (System.currentTimeMillis() - it.lastSeen) / 1000
                            val timeText = when {
                                it.status == DeviceStatus.OFFLINE -> "Offline"
                                it.lastSeen == 0L -> "Never"
                                secondsAgo < 10 -> "Just now"
                                secondsAgo < 60 -> "${secondsAgo}s ago"
                                else -> "${secondsAgo / 60}m ago"
                            }
                            Text(
                                "Last synced: $timeText", 
                                style = MaterialTheme.typography.labelSmall,
                                color = if (it.status == DeviceStatus.OFFLINE) ErrorRed else TextGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryBlue)
                    }
                    
                    var showMoreMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextWhite)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Configuration", color = TextWhite) },
                            onClick = { 
                                showMoreMenu = false
                                showEditDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBlue)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Device", color = Color(0xFFEF4444)) },
                            onClick = { 
                                showMoreMenu = false
                                actionToConfirm = "Remove"
                                showConfirmationDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                            }
                        )
                    }

                    var showControlMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showControlMenu = true }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = WarningOrange)
                    }
                    DropdownMenu(
                        expanded = showControlMenu,
                        onDismissRequest = { showControlMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reboot Device", color = TextWhite) },
                            onClick = { 
                                showControlMenu = false
                                actionToConfirm = "Reboot"
                                showConfirmationDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Suspend Device", color = TextWhite) },
                            onClick = { 
                                showControlMenu = false
                                actionToConfirm = "Suspend"
                                showConfirmationDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shutdown Device", color = WarningOrange) },
                            onClick = { 
                                showControlMenu = false
                                actionToConfirm = "Shutdown"
                                showConfirmationDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite
                )
            )

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirm Action", color = TextWhite) },
                    text = { 
                        val message = if (actionToConfirm == "Remove") 
                            "Are you sure you want to remove this device? This action cannot be undone."
                        else 
                            "Are you sure you want to $actionToConfirm this device?"
                        Text(message, color = TextGray) 
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            when (actionToConfirm) {
                                "Remove" -> viewModel.deleteDevice { onBackClick() }
                                "Reboot" -> viewModel.reboot()
                                "Suspend" -> viewModel.suspend()
                                "Shutdown" -> viewModel.shutdown()
                            }
                            showConfirmationDialog = false
                        }) {
                            val color = when(actionToConfirm) {
                                "Remove", "Shutdown" -> WarningOrange
                                "Suspend" -> PrimaryBlue
                                else -> TextWhite
                            }
                            Text("Confirm", color = color)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text("Cancel", color = TextGray)
                        }
                    },
                    containerColor = DarkBackground,
                    tonalElevation = 0.dp
                )
            }

            // Edit Device Configuration Dialog
            if (showEditDialog && device != null) {
                var editName by remember { mutableStateOf(device!!.name) }
                var editIp by remember { mutableStateOf(device!!.ipAddress) }
                var editPort by remember { mutableStateOf(device!!.port.toString()) }
                var editToken by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
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
                            val portInt = editPort.toIntOrNull() ?: device!!.port
                            viewModel.updateDeviceConfig(
                                editName.trim(),
                                editIp.trim(),
                                portInt,
                                editToken.ifBlank { null }
                            )
                            showEditDialog = false
                        }) {
                            Text("Save", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancel", color = TextGray)
                        }
                    },
                    containerColor = DarkBackground,
                    tonalElevation = 0.dp
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (device != null) {
            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("Summary", "Graphs", "Storage", "Analytics", "Processes", "Alerts", "Logs")
            
            var showAppsSheet by remember { mutableStateOf(false) }
            var showProcessesSheet by remember { mutableStateOf(false) }
            var showDetailSheet by remember { mutableStateOf(false) }
            var detailType by remember { mutableStateOf<DetailType?>(null) }
            var selectedProcess by remember { mutableStateOf<com.rygent.monitor.domain.model.Process?>(null) }
            var selectedApp by remember { mutableStateOf<com.rygent.monitor.domain.model.ApplicationData?>(null) }
            var showKillConfirmationDialog by remember { mutableStateOf(false) }
            var processToKill by remember { mutableStateOf<com.rygent.monitor.domain.model.Process?>(null) }

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                DeviceInfoHeader(device = device!!)

                Spacer(modifier = Modifier.height(12.dp))

                if (device?.status == com.rygent.monitor.domain.model.DeviceStatus.OFFLINE) {
                    com.rygent.monitor.ui.components.OfflineBanner(onRetry = { viewModel.refresh() })
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SegmentedControl(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onIndexChanged = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val currentDevice = device!!
                
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val animationSpec = tween<IntOffset>(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing
                            )
                            if (targetState > initialState) {
                                slideInHorizontally(animationSpec = animationSpec) { it }.togetherWith(
                                    slideOutHorizontally(animationSpec = animationSpec) { -it })
                            } else {
                                slideInHorizontally(animationSpec = animationSpec) { -it }.togetherWith(
                                    slideOutHorizontally(animationSpec = animationSpec) { it })
                            }.using(SizeTransform(clip = false))
                        },
                        label = "tab_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> DashboardTab(
                                device = currentDevice,
                                dlHistory = netDownloadHistory,
                                ulHistory = netUploadHistory,
                                onMetricClick = { type ->
                                    detailType = when(type) {
                                        "CPU" -> DetailType.CPU
                                        "RAM" -> DetailType.RAM
                                        "GPU" -> DetailType.GPU
                                        else -> null
                                    }
                                    if (detailType != null) {
                                        showDetailSheet = true
                                    }
                                },
                                onTabChange = { selectedTab = it }
                            )
                            1 -> GraphsTab(
                                device = currentDevice,
                                cpuHistory = cpuHistory,
                                ramHistory = ramHistory,
                                gpuHistory = gpuHistory
                            )
                            2 -> StorageTab(
                                isOnline = currentDevice.status == com.rygent.monitor.domain.model.DeviceStatus.ONLINE,
                                smartDrives = smartDrives,
                                diskIo = currentDevice.diskIo ?: emptyList(),
                                largeFiles = largeFiles,
                                isLoadingSmart = isLoadingSmart,
                                isLoadingFiles = isLoadingFiles,
                                onRefreshSmart = { viewModel.loadSmartDrives() },
                                onScanLargeFiles = { viewModel.scanLargeFiles() }
                            )
                            3 -> AnalyticsTab(
                                isOnline = currentDevice.status == com.rygent.monitor.domain.model.DeviceStatus.ONLINE,
                                speedTestResult = speedTestResult,
                                isSpeedTestRunning = isSpeedTestRunning,
                                onRunSpeedTest = { viewModel.runSpeedTest() }
                            )
                            4 -> ProcessesTab(
                                device = currentDevice,
                                processHistory = viewModel.processHistory.collectAsState().value,
                                onProcessClick = { process ->
                                    processToKill = process
                                    showKillConfirmationDialog = true
                                }
                            )
                            5 -> AlertsTab(
                                device = currentDevice,
                                runawayAlerts = viewModel.runawayAlerts.collectAsState().value
                            )

                            6 -> LogsTab(device = currentDevice)
                        }
                    }
                }

                if (showKillConfirmationDialog && processToKill != null) {
                    AlertDialog(
                        onDismissRequest = { showKillConfirmationDialog = false },
                        title = { Text("End Process?", color = TextWhite) },
                        text = { 
                            Text(
                                "Are you sure you want to end process '${processToKill?.name}' (PID: ${processToKill?.pid})? Unsaved data may be lost.", 
                                color = TextGray
                            ) 
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                processToKill?.let { 
                                    viewModel.killProcess(it.pid)
                                }
                                showKillConfirmationDialog = false
                                processToKill = null
                            }) {
                                Text("End Process", color = ErrorRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKillConfirmationDialog = false }) {
                                Text("Cancel", color = PrimaryBlue)
                            }
                        },
                        containerColor = DarkBackground,
                        tonalElevation = 0.dp
                    )
                }

                if (showDetailSheet && detailType != null) {
                    ModalBottomSheet(
                        onDismissRequest = { 
                            showDetailSheet = false
                            detailType = null
                            selectedProcess = null
                            selectedApp = null
                        },
                        containerColor = DarkBackground,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = TextGray) }
                    ) {
                        val coreUsageHistory by viewModel.coreUsageHistory.collectAsState()

                        DetailSheetContent(
                            type = detailType!!,
                            device = device!!,
                            process = selectedProcess,
                            app = selectedApp,
                            history = when(detailType) {
                                DetailType.CPU -> cpuHistory
                                DetailType.RAM -> ramHistory
                                DetailType.DISK -> diskHistory
                                DetailType.GPU -> gpuHistory
                                else -> emptyList()
                            },
                            coreUsageHistory = coreUsageHistory,
                            onKillProcess = { proc ->
                                processToKill = proc
                                showKillConfirmationDialog = true
                            }
                        )
                    }
                }

                if (showKillConfirmationDialog && processToKill != null) {
                    AlertDialog(
                        onDismissRequest = { showKillConfirmationDialog = false },
                        title = { Text("Terminate Process?", color = TextWhite) },
                        text = { Text("Are you sure you want to terminate '${processToKill?.name}' (PID: ${processToKill?.pid})?", color = TextGray) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.killProcess(processToKill!!.pid)
                                showKillConfirmationDialog = false
                            }) {
                                Text("Terminate", color = ErrorRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKillConfirmationDialog = false }) {
                                Text("Cancel", color = TextGray)
                            }
                        },
                        containerColor = DarkBackground,
                        tonalElevation = 0.dp
                    )
                }

                if (showAppsSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAppsSheet = false },
                        containerColor = DarkBackground,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = TextGray) }
                    ) {
                        AppListSheet(
                            apps = device?.apps ?: emptyList(),
                            onAppClick = { app ->
                                selectedApp = app
                                detailType = DetailType.APP
                                showAppsSheet = false
                                showDetailSheet = true
                            }
                        )
                    }
                }

                if (showProcessesSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showProcessesSheet = false },
                        containerColor = DarkBackground,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = TextGray) }
                    ) {
                        ProcessListSheet(
                            processes = device?.processes ?: emptyList(),
                            onProcessClick = { proc ->
                                selectedProcess = proc
                                detailType = DetailType.PROCESS
                                showProcessesSheet = false
                                showDetailSheet = true
                            }
                        )
                    }
                }
            }
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                 CircularProgressIndicator(color = PrimaryBlue)
             }
        }
    }
}

@Composable
fun CoreMetricsSection(
    usagePerCore: List<Int>?,
    freqPerCore: List<Double>?,
    globalFrequency: Double? = null,
    coreUsageHistory: Map<Int, List<Float>> = emptyMap()
) {
    if (usagePerCore == null || usagePerCore.isEmpty()) return

    Column {
        SectionHeader("CPU CORES", Icons.Default.Speed)
        val chunked = usagePerCore.chunked(2)
        chunked.forEachIndexed { rowIndex, rowCores ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCores.forEachIndexed { colIndex, usage ->
                    val coreIndex = rowIndex * 2 + colIndex
                    val freq = freqPerCore?.getOrNull(coreIndex)
                    val history = coreUsageHistory[coreIndex] ?: emptyList()
                    
                    GlassCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Text(
                                 text = "Core $coreIndex • $usage%", 
                                 style = MaterialTheme.typography.labelSmall, 
                                 color = TextGray
                             )
                             Text(
                                 text = when {
                                     freq != null && freq > 0 -> "${freq.toInt()} MHz"
                                     globalFrequency != null && globalFrequency > 0 -> "${globalFrequency.toInt()} MHz"
                                     else -> "-- MHz"
                                 },
                                 style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                 color = PrimaryBlue
                             )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Box(modifier = Modifier.height(30.dp).fillMaxWidth()) {
                            if (history.isNotEmpty()) {
                                SparklineGraph(
                                    data = history,
                                    color = if (usage > 80) WarningOrange else SuccessGreen,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = usage / 100f,
                                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = if (usage > 80) WarningOrange else SuccessGreen,
                                    trackColor = GlassWhite
                                )
                            }
                        }
                    }
                }
                if (rowCores.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun DetailSheetContent(
    type: DetailType,
    device: com.rygent.monitor.domain.model.Device,
    process: com.rygent.monitor.domain.model.Process? = null,
    app: com.rygent.monitor.domain.model.ApplicationData? = null,
    history: List<Float> = emptyList(),
    coreUsageHistory: Map<Int, List<Float>> = emptyMap(),
    onKillProcess: (com.rygent.monitor.domain.model.Process) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val title = when(type) {
            DetailType.CPU -> "CPU Status"
            DetailType.RAM -> "Memory Details"
            DetailType.DISK -> "Storage Info"
            DetailType.APP -> "Application Detail"
            DetailType.PROCESS -> "Process Control"
            DetailType.GPU -> "GPU Status"
        }
        
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextWhite, fontWeight = FontWeight.Bold)
        
        if (type in listOf(DetailType.CPU, DetailType.RAM, DetailType.DISK, DetailType.GPU)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "HISTORY TRAFFIC", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    com.rygent.monitor.ui.screens.detail.components.SimpleLineChart(
                        label = "",
                        currentValue = "",
                        data = history,
                        color = when(type) {
                            DetailType.CPU -> if (device.cpuUsage > 80) WarningOrange else PrimaryBlue
                            DetailType.DISK -> SuccessGreen
                            DetailType.GPU -> SuccessGreen
                            else -> PrimaryBlue
                        },
                        drawLabels = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        when(type) {
            DetailType.CPU -> {
                InfoRow("Total Usage", "${device.cpuUsage}%")
                device.cpuFrequency?.let { InfoRow("Clock Speed", "${it.toInt()} MHz") }
                device.hardware?.cpuModel?.let { InfoRow("Model", it) }
                device.hardware?.threads?.let { InfoRow("Logical Processors", "$it") }
                device.hardware?.cpuTemp?.let { 
                    InfoRow("Package Temperature", String.format("%.1f°C", it))
                }
                device.hardware?.coreTemps?.let { temps ->
                    if (temps.isNotEmpty()) {
                        val avg = temps.average()
                        InfoRow("Cores (Avg)", String.format("%.1f°C", avg))
                    }
                }
                
                device.cpuTemp?.let {
                    InfoRow("CPU Temperature", String.format("%.1f°C", it))
                }
                
                device.hardware?.boardTemp?.let { 
                    InfoRow("Board Temperature", String.format("%.1f°C", it))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CoreMetricsSection(
                    usagePerCore = device.cpuUsagePerCore,
                    freqPerCore = device.cpuFreqPerCore,
                    globalFrequency = device.cpuFrequency,
                    coreUsageHistory = coreUsageHistory
                )
            }
            DetailType.RAM -> {
                InfoRow("Usage Percent", "${device.ramUsage}%")
                device.hardware?.ramTotal?.let { InfoRow("Total RAM", String.format("%.2f GB", it)) }
                device.hardware?.ramUsed?.let { InfoRow("Used RAM", String.format("%.2f GB", it)) }
                device.hardware?.ramFree?.let { InfoRow("Free RAM", String.format("%.2f GB", it)) }
                device.hardware?.swapTotal?.let { InfoRow("Swap Total", String.format("%.2f GB", it)) }
                device.hardware?.swapUsed?.let { InfoRow("Swap Used", String.format("%.2f GB", it)) }
            }
            DetailType.DISK -> {
                InfoRow("Global Usage", "${device.diskUsage ?: 0}%")
                device.hardware?.diskPartitions?.let { partitions ->
                    for (part in partitions) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(part.mountpoint, style = MaterialTheme.typography.titleSmall, color = PrimaryBlue)
                        InfoRow("Device", part.device)
                        InfoRow("File System", part.fstype)
                        InfoRow("Usage", "${part.percent}%")
                        InfoRow("Total", String.format("%.2f GB", part.total / (1024.0 * 1024.0 * 1024.0)))
                        InfoRow("Free", String.format("%.2f GB", part.free / (1024.0 * 1024.0 * 1024.0)))
                    }
                }
            }
            DetailType.GPU -> {
                device.gpus?.forEachIndexed { index, gpu ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("GPU ${index + 1}", style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
                    }
                    InfoRow("Model", gpu.name)
                    InfoRow("Usage", "${gpu.usage}%")
                    InfoRow("Memory Used", "${gpu.memory}%")
                    InfoRow("Temperature", "${gpu.temp}°C")
                } ?: device.gpus?.let { 
                    // Fallback for older data if any
                }
            }
            DetailType.APP -> {
                app?.let {
                    InfoRow("Package", it.packageName)
                    InfoRow("Status", "Installed")
                }
            }
            DetailType.PROCESS -> {
                process?.let {
                    InfoRow("Name", it.name)
                    InfoRow("PID", "${it.pid}")
                    InfoRow("CPU Usage", String.format("%.1f%%", it.cpuPercent))
                    InfoRow("Memory RSS", String.format("%.1f MB", it.memoryBytes / (1024.0 * 1024.0)))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onKillProcess(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Terminate Process")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HardwareTabContent(device: com.rygent.monitor.domain.model.Device) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("HARDWARE SPECS", Icons.Default.Computer)
        
        InfoSection(title = "Processor") {
            InfoRow("Architecture", device.hardware?.architecture ?: "Unknown")
            InfoRow("Cores", "${device.hardware?.cores ?: 0}")
            InfoRow("Threads", "${device.hardware?.threads ?: 0}")
            device.hardware?.cpuModel?.let { InfoRow("Model", it) }
        }
        
        InfoSection(title = "Memory & Storage") {
            device.hardware?.ramTotal?.let { InfoRow("RAM Total", String.format("%.2f GB", it)) }
            device.hardware?.diskPartitions?.firstOrNull()?.let { 
                InfoRow("Main Disk", String.format("%.2f GB", it.total / (1024.0 * 1024.0 * 1024.0)))
            }
        }

        device.gpus?.forEachIndexed { index, gpu ->
            InfoSection(title = if (index == 0) "Graphics (GPU)" else "Secondary GPU") {
                InfoRow("Model", gpu.name)
                InfoRow("Load", "${gpu.usage}%")
            }
        }
        
        device.battery?.let {
            InfoSection(title = "Battery Status") {
                InfoRow("Capacity", "${it.percent.toInt()}%")
                InfoRow("Plugged", if(it.powerPlugged) "Yes" else "No")
            }
        }
    }
}

@Composable
fun SoftwareTabContent(
    onAppsClick: () -> Unit,
    onProcessesClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("SYSTEM TOOLS", Icons.Default.Settings)
        
        com.rygent.monitor.ui.components.GlassCard(
            onClick = onAppsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowIcon(Icons.Default.Apps, PrimaryBlue)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Installed Applications", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    Text("View and manage installed apps", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
        
        com.rygent.monitor.ui.components.GlassCard(
            onClick = onProcessesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowIcon(Icons.Default.Terminal, WarningOrange)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Running Processes", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    Text("Inspect and terminate processes", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title, 
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), 
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SystemSummarySection(device: com.rygent.monitor.domain.model.Device) {
    InfoSection(title = "System Summary") {
        InfoRow("Platform", device.system?.os ?: "Unknown")
        InfoRow("Kernel", device.system?.kernel ?: "Unknown")
        InfoRow("Uptime", device.uptime)
    }
}

@Composable
fun OperatingSystemSection(device: com.rygent.monitor.domain.model.Device) {
    InfoSection(title = "Control Panel") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                 Text("Network", style = MaterialTheme.typography.labelSmall, color = TextGray)
                 val displayAddr = if (device.ipAddress.contains("trycloudflare.com")) "Remote \uD83C\uDF10" else device.ipAddress
                 Text(displayAddr, style = MaterialTheme.typography.bodyMedium, color = TextWhite, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Column(modifier = Modifier.weight(1f)) {
                 Text("CPU Temp", style = MaterialTheme.typography.labelSmall, color = TextGray)
                 Text("${device.hardware?.cpuTemp?.toInt() ?: "--"}°C", style = MaterialTheme.typography.bodyMedium, color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppListSheet(
    apps: List<com.rygent.monitor.domain.model.ApplicationData>,
    onAppClick: (com.rygent.monitor.domain.model.ApplicationData) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .fillMaxHeight(0.8f)
    ) {
        Text("Installed Apps", style = MaterialTheme.typography.headlineSmall, color = TextWhite, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search applications...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite
            )
        )
        Spacer(Modifier.height(16.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            com.rygent.monitor.ui.screens.detail.components.AppList(
                apps = filtered,
                onAppClick = onAppClick
            )
        }
    }
}

@Composable
fun ProcessListSheet(
    processes: List<com.rygent.monitor.domain.model.Process>,
    onProcessClick: (com.rygent.monitor.domain.model.Process) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = processes.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .fillMaxHeight(0.8f)
    ) {
        Text("System Processes", style = MaterialTheme.typography.headlineSmall, color = TextWhite, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search processes...", color = TextGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite
            )
        )
        Spacer(Modifier.height(16.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            com.rygent.monitor.ui.screens.detail.components.ProcessList(
                processes = filtered,
                onProcessClick = onProcessClick
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    com.rygent.monitor.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = PrimaryBlue)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = TextWhite) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.bodyMedium, 
            color = TextGray,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
            color = valueColor,
            modifier = Modifier.weight(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun DetailMetricCard(
    label: String,
    value: String,
    data: List<Float>,
    color: Color,
    onClick: () -> Unit = {}
) {
    com.rygent.monitor.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextGray)
                Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = TextWhite)
            }
            
            Box(modifier = Modifier.width(120.dp).height(50.dp)) {
                SparklineGraph(
                    data = data,
                    color = color,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

enum class DetailType {
    CPU, RAM, DISK, APP, PROCESS, GPU
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = PrimaryBlue) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title, 
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), 
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
