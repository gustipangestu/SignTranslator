package com.example.signtranslator

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.properties.Delegates

class HandRules(val hand: CameraModel.HandInfo, val index: Int) {

    val _handArray = hand.landmarks.map { (x, y, z) -> listOf(x, y, z) }
    lateinit var tanganKiri: List<List<Float>>
    lateinit var tanganKanan: List<List<Float>>
    var kananMenunjuk: Boolean = false
    var kiriMenunjuk: Boolean = false


    fun eksekusi(){


//        Log.d("index tangan", "tangan ke ${index}, ${_handArray}")
            Log.d("jenis", "${hand.label}")
            if (hand.label == "Left"){
                tanganKiri = hand.landmarks.map { (x,y,z) -> listOf(x,y,z)}
                Log.d("tanganKiri", "${tanganKiri}")
                deteksiNunjuk(tanganKiri)
            }else{
                tanganKanan = hand.landmarks.map { (x,y,z) -> listOf(x,y,z) }
                Log.d("tanganKanan", "${tanganKanan}")
            }

    }

    fun deteksiNunjuk(tanganKiri: List<List<Float>>): Boolean{
        val titikTelunjuk = floatArrayOf(tanganKiri[8][0], tanganKiri[8][1], tanganKiri[8][2])
        val titikWirst = floatArrayOf(tanganKiri[0][0], tanganKiri[0][1], tanganKiri[0][2])
        val jarakTelunjukWirst = euclideanDistanceN(titikWirst, titikTelunjuk)
        Log.d("jarakTelunjukWirst", "${jarakTelunjukWirst}")
        return false
    }
    fun euclideanDistanceN(p1: FloatArray, p2: FloatArray): Float {
        require(p1.size == p2.size) { "Vectors must have the same dimensions" }

        // Using traditional loop for high performance with Float primitives
        var sumSquaredDiffs = 0.0f
        for (i in p1.indices) {
            val diff = p2[i] - p1[i]
            sumSquaredDiffs += diff * diff
        }

        return sqrt(sumSquaredDiffs)
    }

    fun klasifikasi(tanganKanan: List<List<Float>>, tanganKiri: List<List<Float>>){
//        huruf A

    }
}