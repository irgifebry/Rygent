package com.rygent.monitor.ui.components

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QRCodeScanner(
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                QrCodeAnalyzer { result ->
                    onCodeScanned(result)
                }
            )

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("QRCodeScanner", "Camera binding failed", e)
            }
        }
    )
    
    // Overlay Box
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val scanSize = 250.dp.toPx()
        val left = (w - scanSize) / 2f
        val top = (h - scanSize) / 2f
        val right = left + scanSize
        val bottom = top + scanSize
        val cornerLength = 40.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val color = androidx.compose.ui.graphics.Color.White
        val dimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)

        // Clear center (drawing rects around)
        // Top rect
        drawRect(
            color = dimColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, top)
        )
        // Bottom rect
        drawRect(
            color = dimColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, bottom),
            size = androidx.compose.ui.geometry.Size(w, (h - bottom).coerceAtLeast(0f))
        )
        // Left rect
        drawRect(
            color = dimColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(left, scanSize)
        )
        // Right rect
        drawRect(
            color = dimColor,
            topLeft = androidx.compose.ui.geometry.Offset(right, top),
            size = androidx.compose.ui.geometry.Size((w - right).coerceAtLeast(0f), scanSize)
        )

        // Draw corners
        // Top Left
        drawLine(color, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + cornerLength, top), strokeWidth = strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + cornerLength), strokeWidth = strokeWidth)

        // Top Right
        drawLine(color, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right - cornerLength, top), strokeWidth = strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right, top + cornerLength), strokeWidth = strokeWidth)

        // Bottom Left
        drawLine(color, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left + cornerLength, bottom), strokeWidth = strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left, bottom - cornerLength), strokeWidth = strokeWidth)

        // Bottom Right
        drawLine(color, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right - cornerLength, bottom), strokeWidth = strokeWidth)
        drawLine(color, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right, bottom - cornerLength), strokeWidth = strokeWidth)
    }
}

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let {
                            onQrCodeScanned(it)
                        }
                    }
                }
                .addOnFailureListener {
                    // Failure handling
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
