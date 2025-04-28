package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.personal.depthdetector.ui.features.screens.depth.states.DistanceMeasurement
import id.personal.depthdetector.ui.features.screens.depth.states.ProximityLevel

@Composable
fun DistanceInfoCard(measurement: DistanceMeasurement) {
    Card(
        modifier = Modifier
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Distance",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${String.format("%.2f", measurement.distanceCm)} cm",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${String.format("%.2f", measurement.distanceM)} m",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val proximityColor = when (measurement.proximityLevel) {
                ProximityLevel.NEAR -> Color.Red
                ProximityLevel.MEDIUM -> Color.Yellow
                ProximityLevel.FAR -> Color.Green
                ProximityLevel.NONE -> {
                    Color.Transparent
                }
            }

            if (measurement.proximityLevel != ProximityLevel.NONE) {
                Text(
                    text = measurement.proximityLevel.name,
                    color = proximityColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
