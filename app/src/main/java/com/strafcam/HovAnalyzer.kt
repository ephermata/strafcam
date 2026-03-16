package com.strafcam

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class HovAnalyzer : ImageAnalysis.Analyzer {

    // Setup Object Detector (Stream mode for live camera feed)
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
    private val objectDetector = ObjectDetection.getClient(objectDetectorOptions)

    // Setup Text Recognizer for License Plates (ALPR)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // 1. Run Object Detection (Cars, HOV Diamonds, Occupants)
            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    for (detectedObject in detectedObjects) {
                        // TODO: Implement custom logic to flag single-occupant vehicles
                        // Log.d(TAG, "Object found with tracking ID: ${detectedObject.trackingId}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Object detection failed", e)
                }

            // 2. Run Text Recognition (License Plates)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        val text = block.text
                        // Basic regex example for standard WA passenger plate (3 letters, 4 numbers)
                        // In reality, this needs tuning for varying plate types and environmental noise
                        if (text.replace(" ", "").matches(Regex("^[A-Z]{3}\\d{4}$"))) {
                            Log.i(TAG, "Potential License Plate Captured: $text")
                            // TODO: Save to local database alongside the video timestamp
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                }
                .addOnCompleteListener {
                    // CRITICAL: We must close the imageProxy to receive the next frame from the camera
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    companion object {
        private const val TAG = "HovAnalyzer"
    }
}
