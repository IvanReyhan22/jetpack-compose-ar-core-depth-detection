package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import com.google.ar.core.Frame
import com.google.ar.core.Session

@SuppressLint("ViewConstructor")
class ARGLSurfaceView(
    context: Context,
    sessionProvider: () -> Session?,
    onFrame: (Frame) -> Unit,
    onSurfaceReady: () -> Unit
) : GLSurfaceView(context) {
    init {
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(ARRenderer(context,sessionProvider, onFrame, onSurfaceReady))
        renderMode = RENDERMODE_CONTINUOUSLY
        setWillNotDraw(false)
    }
}