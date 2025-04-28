package id.personal.depthdetector.utils.helpers

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class RealTimeDepthAnalyzer(
    private val isRealTimeEnabled: () -> Boolean,
    private val onFrameProcessed: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    // Track frame count to avoid processing every single frame
    private var frameCount = 0

    // Process every Nth frame for better performance
    private val FRAME_INTERVAL = 10

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        try {
            frameCount++

            // Only process frames if real-time mode is enabled and it's time to process a frame
            if (isRealTimeEnabled() && frameCount % FRAME_INTERVAL == 0) {
                val mediaImage = imageProxy.image

                if (mediaImage != null) {
                    // Convert YUV to Bitmap
                    val bitmap = imageToBitmap(imageProxy)

                    // Send the bitmap for processing
                    onFrameProcessed(bitmap)
                }
            }
        } finally {
            imageProxy.close()
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageToBitmap(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image!!

        // Proper YUV to RGB conversion
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height),
            100,
            out
        )

        val imageBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Rotate the bitmap if needed based on the image proxy's rotation value
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

        return bitmap
    }
}