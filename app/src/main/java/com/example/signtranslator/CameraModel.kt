package com.example.signtranslator

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


class CameraModel: ViewModel() {

    data class HandInfo(
        val label: String, // "Left" atau "Right"
        val score: Float,  // Tingkat keyakinan (0.0 - 1.0)
        val landmarks: List<Triple<Float, Float, Float>>
    )

    data class HandTrackingState(
        val hands: List<HandInfo> = emptyList(),
        val imageWidth: Int = 1,      // Wajib ada untuk kalkulasi skala
        val imageHeight: Int = 1,     // Wajib ada untuk kalkulasi skala
        val isMirrored: Boolean = false // Untuk penanganan kamera depan
    ) {
        val landmarks: List<List<Triple<Float, Float, Float>>>
            get() = hands.map { it.landmarks }
    }
    private lateinit var handlandmarkerHelper: HandLandmarkerHelper
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val isProcessing = AtomicBoolean(false)

    private val _trackingState = MutableStateFlow(HandTrackingState())
    val trackingState: StateFlow<HandTrackingState> = _trackingState.asStateFlow()

    /**
     * Mengembalikan keypoints terdeteksi dalam format array 3D [x, y, z]:
     * List dari tangan -> List dari 21 landmark -> List [x, y, z]
     * Contoh 1 tangan: [[[x0, y0, z0], [x1, y1, z1], ...]]
     * Contoh 2 tangan: [[[x0, y0, z0], ...], [[x0, y0, z0], ...]]
     */
    fun getKeypointsAsArray(): List<List<List<Float>>> {
        return trackingState.value.hands.map { hand ->
            hand.landmarks.map { (x, y, z) -> listOf(x, y, z) }
        }
    }


    fun initialize(context: Context){
        handlandmarkerHelper = HandLandmarkerHelper(context)
    }

    fun setupCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalyse = ImageAnalysis.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyse.setAnalyzer(cameraExecutor){imageProxy ->
                processImage(imageProxy)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyse
                )
            }catch (e: Exception){
                e.printStackTrace()
            }
        }, context.mainExecutor)

    }
    private fun processImage(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)

            val result = handlandmarkerHelper.detectHand(rotatedBitmap)

            val landmarksList = result?.landmarks() ?: emptyList()
            val handednessList = result?.handedness() ?: emptyList()

            val detectedHands = landmarksList.mapIndexed { index, handLandmarks ->
                val category = handednessList.getOrNull(index)?.firstOrNull()
                val label = category?.categoryName() ?: "Unknown" // "Left" atau "Right"
                val score = category?.score() ?: 0f

                val landmarks = handLandmarks.map { landmark ->
                    Triple(landmark.x(), landmark.y(), landmark.z())
                }

                HandInfo(label = label, score = score, landmarks = landmarks)
            }

            // Print info tangan (Left/Right + Score) & keypoints 3D ke Logcat
            detectedHands.forEachIndexed { index, hand ->
                val handArray = hand.landmarks.map { (x, y, z) -> listOf(x, y, z) }
                val scorePercent = String.format(Locale.US, "%.1f%%", hand.score * 100)
                Log.d("HandKeypoints", "Tangan ${index + 1} [${hand.label}] (Akurasi: $scorePercent): $handArray")
            }

            viewModelScope.launch {
                _trackingState.value = HandTrackingState(
                    hands = detectedHands,
                    imageWidth = rotatedBitmap.width,
                    imageHeight = rotatedBitmap.height,
                    isMirrored = false // Set ke true jika menggunakan DEFAULT_FRONT_CAMERA
                )
            }

            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap{
        if(degrees == 0){
            return bitmap
        }
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0,0, bitmap.width, bitmap.height, matrix, true)

    }

    override fun onCleared() {
        super.onCleared()
        handlandmarkerHelper.close()
        cameraExecutor.shutdown()
    }
}