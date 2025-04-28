package id.personal.depthdetector.ui.features.screens.depth.states

import android.graphics.Bitmap

// UI state for the depth detector
data class DepthUiState(
    val originalBitmap: Bitmap? = null,
    val depthMapBitmap: Bitmap? = null,
    val isRealTimeMode: Boolean = true
)