package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import id.personal.depthdetector.utils.helpers.Logger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(
    private val context: Context,
    private val sessionProvider: () -> Session?,
    private val onFrame: (Frame) -> Unit,
    private val onSurfaceReady: () -> Unit
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private var deviceRotation = Surface.ROTATION_0

    // Texture ID for the camera
    private val textureId = IntArray(1)
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceInitialized = false

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Clear color to black
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        GLES30.glGenTextures(1, textureId, 0)

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0])
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )

        // set camera texture name in ARCore session
        sessionProvider()?.setCameraTextureName(textureId[0])
        backgroundRenderer.createOnGlThread(textureId[0])
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        GLES30.glViewport(0, 0, width, height)

        // Get device rotation
        deviceRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.rotation
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        }
        // Set the screen dimensions for ARCore
        sessionProvider()?.setDisplayGeometry(deviceRotation, width, height)

        if (!surfaceInitialized) {
            surfaceInitialized = true
            onSurfaceReady()
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear the rendering surface
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val session = sessionProvider() ?: return

        try {
            // Update the AR session and get the current frame
            val frame = session.update()
            // Track augmented images
            val updatedImages =
                frame.getUpdatedTrackables(AugmentedImage::class.java)

            // Track augmented images
            for (image in updatedImages) {
                when (image.trackingState) {
                    TrackingState.TRACKING -> {
                        val centerPose = image.centerPose
                        Logger.logInfo("ARCore -> Image detected: ${image.name}, position: $centerPose")
                    }

                    TrackingState.PAUSED -> {
                        Logger.logInfo("ARCore -> Image tracking paused: ${image.name}")
                    }

                    else -> {
                        Logger.logInfo("ARCore -> Image not tracked: ${image.name}")
                    }
                }
            }

            backgroundRenderer.updateTextureMatrix(frame)
            backgroundRenderer.draw()
            // Process the frame
            onFrame(frame)
        } catch (e: CameraNotAvailableException) {
            Logger.logError("ARRenderer -> Camera not available: ${e.message}")
        } catch (e: Exception) {
            Logger.logError("ARRenderer -> Failed to update AR session: ${e.message}")
        }
    }

}
