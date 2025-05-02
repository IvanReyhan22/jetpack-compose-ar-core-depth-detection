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
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // Camera Preview with ARCore integration
//    val arSessionLifecycle = remember {
//        ARSessionLifecycle(context) { frame ->
//            viewModel.measureDistance(frame)
//            onFrameReceived(frame)
//        }
//    }
//
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_RESUME) {
//                viewModel.createARSession()
//                viewModel.resumeARSession()
//            } else if (event == Lifecycle.Event.ON_PAUSE) {
//                viewModel.pauseARSession()
//            } else if (event == Lifecycle.Event.ON_DESTROY) {
//                viewModel.closeARSession()
//            }
//        }
//
//        lifecycleOwner.lifecycle.addObserver(observer)
//        lifecycleOwner.lifecycle.addObserver(arSessionLifecycle)
//
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//            lifecycleOwner.lifecycle.removeObserver(arSessionLifecycle)
//        }
//    }
//
//    Box(modifier = modifier.fillMaxSize()) {
//
//        // Setup camera provider
//        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//        var previewView: PreviewView? by remember { mutableStateOf(null) }
//
//        LaunchedEffect(cameraProviderFuture) {
//            val cameraProvider = context.getCameraProvider()
//
//            previewView?.let { view ->
//                bindCameraUseCases(
//                    lifecycleOwner = lifecycleOwner,
//                    cameraProvider = cameraProvider,
//                    previewView = view,
//                    arSessionLifecycle = arSessionLifecycle,
//                    onFrameReceived = onFrameReceived
//                )
//            }
//        }
//
//        // Camera Preview
//        AndroidView(
//            modifier = Modifier.fillMaxSize(),
//            factory = { ctx ->
//                val view = PreviewView(ctx).apply {
//                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
//                    scaleType = PreviewView.ScaleType.FILL_CENTER
//                    layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                }
//                previewView = view
//                view
//            },
//            update = { view ->
//                if (previewView == view) {
//                    val cameraProvider = cameraProviderFuture.get()
//                    bindCameraUseCases(
//                        lifecycleOwner = lifecycleOwner,
//                        cameraProvider = cameraProvider,
//                        previewView = view,
//                        arSessionLifecycle = arSessionLifecycle,
//                        onFrameReceived = onFrameReceived
//                    )
//                }
//
//            }
//        )
//
//        // Crosshair in the center
//        CrosshairOverlay(modifier = Modifier.fillMaxSize())
//    }
//}

//private fun bindCameraUseCases(
//    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
//    cameraProvider: ProcessCameraProvider,
//    previewView: PreviewView,
//    arSessionLifecycle: ARSessionLifecycle,
//    onFrameReceived: (Frame) -> Unit,
//) {
//    try {
//        cameraProvider.unbindAll()
//
//        val preview = Preview.Builder().build()
//        preview.surfaceProvider = previewView.surfaceProvider
//
//        val imageAnalysis = ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//
//        val executor = Executors.newSingleThreadExecutor()
//        imageAnalysis.setAnalyzer(executor) { imageProxy ->
//            arSessionLifecycle.getSession()?.let { session ->
//                val frame = session.update()
//                onFrameReceived(frame)
//            }
//            // Process each frame for AR
////            arSessionLifecycle.onDrawFrame()
//            imageProxy.close()
//        }
//
//        cameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            CameraSelector.DEFAULT_BACK_CAMERA,
//            preview,
//            imageAnalysis
//        )
//    } catch (e: Exception) {
//        Logger.logInfo("ARViewModel -> Use case binding failed ${e.message}")
//    }
//}
//
//suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
//    ProcessCameraProvider.getInstance(this).also { future ->
//        future.addListener(
//            {
//                continuation.resume(future.get())
//            },
//            ContextCompat.getMainExecutor(this)
//        )
//    }
//}