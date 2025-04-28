package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.ARViewModel

@Composable
fun ARDistanceDisplay(
    viewModel: ARViewModel
) {
    val distanceMeasurement by viewModel.distanceMeasurement.collectAsState()
    val isArCoreAvailable by viewModel.isArCoreAvailable.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Camera Preview will be handled in the MainActivity

        // Distance measurement display
        distanceMeasurement?.let { measurement ->
            DistanceInfoCard(measurement)
        }

        // Center point indicator
        CenterPointIndicator()

        // Status indicators
        if (!isArCoreAvailable) {
            Text(
                text = "ARCore is not available on this device",
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

    }
}