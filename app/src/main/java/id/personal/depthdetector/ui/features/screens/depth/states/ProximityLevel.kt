package id.personal.depthdetector.ui.features.screens.depth.states

enum class ProximityLevel {
    CLOSEST,
    NEAR,    // Less than 50 cm
    MEDIUM,  // Between 50 cm and 2 meters
    FAR,      // Greater than 2 meters
    VERY_FAR,
    UNKNOWN,
}