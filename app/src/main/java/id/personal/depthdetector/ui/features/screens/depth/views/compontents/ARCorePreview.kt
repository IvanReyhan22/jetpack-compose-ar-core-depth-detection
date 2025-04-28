package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import id.personal.depthdetector.utils.helpers.Logger
import kotlinx.coroutines.Runnable

@Composable
fun ARCorePreview(
    onFrameAvailable: (Frame, SurfaceView) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceView = remember { SurfaceView(context) }

    var session by remember { mutableStateOf<Session?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            surfaceView.apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        try {
                            session = Session(context).apply {
                                val config = Config(this).apply {
                                    depthMode = Config.DepthMode.AUTOMATIC
                                }
                                configure(config)
                                surfaceView.holder.setKeepScreenOn(true)
                                resume()
                            }
                        } catch (e: Exception) {
                            Logger.logError(e.message.toString())
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        session?.close()
                        session = null
                    }
                })

                lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        super.onResume(owner)
                        surfaceView.holder.surface?.let {
                            Logger.logInfo("RESUMED")
                            session?.resume()
                        }
                    }

                    override fun onPause(owner: LifecycleOwner) {
                        super.onPause(owner)
                        Logger.logInfo("PAUSED")
                        session?.pause()
                    }
                })

                post(object : Runnable {
                    override fun run() {
                        session?.let { sessionData ->
                            try {
                                val frame = sessionData.update()
                                onFrameAvailable(frame, surfaceView)
                                sessionData.setCameraTextureName(0)
                            } catch (e: Exception) {
                                Logger.logError("ARCore frame updater error: ${e}")
                            }
                        }
                        postDelayed(this, 30)
                    }

                })
            }
        }
    )
}