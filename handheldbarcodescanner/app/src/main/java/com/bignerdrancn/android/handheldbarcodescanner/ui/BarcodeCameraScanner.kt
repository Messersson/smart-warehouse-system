@file:androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.bignerdrancn.android.handheldbarcodescanner.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Composable
fun BarcodeCameraScanner(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRequested by remember { mutableStateOf(hasPermission) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                hasPermission = context.hasCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "扫码窗口",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    CameraPreview(
                        enabled = enabled,
                        onCodeScanned = onCodeScanned
                    )
                    ScannerFrame()
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "相机权限未开启",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("授权相机")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    enabled: Boolean,
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val scannerEnabled = remember { AtomicBoolean(enabled) }
    val scannerCallback = remember { AtomicReference(onCodeScanned) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    FORMAT_QR_CODE,
                    FORMAT_CODE_128,
                    FORMAT_CODE_39,
                    FORMAT_CODE_93,
                    FORMAT_CODABAR,
                    FORMAT_EAN_13,
                    FORMAT_EAN_8,
                    FORMAT_ITF,
                    FORMAT_UPC_A,
                    FORMAT_UPC_E,
                    FORMAT_DATA_MATRIX,
                    FORMAT_PDF417,
                    FORMAT_AZTEC
                )
                .build()
        )
    }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    SideEffect {
        scannerEnabled.set(enabled)
        scannerCallback.set(onCodeScanned)
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            runCatching {
                cameraReady = false
                cameraError = null
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = cameraProvider.firstAvailableCameraSelector()
                    ?: error("未检测到可用摄像头")
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            cameraExecutor,
                            MlKitBarcodeAnalyzer(
                                scanner = scanner,
                                isEnabled = { scannerEnabled.get() },
                                onScanned = { code ->
                                    mainExecutor.execute {
                                        scannerCallback.get().invoke(code)
                                    }
                                }
                            )
                        )
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                cameraReady = true
                cameraError = null
            }.onFailure { error ->
                cameraError = error.message ?: "相机启动失败"
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    if (!cameraReady && cameraError == null) {
        CameraOverlayMessage(message = "正在启动相机...")
    }

    cameraError?.let { message ->
        CameraOverlayMessage(message = "相机启动失败：$message")
    }
}

@Composable
private fun CameraOverlayMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun ProcessCameraProvider.firstAvailableCameraSelector(): CameraSelector? {
    val selectors = listOf(
        CameraSelector.DEFAULT_BACK_CAMERA,
        CameraSelector.Builder().build(),
        CameraSelector.DEFAULT_FRONT_CAMERA
    )
    return selectors.firstOrNull { selector ->
        runCatching { hasCamera(selector) }.getOrDefault(false)
    }
}

@Composable
private fun ScannerFrame() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(112.dp)
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp)
            )
    )
}

private class MlKitBarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val isEnabled: () -> Boolean,
    private val onScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastCode = ""
    private var lastEmitAt = 0L

    override fun analyze(imageProxy: ImageProxy) {
        if (!isEnabled()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstLookupValue()
                if (!code.isNullOrBlank()) {
                    emitIfFresh(code.trim())
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun emitIfFresh(code: String) {
        val now = SystemClock.elapsedRealtime()
        if (code == lastCode && now - lastEmitAt < CAMERA_SCAN_REPEAT_MS) {
            return
        }

        lastCode = code
        lastEmitAt = now
        onScanned(code)
    }
}

private fun List<Barcode>.firstLookupValue(): String? =
    firstNotNullOfOrNull { barcode ->
        barcode.rawValue?.trim()?.takeIf { it.isNotBlank() }
    }

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private const val CAMERA_SCAN_REPEAT_MS = 2_000L
