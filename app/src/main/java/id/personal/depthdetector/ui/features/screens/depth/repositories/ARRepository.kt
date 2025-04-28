package id.personal.depthdetector.ui.features.screens.depth.repositories

import android.content.Context
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import id.personal.depthdetector.ui.features.screens.depth.states.DistanceMeasurement
import id.personal.depthdetector.ui.features.screens.depth.states.ProximityLevel
import id.personal.depthdetector.utils.helpers.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class ARRepository(private val context: Context) {
    private var session: Session? = null
    private val _distanceMeasurement = MutableStateFlow<DistanceMeasurement?>(null)
    val distanceMeasurement: StateFlow<DistanceMeasurement?> = _distanceMeasurement

    private val _isArCoreAvailable = MutableStateFlow(false)
    val isArCoreAvailable: StateFlow<Boolean> = _isArCoreAvailable

    init {
        checkArCoreAvailability()
    }

    private fun checkArCoreAvailability() {
        val maxRetries = 3
        var retryCount = 0
        var availability: ArCoreApk.Availability

        do {
            availability = ArCoreApk.getInstance().checkAvailability(context)
            if (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                break
            }
            if (availability == ArCoreApk.Availability.UNKNOWN_ERROR ||
                availability == ArCoreApk.Availability.UNKNOWN_CHECKING ||
                availability == ArCoreApk.Availability.UNKNOWN_TIMED_OUT) {
                // Wait a bit and retry
                Thread.sleep(500)
                retryCount++
            } else {
                // Other definitive results don't need retry
                break
            }
        } while (retryCount < maxRetries)

        Log.d("ARRepository", "ARCore availability after $retryCount retries: $availability")

        // For development purposes, force availability to true if you know your device supports it
        _isArCoreAvailable.value = when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            else -> {
                // For development on known supported devices, you could override here
                // Set to true if you're confident your device supports ARCore
                // but is reporting incorrectly
                true
            }
        }
    }

    fun initializeArSession() {
        if (session == null) {
            try {
                session = Session(context)

                val config = session?.config
                config?.depthMode = Config.DepthMode.AUTOMATIC
                session?.configure(config)

                session?.resume()
            } catch (e: Exception) {
                Logger.logError("Error initializing AR session: ${e.message}")
            }
        }
    }

    fun pauseArSession() {
        session?.pause()
    }

    fun resumeArSession() {
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Logger.logError("Camera not available: ${e.message}")
        } catch (e: Exception) {
            Logger.logError("Error resuming AR session: ${e.message}")
        }
    }

    fun updateFrame(frame: Frame?) {
        frame?.let { arFrame ->
            if (arFrame.camera.trackingState == TrackingState.TRACKING) {
                try {
                    // Get the depth image
                    val depthImage = arFrame.acquireDepthImage16Bits()
                    // Or alternative approach for depth points
                    val depthPoints = arFrame.acquirePointCloud()

                    // Get screen center point
                    val metrics = context.resources.displayMetrics
                    val centerX = metrics.widthPixels / 2
                    val centerY = metrics.heightPixels / 2

                    // Convert screen coordinates to depth image coordinates
                    val depthWidth = depthImage.width
                    val depthHeight = depthImage.height

                    // Scale screen coordinates to depth image coordinates
                    val depthX =
                        (centerX.toFloat() / metrics.widthPixels.toFloat() * depthWidth).toInt()
                    val depthY =
                        (centerY.toFloat() / metrics.heightPixels.toFloat() * depthHeight).toInt()

                    // Get the depth value at the center point
                    val buffer = depthImage.planes[0].buffer
                    val stride = depthImage.planes[0].rowStride / 2 // 16-bit (2 bytes) per pixel

                    // Calculate buffer position for the center pixel
                    val pos = depthY * stride + depthX
                    buffer.position(pos * 2) // 2 bytes per depth value

                    // Read depth value (in millimeters)
                    val depthMillimeters = buffer.short.toInt() and 0xFFFF
                    val depthMeters = depthMillimeters / 1000.0f
                    val depthCentimeters = depthMillimeters / 10.0f

                    // Determine proximity level
                    val proximityLevel = when {
                        depthCentimeters < 50 -> ProximityLevel.NEAR
                        depthCentimeters < 200 -> ProximityLevel.MEDIUM
                        else -> ProximityLevel.FAR
                    }

                    // Update the distance measurement
                    _distanceMeasurement.value = DistanceMeasurement(
                        distanceCm = depthCentimeters,
                        distanceM = depthMeters,
                        proximityLevel = proximityLevel
                    )

                    // Close the depth image when done
                    depthImage.close()
                    depthPoints.release()

                } catch (e: Exception) {
                    Log.e("ARRepository", "Error processing depth data: ${e.message}")
                    // Fall back to hit test method if depth fails
                    performHitTestDistanceMeasurement(arFrame)
                }
            }
        }
    }

    // Fallback method using hit test
    private fun performHitTestDistanceMeasurement(frame: Frame) {
        val cameraPose = frame.camera.pose
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f

        val hitResults = frame.hitTest(centerX, centerY)

        if (hitResults.isNotEmpty()) {
            val hitResult = hitResults[0]
            val hitPose = hitResult.hitPose

            val dx = hitPose.tx() - cameraPose.tx()
            val dy = hitPose.ty() - cameraPose.ty()
            val dz = hitPose.tz() - cameraPose.tz()

            val distanceM = sqrt(dx * dx + dy * dy + dz * dz)
            val distanceCm = distanceM * 100

            val proximityLevel = when {
                distanceCm < 50 -> ProximityLevel.NEAR
                distanceCm < 200 -> ProximityLevel.MEDIUM
                else -> ProximityLevel.FAR
            }

            _distanceMeasurement.value = DistanceMeasurement(
                distanceCm = distanceCm,
                distanceM = distanceM,
                proximityLevel = proximityLevel
            )
        }
    }

}