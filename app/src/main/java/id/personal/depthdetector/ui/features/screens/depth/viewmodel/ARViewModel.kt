package id.personal.depthdetector.ui.features.screens.depth.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import id.personal.depthdetector.ui.features.screens.depth.states.ProximityLevel
import id.personal.depthdetector.utils.helpers.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

@SuppressLint("StaticFieldLeak")
class ARViewModel(
    private val context: Context
) : ViewModel() {

    private val _arCoreAvailable = MutableStateFlow(false)
    val arCoreAvailable: StateFlow<Boolean> = _arCoreAvailable.asStateFlow()

    private val _distance = MutableStateFlow<Float?>(null)
    val distance: StateFlow<Float?> = _distance.asStateFlow()

    private val _proximityLevel = MutableStateFlow<ProximityLevel>(ProximityLevel.UNKNOWN)
    val proximityLevel: StateFlow<ProximityLevel> = _proximityLevel.asStateFlow()

    private val _planesDetected = MutableStateFlow(false)
    val planesDetected: StateFlow<Boolean> = _planesDetected.asStateFlow()

    var arSession: Session? = null

    init {
        checkARCoreAvailability()
    }

    private fun checkARCoreAvailability() {
        try {
            when (ArCoreApk.getInstance().checkAvailability(context)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    _arCoreAvailable.value = true
                    Logger.logInfo("ARViewModel -> ARCore is available and installed")
                }

                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    try {
                        if (ArCoreApk.getInstance().requestInstall(
                                null,
                                true
                            ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
                        ) {
                            Logger.logInfo("ARViewModel -> ARCore installation requested")
                        } else {
                            _arCoreAvailable.value = true
                            Logger.logInfo("ARViewModel -> ARCore is being installed")
                        }
                    } catch (e: Exception) {
                        Logger.logError("ARViewModel -> Error requesting ARCore installation ${e.message}")
                    }
                }

                else -> {
                    _arCoreAvailable.value = false
                    Logger.logError("ARViewModel -> ARCore is not available on this device")
                }
            }
        } catch (e: Exception) {
            Logger.logError("ARViewModel -> Error checking ARCore availability ${e.message}")
            _arCoreAvailable.value = false
        }
    }

    fun createARSession() {
        if (arSession == null) {
            try {
                val session = Session(context)
                val config = Config(session)
                if(session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)){
                    config.depthMode = Config.DepthMode.AUTOMATIC
                }else {
                    config.depthMode = Config.DepthMode.DISABLED
                }
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.focusMode = Config.FocusMode.AUTO
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.focusMode = Config.FocusMode.AUTO
                session.configure(config)
                arSession = session
            } catch (e: UnavailableArcoreNotInstalledException) {
                Logger.logError("ARViewModel -> ARCore not installed ${e.message}")
            } catch (e: UnavailableApkTooOldException) {
                Logger.logError("ARViewModel -> ARCore APK too old ${e.message}")
            } catch (e: UnavailableSdkTooOldException) {
                Logger.logError("ARViewModel -> ARCore SDK too old ${e.message}")
            } catch (e: UnavailableDeviceNotCompatibleException) {
                Logger.logError("ARViewModel -> Device not compatible with ARCore ${e.message}")
            } catch (e: Exception) {
                Logger.logError("ARViewModel -> Failed to create AR session: ${e.message}")
            }
        }
    }

    fun resumeARSession() {
        if (arSession != null) {
            try {
                arSession?.resume()
            } catch (e: CameraNotAvailableException) {
                Logger.logError("ARViewModel -> Camera not available for AR session ${e.message}")
            } catch (e: Exception) {
                Logger.logError("ARViewModel -> Unable to resume session ${e.message}")
            }
        }
    }

    fun pauseARSession() {
        arSession?.pause()
    }

    fun closeARSession() {
        arSession?.close()
        arSession = null
    }

    fun measureDistance(frame: Frame?) {
        frame?.let {
            // Check if tracking is working
            if (it.camera.trackingState != TrackingState.TRACKING) {
                _distance.value = null
                _proximityLevel.value = ProximityLevel.UNKNOWN
//                Logger.logInfo("ARViewModel -> Camera not tracking ${it.camera.trackingState} :: ${it.camera.trackingFailureReason}")
                return
            }

            // Check for detected planes
            val planeIterator = it.getUpdatedTrackables(Plane::class.java).iterator()
            var hasPlanes = false
            while (planeIterator.hasNext()) {
                val plane = planeIterator.next()
                if (plane.trackingState == TrackingState.TRACKING) {
                    hasPlanes = true
                    break
                }
            }
            _planesDetected.value = hasPlanes

            // Get the pose of the device
            val cameraPose = it.camera.pose

            // Get screen
            val screen = context.resources.displayMetrics
            // Check if hit test is possible
            val hitResults = frame.hitTest(screen.widthPixels / 2f, screen.heightPixels / 2f)

            if (hitResults.isNotEmpty()) {
                // Get the closest hit result
                val hitResult = hitResults[0]
                val hitPose = hitResult.hitPose

                // Calculate distance between camera and hit point (in meters)
                val distanceInMeters = calculateDistance(cameraPose, hitPose)
                _distance.value = distanceInMeters

                // Update proximity level based on the distance
                _proximityLevel.value = when {
                    distanceInMeters < 0.5f -> ProximityLevel.CLOSEST
                    distanceInMeters < 1.0f -> ProximityLevel.NEAR
                    distanceInMeters < 2.0f -> ProximityLevel.MEDIUM
                    distanceInMeters < 5.0f -> ProximityLevel.FAR
                    else -> ProximityLevel.VERY_FAR
                }

                Logger.logInfo("ARViewModel -> Distance measured: $distanceInMeters meters, Proximity: ${_proximityLevel.value}")
            } else {
                _distance.value = null
                _proximityLevel.value = ProximityLevel.UNKNOWN
                if (hasPlanes) {
                    Logger.logInfo("ARViewModel -> No object detected at center to measure distance (Planes detected: $hasPlanes)")
                } else {
                    Logger.logInfo("ARViewModel -> No surface detected to measure distance")
                }
            }
        }
    }

    private fun calculateDistance(pose1: Pose, pose2: Pose): Float {
        // Calculate Euclidean distance between two poses
        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    // Format distance for display (meters or centimeters)
    fun getFormattedDistance(): String {
        val currentDistance = distance.value ?: return "Distance: unknown"

        return when {
            currentDistance < 1.0f -> {
                // Display in centimeters if less than 1 meter
                val cm = (currentDistance * 100).roundToInt()
                "Distance: $cm cm"
            }

            else -> {
                // Display in meters if 1 meter or more
                val roundedMeter = (currentDistance * 10).roundToInt() / 10f
                "Distance: $roundedMeter m"
            }
        }
    }

    fun getFullDistanceInfo(): String {
        val currentDistance = distance.value ?: return "Distance: unknown"
        val cm = (currentDistance * 100).roundToInt()
        val roundedMeter = (currentDistance * 10).roundToInt() / 10f

        return "Distance: $roundedMeter m ($cm cm)\nProximity: ${proximityLevel.value}"
    }

}