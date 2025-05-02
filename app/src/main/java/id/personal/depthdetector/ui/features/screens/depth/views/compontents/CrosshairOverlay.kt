package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun CrosshairOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = 20f

        // Draw circle
        drawCircle(
            color = Color.White,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Draw crosshair lines
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(centerX - radius - 10, centerY),
            end = androidx.compose.ui.geometry.Offset(centerX + radius + 10, centerY),
            strokeWidth = 2f
        )

        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(centerX, centerY - radius - 10),
            end = androidx.compose.ui.geometry.Offset(centerX, centerY + radius + 10),
            strokeWidth = 2f
        )
    }
}