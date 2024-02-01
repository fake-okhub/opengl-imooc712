package com.example.android.camera2.myglsurfaceview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureRender {

    private lateinit var mContext : Context

    private val vertexShaderCode =
            "uniform mat4 uTMatrix;" +
            "attribute vec4 aPosition;" +
            "attribute vec2 aTexCoord;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  gl_Position = uTMatrix * aPosition;" +
            "  vTexCoord = aTexCoord;" +
            "}"

    private val fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D uSampler ;" +
            "varying vec2 vTexCoord ;" +
            "void main() {" +
            "  gl_FragColor = texture2D(uSampler, vTexCoord);" +
            "}"

    private val coordData = floatArrayOf(
        //顶点坐标           纹理坐标
        -1.0f, 1.0f, 0.0f, 0.0f, 0.0f,  //左上角
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, //左下角
        1.0f, 1.0f, 0.0f, 1.0f, 0.0f,   //右上角
        1.0f, -1.0f, 0.0f, 1.0f, 1.0f   //右下角
    )
    private var translateMatrix = FloatArray(16)

    private val color = floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)

    private var mProgram = -1
    private var mPositionHandle = -1
    private var mTexCoordHandle = -1
    private var mTMatrixHandle = -1
    private var mSamplerHandle = -1

    private var mTextureId = -1

    // VBO相关
    private var vboId = IntArray(1)
    private var vaoId = IntArray(1)

    private lateinit var coordBuffer: FloatBuffer

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        return shader
    }

    constructor(context: Context) {

        mContext = context

        mTextureId = uploadTexture()

        val bb = ByteBuffer.allocateDirect(coordData.size * 4)
        bb.order(ByteOrder.nativeOrder())
        coordBuffer = bb.asFloatBuffer()
        coordBuffer.put(coordData)
        coordBuffer.position(0)

        //创建shader，并为其指定源码
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES30.glCreateProgram()
        GLES30.glAttachShader(mProgram, vertexShader)
        GLES30.glAttachShader(mProgram, fragmentShader)

        GLES30.glLinkProgram(mProgram)

        GLES30.glDeleteShader(vertexShader); // 立即释放vertexShader
        GLES30.glDeleteShader(fragmentShader); // 立即释放fragmentShader

        // 生成VBO
        GLES30.glGenBuffers(1, vboId, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bb.capacity() ,
            bb, GLES30.GL_STATIC_DRAW)

        Matrix.setIdentityM(translateMatrix, 0)
        //Matrix.translateM(translateMatrix, 0, 0.5f, 0f, 0f)
        //Matrix.scaleM(translateMatrix, 0, 0.5f, 0.5f, 1.0f)

        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "aPosition")
        GLES30.glEnableVertexAttribArray(mPositionHandle)

        mTexCoordHandle = GLES30.glGetAttribLocation(mProgram, "aTexCoord")
        GLES30.glEnableVertexAttribArray(mTexCoordHandle)

        mTMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uTMatrix")
        mSamplerHandle = GLES30.glGetUniformLocation(mProgram, "uSampler")

        // Unbind the VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        //使用program
        GLES30.glUseProgram(mProgram)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId[0])

        GLES30.glVertexAttribPointer(mPositionHandle, 3, GLES30.GL_FLOAT, false, 5*Float.SIZE_BYTES, 0)
        GLES30.glVertexAttribPointer(mTexCoordHandle, 2, GLES30.GL_FLOAT, false, 5*Float.SIZE_BYTES, 3*Float.SIZE_BYTES)

        GLES30.glUniformMatrix4fv(mTMatrixHandle, 1, false, translateMatrix, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)
        GLES30.glUniform1i(mSamplerHandle, 0)

        //drawAarray, 绘制矩型
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        // 解绑VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun release(){
        GLES30.glDeleteProgram(mProgram)

        GLES30.glDeleteBuffers(1, vboId, 0)
        //GLES30.glDeleteVertexArrays(1, vaoId, 0)
    }

    private fun loadImage() : Bitmap {
        val options  = BitmapFactory.Options()
        options.inScaled = false
        return BitmapFactory.decodeResource(mContext.resources, R.drawable.cub, options)
    }

    private fun uploadTexture(): Int {
        val bitmap = loadImage()

        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                                GLES30.GL_TEXTURE_MIN_FILTER,
                                GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                                GLES30.GL_TEXTURE_MAG_FILTER,
                                GLES30.GL_LINEAR)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,
                            0,
                           GLES30.GL_RGBA,
                           bitmap,
                     0)
        bitmap.recycle()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return textureIds[0]
    }
}