package id.personal.depthdetector.ui.features.screens.depth.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.personal.depthdetector.ui.features.screens.depth.states.DepthUiState
import id.personal.depthdetector.ui.features.screens.depth.states.ProximityResult
import id.personal.depthdetector.utils.helpers.Logger
import id.personal.depthdetector.utils.helpers.MiDASModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class DepthViewModel(
    application: Application
) : ViewModel() {
    /// model initialization
    private val midasModel = MiDASModel(application)

    /// background processing executor
    private val processingExecutor = Executors.newSingleThreadExecutor()

    /// UI State
    private val _uiState = MutableStateFlow(DepthUiState())
    val uiState: StateFlow<DepthUiState> = _uiState.asStateFlow()

    // Add these new properties to the ViewModel
    private val _estimatedDistance = MutableStateFlow<Float?>(null)
    val estimatedDistance: StateFlow<Float?> = _estimatedDistance

    /// proximity result
    private val _proximityResult = MutableStateFlow<ProximityResult>(ProximityResult.NONE)
    val proximityResult: StateFlow<ProximityResult> = _proximityResult

    private var BASELINE_DISTANCE = 1.5f   // Reference distance in meters
    private var BASELINE_DEPTH = 150f      // Reference depth value at the known distance

    /// processing state
    private var isCurrentlyProcessing = false

    /// process frame depth
    fun processFrame(bitmap: Bitmap) {
        // Skip if already processing a frame
        if (isCurrentlyProcessing) return

        isCurrentlyProcessing = true
        viewModelScope.launch {
            try {
                val resizedBitmap = withContext(Dispatchers.Default) {
                    /// resize bitmap to model's input size
                    if (bitmap.width != 256 || bitmap.height != 256) {
                        bitmap.scale(256, 256)
                    } else {
                        bitmap
                    }
                }

                /// process image depth
                val depthMap = withContext(Dispatchers.Default) {
                    midasModel.getDepthMap(resizedBitmap)
                }

                // Draw center dot on both bitmaps
                val originalWithDot = drawCenterDot(resizedBitmap.copy(Bitmap.Config.ARGB_8888, true))
                val depthMapWithDot = drawCenterDot(depthMap.copy(Bitmap.Config.ARGB_8888, true))

                // Get the depth value at center for display
                val centerX = depthMap.width / 2
                val centerY = depthMap.height / 2
                val centerPixel = depthMap[centerX, centerY]
                val depthValue = android.graphics.Color.red(centerPixel)
                Logger.logInfo("Depth Value: $depthValue")
                when {
                    depthValue > 200 -> {
                        _proximityResult.value = ProximityResult.CLOSE
                    }
                    depthValue > 180 -> {  // No need for the upper bound check since we already handled >200
                        _proximityResult.value = ProximityResult.MEDIUM
                    }
                    else -> {  // This handles all remaining cases (≤100)
                        _proximityResult.value = ProximityResult.FAR
                    }
                }
                val estimatedDistance = if (depthValue > 0) {
                    BASELINE_DISTANCE * (BASELINE_DEPTH / depthValue)
                } else {
                    0f
                }

                // Estimate distance at the center of the depth map
//                val distance = estimateDistanceFromDepthMap(depthMap)

                /// update UI state
                _uiState.value = _uiState.value.copy(
                    originalBitmap = originalWithDot,
                    depthMapBitmap = depthMapWithDot
                )
                _estimatedDistance.value = estimatedDistance
            } catch (e: Exception) {

            } finally {
                isCurrentlyProcessing = false
            }
        }

    }

    // Function to estimate distance from the depth map
    private fun estimateDistanceFromDepthMap(depthMap: Bitmap): Float {
        // Get the depth value at the center of the image
        val centerX = depthMap.width / 2
        val centerY = depthMap.height / 2

        // Get the pixel color at the center
        val centerPixel = depthMap[centerX, centerY]

        // For grayscale images, the R, G, B values should be the same
        // Extract the grayscale value (0-255)
        val depthValue = android.graphics.Color.red(centerPixel).toFloat()

        // Convert to meters using the calibration constants
        // Using a simple inverse relationship: distance = baseline_distance * (baseline_depth / depth_value)
        Logger.logInfo("Depth Value: $depthValue")
        when {
            depthValue > 190 -> {
                _proximityResult.value = ProximityResult.CLOSE
            }
            depthValue > 150 -> {  // No need for the upper bound check since we already handled >200
                _proximityResult.value = ProximityResult.MEDIUM
            }
            else -> {  // This handles all remaining cases (≤100)
                _proximityResult.value = ProximityResult.FAR
            }
        }
        val estimatedDistance = if (depthValue > 0) {
            BASELINE_DISTANCE * (BASELINE_DEPTH / depthValue)
        } else {
            0f
        }

        return estimatedDistance
    }

    // Function to draw a dot at the center of a bitmap
    private fun drawCenterDot(bitmap: Bitmap): Bitmap {
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val dotRadius = 5f

        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw the center dot
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), dotRadius, paint)

        // Draw crosshair for better visibility
        paint.strokeWidth = 2f
        paint.style = android.graphics.Paint.Style.STROKE

        val lineLength = 10f
        canvas.drawLine(
            centerX - lineLength - dotRadius, centerY.toFloat(),
            centerX - dotRadius, centerY.toFloat(),
            paint
        )
        canvas.drawLine(
            centerX + dotRadius, centerY.toFloat(),
            centerX + lineLength + dotRadius, centerY.toFloat(),
            paint
        )
        canvas.drawLine(
            centerX.toFloat(), centerY - lineLength - dotRadius,
            centerX.toFloat(), centerY - dotRadius,
            paint
        )
        canvas.drawLine(
            centerX.toFloat(), centerY + dotRadius,
            centerX.toFloat(), centerY + lineLength + dotRadius,
            paint
        )

        return bitmap
    }

    /// on clear viewmodel
    override fun onCleared() {
        super.onCleared()
        processingExecutor.shutdown()
    }
}