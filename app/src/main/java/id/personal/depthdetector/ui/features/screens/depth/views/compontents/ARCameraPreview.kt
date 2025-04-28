package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ARCameraPreview(
    onInitializeArSession: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(previewView) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        preview.surfaceProvider = previewView.surfaceProvider

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview
            )

            val cameraControl = camera.cameraControl
            cameraControl.setZoomRatio(4f)

            // Initialize ARCore session
            onInitializeArSession()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            cameraProvider.unbindAll()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}