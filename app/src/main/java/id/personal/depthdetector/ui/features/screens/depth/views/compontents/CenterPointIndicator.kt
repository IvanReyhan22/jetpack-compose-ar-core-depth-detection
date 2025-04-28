package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CenterPointIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Simple crosshair
        CircularProgressIndicator(
            modifier = Modifier.padding(16.dp),
            strokeWidth = 2.dp,
            color = Color.White
        )
    }
}