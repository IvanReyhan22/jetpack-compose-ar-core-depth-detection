package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.personal.depthdetector.ui.features.screens.depth.states.ProximityLevel
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.ARViewModel


@Composable
fun ARDistanceDisplay(
    viewModel: ARViewModel,
    modifier: Modifier = Modifier
) {
    val distance by viewModel.distance.collectAsState()
    val proximityLevel by viewModel.proximityLevel.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Distance in meters and centimeters
            if (distance != null) {
                val distanceInMeters = distance!!
                val distanceInCm = (distanceInMeters * 100).toInt()

                Text(
                    text = "Distance",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${String.format("%.2f", distanceInMeters)} m",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "$distanceInCm cm",
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Proximity indicator
                val (proximityText, proximityColor) = when (proximityLevel) {
                    ProximityLevel.CLOSEST -> "VERY CLOSE" to Color.Red
                    ProximityLevel.NEAR -> "CLOSE" to Color(0xFFFF6D00) // Orange
                    ProximityLevel.MEDIUM -> "MEDIUM" to Color.Yellow
                    ProximityLevel.FAR -> "FAR" to Color(0xFF76FF03) // Light Green
                    ProximityLevel.VERY_FAR -> "VERY FAR" to Color(0xFF00E676) // Green
                    ProximityLevel.UNKNOWN -> "UNKNOWN" to Color.Gray
                }

                Text(
                    text = "Proximity",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = proximityText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = proximityColor
                )
            } else {
                Text(
                    text = "Point at a surface to measure distance",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}