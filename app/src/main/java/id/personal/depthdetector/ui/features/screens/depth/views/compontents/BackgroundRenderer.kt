package id.personal.depthdetector.ui.features.screens.depth.views.compontents

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BackgroundRenderer {

    private val quadCoords = floatArrayOf(
        -1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, -1.0f,
        1.0f, 1.0f
    )

    private val texCoords = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    private val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(quadCoords)
            position(0)
        }

    private val texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var program = 0
    private var textureId = 0

    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureUniformHandle = 0

    fun createOnGlThread(textureId: Int) {
        this.textureId = textureId

        val vertexShaderCode = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        val fragmentShaderCode = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(it, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES30.GL_TRUE) {
                val error = GLES30.glGetProgramInfoLog(it)
                throw RuntimeException("Error linking program: $error")
            }
        }

        positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        texCoordHandle = GLES30.glGetAttribLocation(program, "a_TexCoord")
        textureUniformHandle = GLES30.glGetUniformLocation(program, "u_Texture")

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    fun updateTextureMatrix(frame: Frame) {
        val inputUv = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(floatArrayOf(
                    0.0f, 1.0f,  // Top-left
                    0.0f, 0.0f,  // Bottom-left
                    1.0f, 1.0f,  // Top-right
                    1.0f, 0.0f
//                    0.0f, 0.0f,
//                    0.0f, 1.0f,
//                    1.0f, 0.0f,
//                    1.0f, 1.0f
                ))
                position(0)
            }

        texCoordBuffer.position(0)
        frame.transformDisplayUvCoords(inputUv, texCoordBuffer)

        texCoordBuffer.position(0)
    }

    fun draw() {
        GLES30.glUseProgram(program)

        // Vertex positions
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glEnableVertexAttribArray(positionHandle)

        // Texture coordinates
        texCoordBuffer.position(0)
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
        GLES30.glEnableVertexAttribArray(texCoordHandle)

        // Bind external OES texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glUniform1i(textureUniformHandle, 0)

        // Draw quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        // Cleanup
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: $error")
        }

        return shader
    }
}

/*
class BackgroundRenderer {

    private val quadCoords = floatArrayOf(
        -1.0f, -1.0f, 0.0f, 0.0f,
        -1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, -1.0f, 1.0f, 0.0f,
        1.0f, 1.0f, 1.0f, 1.0f
    )

    private val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(quadCoords)
            position(0)
        }

    private var program = 0
    private var textureId = 0

    // Transform matrix for camera frame adjustment
    private val transformMatrix = FloatArray(16)
    private val transformMatrixBuffer = ByteBuffer.allocateDirect(16 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    // Handles for shader attributes and uniforms
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureUniformHandle = 0
    private var transformMatrixHandle = 0

    fun createOnGlThread(textureId: Int) {
        this.textureId = textureId

        // Initialize transformation matrix as identity
        Matrix.setIdentityM(transformMatrix, 0)

        val vertexShaderCode = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        val fragmentShaderCode = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            uniform mat4 u_TransformMatrix;
            varying vec2 v_TexCoord;
            void main() {
                vec2 transformedCoord = (u_TransformMatrix * vec4(v_TexCoord, 0.0, 1.0)).xy;
                gl_FragColor = texture2D(u_Texture, transformedCoord);
            }
        """

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)

            // Check for linking errors
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(it, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES30.GL_TRUE) {
                val error = GLES30.glGetProgramInfoLog(it)
                throw RuntimeException("Error linking program: $error")
            }
        }

        // Get handles to shader attributes and uniforms
        positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        texCoordHandle = GLES30.glGetAttribLocation(program, "a_TexCoord")
        textureUniformHandle = GLES30.glGetUniformLocation(program, "u_Texture")
        transformMatrixHandle = GLES30.glGetUniformLocation(program, "u_TransformMatrix")

        // Delete shaders as they're no longer needed
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    fun updateTextureMatrix(frame: Frame) {
        // Create input UVs (0,0 to 1,1)
        val uvs = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(
                    floatArrayOf(
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 0.0f,
                        1.0f, 1.0f
                    )
                )
                position(0)
            }

        // Create output buffer
        val transformedUvs = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        // Transform UVs through ARCore
        frame.transformDisplayUvCoords(uvs, transformedUvs)
        // Calculate transformation matrix from the original and transformed UVs
        calculateTransformationMatrix(transformedUvs)
    }

    private fun calculateTransformationMatrix(transformedUvs: FloatBuffer) {
        // Reset to identity matrix
        Matrix.setIdentityM(transformMatrix, 0)

        // Extract transformed coordinates
        transformedUvs.position(0)
        val bottomLeft = floatArrayOf(transformedUvs.get(), transformedUvs.get())
        val topLeft = floatArrayOf(transformedUvs.get(), transformedUvs.get())
        val bottomRight = floatArrayOf(transformedUvs.get(), transformedUvs.get())
        val topRight = floatArrayOf(transformedUvs.get(), transformedUvs.get())

        // Calculate scale and translation
        val scaleX = (topRight[0] - bottomLeft[0])
        val scaleY = (topRight[1] - bottomLeft[1])
        val translateX = bottomLeft[0]
        val translateY = bottomLeft[1]

        // Apply transformations to matrix
        Matrix.translateM(transformMatrix, 0, translateX, translateY, 0f)
        Matrix.scaleM(transformMatrix, 0, scaleX, scaleY, 1f)

        // Copy to buffer for shader
        transformMatrixBuffer.put(transformMatrix)
        transformMatrixBuffer.position(0)
    }

    fun draw() {
        GLES30.glUseProgram(program)

        // Set up vertex attributes
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer)
        GLES30.glEnableVertexAttribArray(positionHandle)

        vertexBuffer.position(2)
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer)
        GLES30.glEnableVertexAttribArray(texCoordHandle)

        // Set up texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glUniform1i(textureUniformHandle, 0)

        // Apply transformation matrix
        transformMatrixBuffer.position(0)
        transformMatrixBuffer.put(transformMatrix)
        transformMatrixBuffer.position(0)
        GLES30.glUniformMatrix4fv(transformMatrixHandle, 1, false, transformMatrixBuffer)

        // Draw the quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        // Clean up
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            // Check for compilation errors
            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] != GLES30.GL_TRUE) {
                val error = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Error compiling shader: $error")
            }

            return shader
        }
    }
}
* */