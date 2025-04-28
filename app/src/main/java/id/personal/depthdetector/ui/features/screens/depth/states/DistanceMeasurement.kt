package id.personal.depthdetector.ui.features.screens.depth.states

data class DistanceMeasurement(
    val distanceCm: Float,
    val distanceM: Float,
    val proximityLevel: ProximityLevel
)
