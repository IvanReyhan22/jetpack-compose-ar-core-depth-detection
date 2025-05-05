package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Frame
import com.google.ar.core.Session
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.ARViewModel

@Composable
fun ARCameraPreview(
    modifier: Modifier = Modifier,
    viewModel: ARViewModel,
    onFrameReceived: (Frame) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // Remember ARCore session state
    val sessionState = remember { mutableStateOf<Session?>(null) }

    val surfaceCreatedState = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.createARSession()
                    sessionState.value = viewModel.arSession

                    if (surfaceCreatedState.value) {
                        viewModel.resumeARSession()
                    }
                }

                Lifecycle.Event.ON_PAUSE -> viewModel.pauseARSession()
                Lifecycle.Event.ON_DESTROY -> viewModel.closeARSession()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ARCore Surface View
        AndroidView(
            factory = {
                ARGLSurfaceView(
                    context = it,
                    sessionProvider = { sessionState.value },
                    onFrame = { frame ->
                        viewModel.measureDistance(frame)
                        onFrameReceived(frame)
                    },
                    onSurfaceReady = {
                        surfaceCreatedState.value = true
                        // Only resume if we have a session
                        viewModel.arSession?.let {
                            viewModel.resumeARSession()
                        }
                    }
                ).apply {
                    visibility = View.VISIBLE
                    translationZ = 1f
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3. Your overlay
        CrosshairOverlay(modifier = Modifier.fillMaxSize())
    }
}