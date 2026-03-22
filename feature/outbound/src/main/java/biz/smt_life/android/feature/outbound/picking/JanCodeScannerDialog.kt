package biz.smt_life.android.feature.outbound.picking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private const val TAG = "JanCodeScanner"

/**
 * JAN code scanner dialog using CameraX + ML Kit.
 * Shows camera preview with scan line overlay.
 * Auto-captures barcode and compares with expected JAN code.
 *
 * Handles runtime camera permission request internally.
 */
@Composable
fun JanCodeScannerDialog(
    expectedJanCode: String?,
    isInCamera: Boolean = false,
    onResult: (scannedCode: String, isMatch: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isMatch by remember { mutableStateOf<Boolean?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Request permission on first composition if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDFBF2))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JAN確認",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC0392B)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "閉じる",
                            tint = Color(0xFFC0392B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF9A825), thickness = 2.dp)

                // Camera preview area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black)
                ) {
                    when {
                        !hasCameraPermission -> {
                            // Permission not granted
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "カメラの権限が必要です",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD97706)
                                    )
                                ) {
                                    Text("権限を許可する", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        isScanning -> {
                            CameraPreviewWithScanner(
                                isInCamera = isInCamera,
                                onBarcodeDetected = { code ->
                                    if (isScanning) {
                                        isScanning = false
                                        scannedCode = code
                                        val match = !expectedJanCode.isNullOrBlank() && code == expectedJanCode
                                        isMatch = match
                                        onResult(code, match)

                                        // Vibrate and sound on mismatch
                                        if (!match) {
                                            vibrateDevice(context)
                                            playErrorSound()
                                        }
                                    }
                                }
                            )

                            // Scan line overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Red.copy(alpha = 0.8f))
                            )
                        }
                    }
                }

                // Result display
                if (scannedCode != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "読取: $scannedCode",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isMatch == true) {
                            Surface(
                                color = Color(0xFF27AE60),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "JAN一致",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else if (isMatch == false) {
                            Surface(
                                color = Color(0xFFE74C3C),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "不一致",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Retry button
                        Button(
                            onClick = {
                                scannedCode = null
                                isMatch = null
                                isScanning = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Text("再スキャン", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", color = Color(0xFFE67E22), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithScanner(
    isInCamera: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    // Track camera provider for cleanup
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Clean up camera when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding camera", e)
            }
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // Use COMPATIBLE mode (TextureView) for better compatibility inside Dialog
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                try {
                                    @androidx.camera.core.ExperimentalGetImage
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    if (barcode.format == Barcode.FORMAT_EAN_13 ||
                                                        barcode.format == Barcode.FORMAT_EAN_8 ||
                                                        barcode.format == Barcode.FORMAT_UPC_A
                                                    ) {
                                                        barcode.rawValue?.let { code ->
                                                            onBarcodeDetected(code)
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error analyzing image", e)
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = if (isInCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.d(TAG, "Camera bound successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Camera init failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun vibrateDevice(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
        // Vibration not available
    }
}

private fun playErrorSound() {
    try {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 500)
    } catch (_: Exception) {
        // Sound not available
    }
}
