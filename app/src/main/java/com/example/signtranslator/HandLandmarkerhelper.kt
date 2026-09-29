package com.example.signtranslator

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(context: Context) {
    private var handLandmarker: HandLandmarker? = null

    init {
        try {
            setupHandLandmarker(context)
            android.util.Log.d("HandTracker", "✅ HandLandmarker initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("HandTracker", "❌ Failed to initialize HandLandmarker", e)
        }
    }

    private fun setupHandLandmarker(context: Context){
        val baseOption = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOption)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.8f)
            .setMinHandPresenceConfidence(0.8f)
            .setMinTrackingConfidence(0.8f)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)

    }

    fun detectHand(bitmap: Bitmap): HandLandmarkerResult?{
        val mpImage = BitmapImageBuilder(bitmap).build()
        return handLandmarker?.detect(mpImage)
    }

    fun close(){
        handLandmarker?.close()
    }
}