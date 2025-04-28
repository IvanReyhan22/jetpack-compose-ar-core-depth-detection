package id.personal.depthdetector.ui.features.screens.depth.views.pages

import android.Manifest
import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.DepthViewModel
import id.personal.depthdetector.ui.features.screens.depth.views.compontents.CameraPreview
import id.personal.depthdetector.utils.helpers.ViewModelFactory
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DepthPage(
) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    /// get screen height
    val screenHeight = LocalConfiguration.current.screenHeightDp
    /// 70% screen dp
    val heightInDp = (screenHeight * 0.6).dp

    /// viewmodel initialization
    val viewModel: DepthViewModel =
        viewModel(factory = ViewModelFactory(context.applicationContext as Application))

    /// ui state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /// estimated distance
    val estimatedDistance by viewModel.estimatedDistance.collectAsStateWithLifecycle()
    val proximityResult by viewModel.proximityResult.collectAsStateWithLifecycle()

    /// camera permission state
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    /// request camera permission on first launch
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    /// handle cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cameraPermissionState.status.isGranted) {
            /// camera preview
            Box(
                modifier = Modifier
                    .height(heightInDp)
            ) {
                CameraPreview(
                    isRealTimeMode = uiState.isRealTimeMode,
                    onImageCaptured = viewModel::processFrame,
                    cameraExecutor = cameraExecutor
                )
            }
            Spacer(modifier = Modifier.height(42.dp))

            estimatedDistance?.let { distance ->
                Text(
                    text = "%.2f meters / ${proximityResult.name}".format(distance),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Convert to centimeters for additional reference
                Text(
                    text = "(%.1f cm)".format(distance * 100),
                    style = MaterialTheme.typography.bodyMedium
                )
            } ?: Text(
                text = "Processing...",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display area for original and depth images
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Original image
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Original")
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .background(Color.LightGray)
                    ) {
                        uiState.originalBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Original Image",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Depth map image
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Depth Map")
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .background(Color.LightGray)
                    ) {
                        uiState.depthMapBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Depth Map",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            /// permission denied
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera permission is required for depth detection",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                }
            }
        }
    }
}