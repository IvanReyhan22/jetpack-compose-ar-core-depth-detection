package id.personal.depthdetector.ui.features.screens.depth.viewmodel

import android.content.Context
import android.view.Surface
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import id.personal.depthdetector.utils.helpers.Logger
import java.io.IOException

class ARSessionLifecycle(
    private val context: Context,
    private val onFrameAvailable: (Frame) -> Unit
) : DefaultLifecycleObserver {
    private var session: Session? = null
    private var installRequested = false

    override fun onResume(owner: LifecycleOwner) {
        if (session == null) {
            try {
                // Check if ARCore is installed and up to date
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                if (availability.isSupported) {
                    if (ArCoreApk.getInstance().requestInstall(null, !installRequested) ==
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED
                    ) {
                        installRequested = true
                        return
                    }

                    // Create the session
                    session = Session(context)

                    // Configure the session
                    val config = Config(session)
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO
                    session?.configure(config)

                    session?.resume()
                    Logger.logInfo("ARSessionLifecycle -> AR Session resumed")
                } else {
                    Logger.logInfo("ARSessionLifecycle -> ARCore not supported on this device")
                }
            } catch (e: UnavailableDeviceNotCompatibleException) {
                Logger.logError("ARSessionLifecycle -> Device not compatible with ARCore ${e.message}")
            } catch (e: CameraNotAvailableException) {
                Logger.logError("ARSessionLifecycle -> Camera not available ${e.message}")
            } catch (e: Exception) {
                Logger.logError("ARSessionLifecycle -> Failed to create AR session ${e.localizedMessage}")
            }
        } else {
            try {
                session?.resume()
                Logger.logInfo("ARSessionLifecycle -> AR Session resumed")
            } catch (e: CameraNotAvailableException) {
                Logger.logError("ARSessionLifecycle -> Camera not available ${e.message}")
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        session?.pause()
        Logger.logInfo("ARSessionLifecycle -> AR Session paused")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        session?.close()
        session = null
        Logger.logInfo("ARSessionLifecycle -> AR Session closed")
    }

    fun updateSession(surface: Surface, width: Int, height: Int) {
        try {
            session?.let {
                it.setCameraTextureName(0)
                it.setDisplayGeometry(Surface.ROTATION_0, width, height)
            }
        } catch (e: Exception) {
            Logger.logError("ARSessionLifecycle -> Failed to update AR session ${e.message}")
        }
    }

    fun onDrawFrame() {
        session?.let { session ->
            try {
                // Sync rendering and camera data
//                session.setCameraTextureName(0) // Will be set by CameraX

                // Get the current frame
                val frame = session.update()

                // Notify the listener about the new frame
                onFrameAvailable(frame)

            } catch (e: Exception) {
                Logger.logError("ARSessionLifecycle -> Exception on the ARCore drawing thread $e")
            }
        }
    }

    fun getSession(): Session? = session
}
class UnavailableDeviceNotCompatibleException : IOException("This device is not compatible with ARCore")