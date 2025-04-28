package id.personal.depthdetector.ui.features.screens.depth.views.pages

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import id.personal.depthdetector.ui.features.screens.depth.viewmodel.ARViewModel
import id.personal.depthdetector.ui.features.screens.depth.views.compontents.ARCameraPreview
import id.personal.depthdetector.ui.features.screens.depth.views.compontents.ARDistanceDisplay
import id.personal.depthdetector.utils.helpers.ViewModelFactory
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DepthPage(
    viewModel: ARViewModel = viewModel(factory = ViewModelFactory(LocalContext.current))
) {
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

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

    if (cameraPermissionState.status.isGranted) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ARCameraPreview(onInitializeArSession = { viewModel.initializeArSession() })
            ARDistanceDisplay(viewModel)
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