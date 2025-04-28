package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.personal.depthdetector.utils.helpers.Logger
import id.personal.depthdetector.utils.helpers.RealTimeDepthAnalyzer
import java.util.concurrent.ExecutorService

@Composable
fun CameraPreview(
    isRealTimeMode: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    cameraExecutor: ExecutorService,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                /// preview
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            cameraExecutor, RealTimeDepthAnalyzer(
                                isRealTimeEnabled = { isRealTimeMode },
                                onFrameProcessed = onImageCaptured
                            )
                        )
                    }

                try {
                    /// unbind before rebinding
                    cameraProvider.unbindAll()

                    // bind to lifecycle
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )

                    val cameraControl = camera.cameraControl
                    cameraControl.setZoomRatio(4f)

                } catch (e: Exception) {
                    Logger.logInfo("Binding failed: ${e.message}")
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}