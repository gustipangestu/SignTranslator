package com.example.signtranslator

import android.Manifest
import android.content.pm.PackageManager

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.signtranslator.ui.theme.SignTranslatorTheme
import androidx.lifecycle.viewmodel.compose.*



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HandTrackingScreen()
                }
            }
        }
    }
}

@Composable
fun HandTrackingScreen(viewModel: CameraModel = viewModel()){
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(false)
    }

    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()){
            isGranted: Boolean ->
            hasCameraPermission = isGranted
        }


    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            hasCameraPermission = true
        }else{
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if(hasCameraPermission){
        Box(modifier = Modifier.fillMaxSize()){
            AndroidView(
                factory = {ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        viewModel.setupCamera(ctx, lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val imgWidth = trackingState.imageWidth
                val imgHeight = trackingState.imageHeight

                if (imgWidth > 0 && imgHeight > 0 && trackingState.landmarks.isNotEmpty()) {
                    // Kalkulasi skala dan offset untuk PreviewView.ScaleType.FILL_CENTER
                    val scale = maxOf(
                        canvasWidth / imgWidth.toFloat(),
                        canvasHeight / imgHeight.toFloat()
                    )
                    val scaledWidth = imgWidth * scale
                    val scaledHeight = imgHeight * scale
                    val offsetX = (canvasWidth - scaledWidth) / 2f
                    val offsetY = (canvasHeight - scaledHeight) / 2f

                    // Garis penghubung jari-jari tangan
                    val connections = listOf(
                        // Ibu jari
                        0 to 1, 1 to 2, 2 to 3, 3 to 4,
                        // Telunjuk
                        0 to 5, 5 to 6, 6 to 7, 7 to 8,
                        // Jari Tengah
                        5 to 9, 9 to 10, 10 to 11, 11 to 12,
                        // Jari Manis
                        9 to 13, 13 to 14, 14 to 15, 15 to 16,
                        // Kelingking
                        13 to 17, 17 to 18, 18 to 19, 19 to 20,
                        // Pergelangan ke Kelingking
                        0 to 17
                    )

                    // Skema warna terpisah untuk Tangan 1 dan Tangan 2
                    val handColors = listOf(
                        Color.Green to Color.Red,
                        Color.Cyan to Color.Yellow
                    )

                    trackingState.landmarks.forEachIndexed { handIndex, handLandmarks ->
                        val (lineColor, pointColor) = handColors.getOrElse(handIndex) { Color.Green to Color.Red }

                        // Map koordinat ter-normalisasi ke pixel canvas (z digunakan untuk ukuran titik kedalaman)
                        val mappedPoints = handLandmarks.map { (x, y, z) ->
                            val adjustedX = if (trackingState.isMirrored) (1f - x) else x
                            val pointOffset = Offset(
                                x = adjustedX * scaledWidth + offsetX,
                                y = y * scaledHeight + offsetY
                            )
                            val dynamicRadius = (8f - z * 20f).coerceIn(4f, 16f)
                            Pair(pointOffset, dynamicRadius)
                        }

                        // Gambar garis skeleton
                        connections.forEach { (startIdx, endIdx) ->
                            if (startIdx < mappedPoints.size && endIdx < mappedPoints.size) {
                                drawLine(
                                    color = lineColor,
                                    start = mappedPoints[startIdx].first,
                                    end = mappedPoints[endIdx].first,
                                    strokeWidth = 5f
                                )
                            }
                        }

                        // Gambar titik landmark
                        mappedPoints.forEach { (point, radius) ->
                            drawCircle(
                                color = pointColor,
                                radius = radius,
                                center = point
                            )
                        }
                    }
                }
            }
            val handsSummary = if (trackingState.hands.isEmpty()) {
                "Tidak ada tangan"
            } else {
                trackingState.hands.joinToString { hand ->
                    "${hand.label} (${(hand.score * 100).toInt()}%)"
                }
            }
            Text(
                text = "Tangan Terdeteksi: $handsSummary",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else{
        Text("perlu izin Kamera", color = Color.Red)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SignTranslatorTheme {
        Greeting("Android")
    }
}