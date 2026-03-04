package com.rygent.monitor.ui.screens.adddevice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rygent.monitor.ui.theme.*
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.core.content.ContextCompat
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    code: String? = null,
    onBackClick: () -> Unit = {},
    onSaveClick: (String) -> Unit = {},
    viewModel: AddDeviceViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var deviceName by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5000") }
    var token by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }
    var isTokenVisible by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    
    var isFromQr by remember { mutableStateOf(!code.isNullOrBlank()) }

    // Auto-fill from passed code
    LaunchedEffect(code) {
        if (!code.isNullOrBlank()) {
            try {
                // Decode the URL encoded code
                val decodedCode = java.net.URLDecoder.decode(code, "UTF-8")
                val uri = Uri.parse(decodedCode)
                if (uri.scheme == "rygent" || uri.scheme == "giconnect") {
                    ipAddress = uri.host ?: ""
                    // Handle port: 443 for tunnels, explicit port from QR, or default 5000
                    port = when {
                        uri.port == 443 -> "443"
                        uri.port > 0 -> uri.port.toString()
                        ipAddress.contains("trycloudflare") -> "443"
                        else -> "5000"
                    }
                    token = uri.getQueryParameter("token") ?: ""
                    deviceName = uri.getQueryParameter("name") ?: ""
                    isFromQr = true
                    android.util.Log.d("AddDeviceScreen", "QR parsed: ip=$ipAddress, port=$port, tunnel=${ipAddress.contains("trycloudflare")}")
                }
            } catch (e: Exception) {
                android.util.Log.e("AddDeviceScreen", "Failed to parse QR code: ${e.message}")
            }
        }
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showQrScanner = true
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required to scan QR code", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val isSaving by viewModel.isSaving.collectAsState()
    val scope = rememberCoroutineScope()

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrDetected = { qrCode ->
                scope.launch {
                    try {
                        val uri = Uri.parse(qrCode)
                        if (uri.scheme == "rygent" || uri.scheme == "giconnect") {
                            val scIp = uri.host ?: ""
                            // Handle port: 443 for tunnels, explicit port from QR, or default 5000
                            val scPort = when {
                                uri.port == 443 -> "443"
                                uri.port > 0 -> uri.port.toString()
                                scIp.contains("trycloudflare") -> "443"
                                else -> "5000"
                            }
                            val scToken = uri.getQueryParameter("token") ?: ""
                            val scName = uri.getQueryParameter("name") ?: ""

                            ipAddress = scIp
                            port = scPort
                            token = scToken
                            deviceName = scName

                            android.util.Log.d("AddDeviceScreen", "QR scanned: ip=$scIp, port=$scPort, tunnel=${scIp.contains("trycloudflare")}")
                            showQrScanner = false
                            isFromQr = true
                            showValidationError = false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AddDeviceScreen", "QR parse error: ${e.message}")
                        showQrScanner = false
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rygent Remote Monitor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryBlue
                        )
                    }
                },
                actions = {
                    if (!isFromQr) {
                        IconButton(onClick = { 
                            val permission = android.Manifest.permission.CAMERA
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                showQrScanner = true
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = PrimaryBlue
                            )
                        }
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect a laptop on the local network to monitor CPU, RAM, and thermal metrics in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Discovery Section
            val isScanning by viewModel.isScanning.collectAsState()
            val discoveredDevices by viewModel.discoveredDevices.collectAsState()
            val discoveryLog by viewModel.discoveryLog.collectAsState()

            // Prefill from discovery if empty
            LaunchedEffect(discoveredDevices) {
                if (!isFromQr && ipAddress.isBlank() && discoveredDevices.isNotEmpty()) {
                    val first = discoveredDevices.first()
                    if (first.address != "0.0.0.0" && first.address.isNotBlank()) {
                        ipAddress = first.address
                        deviceName = first.name ?: ""
                        port = "5000"  // Changed to Rygent default port
                        if (token.isBlank()) token = com.rygent.monitor.util.Constants.DEFAULT_AUTH_TOKEN
                    }
                }
            }

            if (!isFromQr) {
                com.rygent.monitor.ui.components.GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTO DISCOVERY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryBlue
                        )
                        
                        TextButton(onClick = { 
                            val permission = android.Manifest.permission.CAMERA
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                showQrScanner = true
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect via QR", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { viewModel.scanNetwork() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isScanning,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Network")
                        }
                    }
                    
                    Text(
                        text = discoveryLog,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    val validDevices = discoveredDevices.filter { it.address != "0.0.0.0" && it.address.isNotBlank() }
                    if (validDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        validDevices.forEach { discovered ->
                            Surface(
                                onClick = { 
                                    ipAddress = discovered.address
                                    deviceName = discovered.name ?: ""
                                    port = "5000"
                                    if (token.isBlank()) token = "debug_token_123"
                                    isFromQr = true // Hide discovery after selection
                                    showValidationError = false
                                },
                                color = PrimaryBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Found: ${discovered.name ?: discovered.address}", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Device Identity
            com.rygent.monitor.ui.components.GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DEVICE IDENTITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                PremiumTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = "Device Name",
                    placeholder = "e.g. MacBook Pro M2",
                    icon = Icons.Default.Computer
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "NETWORK CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = "IP Address",
                        placeholder = "192.168.1.15",
                        icon = Icons.Default.Dns,
                        modifier = Modifier.weight(1.8f)
                    )
                    PremiumTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = "Port",
                        placeholder = "8080",
                        icon = null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SECURITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                PremiumTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = "Access Token",
                    placeholder = "••••••••••••••••",
                    icon = Icons.Default.Security,
                    isPassword = true,
                    isTokenVisible = isTokenVisible,
                    onToggleVisibility = { isTokenVisible = !isTokenVisible }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            var validationMessage by remember { mutableStateOf<String?>(null) }
            if (showValidationError && validationMessage != null) {
                Text(
                    text = validationMessage!!,
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Validation helper - Support Hostnames and IPs
            // Relaxed regex to allow domains/hostnames (e.g. laptop.local, my-pc, 192.168.1.1, example.com)
            val ipPattern = Regex("""^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*$""")
            var testResult by remember { mutableStateOf<String?>(null) }
            var isTesting by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = {
                    if (ipAddress.isNotBlank()) {
                        isTesting = true
                        testResult = "Testing..."
                        viewModel.testConnection(ipAddress, port, token) { success, msg ->
                            isTesting = false
                            testResult = if (success) "Connected: $msg" else "Failed: $msg"
                        }
                    } else {
                        testResult = "Please enter IP Address"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
            ) {
                 if (isTesting) {
                     CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                     Spacer(modifier = Modifier.width(8.dp))
                 }
                 Text("Test Connection")
            }

            if (testResult != null) {
                Text(
                    text = testResult!!,
                    color = if (testResult!!.startsWith("Conn")) SuccessGreen else Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    val portNum = port.toIntOrNull()
                    when {
                        deviceName.isBlank() -> { validationMessage = "Device name is required"; showValidationError = true }
                        ipAddress.isBlank() -> { validationMessage = "IP address is required"; showValidationError = true }
                        !ipPattern.matches(ipAddress) -> { validationMessage = "Invalid IP format"; showValidationError = true }
                        portNum == null || portNum !in 1..65535 -> { validationMessage = "Invalid port"; showValidationError = true }
                        token.isBlank() -> { validationMessage = "Token required"; showValidationError = true }
                        else -> {
                            showValidationError = false
                            viewModel.saveDevice(deviceName, ipAddress, port, token.trim()) { id -> onSaveClick(id) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saving...")
                } else {
                    Text(text = "Save Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrDetected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val scanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(executor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            scanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        barcode.rawValue?.let { onQrDetected(it) }
                                                    }
                                                }
                                                .addOnCompleteListener { imageProxy.close() }
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                // Handle exception
                            }
                        }, executor)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .border(2.dp, PrimaryBlue, RoundedCornerShape(24.dp))
                    )

                    Text(
                        "Point camera at Rygent QR code",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 64.dp)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isTokenVisible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            placeholder = { Text(placeholder, color = TextGray.copy(alpha = 0.5f)) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp)) } },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onToggleVisibility ?: {}) {
                        Icon(
                            imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextGray
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isTokenVisible) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedLabelColor = PrimaryBlue,
                unfocusedLabelColor = TextGray,
                cursorColor = PrimaryBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text)
        )
    }
}

