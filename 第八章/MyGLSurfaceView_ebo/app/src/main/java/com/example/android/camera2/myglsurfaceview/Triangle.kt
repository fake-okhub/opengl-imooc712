package com.example.android.camera2.myglsurfaceview

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Triangle {
    private val vertexShaderCode =
            "uniform mat4 mTMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = mTMatrix * vPosition;" +
            "}"

    private val fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}"

    private val triangleCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f,   // 左上角
        -0.5f, -0.5f, 0.0f,  // 左下角
        0.5f, -0.5f, 0.0f,   // 右下角
        0.5f, 0.5f, 0.0f     // 右上角
    )
    private var translateMatrix = FloatArray(16)

    private val color = floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)

    private var mProgram = 0
    private var mPositionHandle = 0
    private var mColorHandle = 0
    private var mTMatrixHandle = 0

    // VBO相关
    private var vboId = IntArray(1)
    // EBO
    private var eboId = IntArray(1)

    private var indics = intArrayOf(0, 1, 2, 2, 3, 0)

    private lateinit var vertexBuffer: FloatBuffer

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)

        return shader
    }

    constructor() {

        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleCoords)
        vertexBuffer.position(0)

        var idxBuffer = ByteBuffer.allocateDirect(indics.size * 4)
                        .order(ByteOrder.nativeOrder()).asIntBuffer()
        idxBuffer.put(indics).position(0)

        Matrix.setIdentityM(translateMatrix, 0)
        //Matrix.translateM(translateMatrix, 0, 0.5f, 0f, 0f)
        //Matrix.scaleM(translateMatrix, 0, 0.5f, 0.5f, 1.0f)

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
        // Unbind the VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        // 生成EBO
        GLES30.glGenBuffers(1, eboId, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId[0])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, idxBuffer.capacity()*4,
            idxBuffer, GLES30.GL_STATIC_DRAW)
        // Unbind the EBO
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)

        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition")
        GLES30.glEnableVertexAttribArray(mPositionHandle)

        mTMatrixHandle = GLES30.glGetUniformLocation(mProgram, "mTMatrix")
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor")
    }

    fun draw() {
        //使用program
        GLES30.glUseProgram(mProgram)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId[0])
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId[0])

        GLES30.glVertexAttribPointer(mPositionHandle, 3, GLES30.GL_FLOAT, false, 12, 0)
        //将数据传递给shader
        GLES30.glUniformMatrix4fv(mTMatrixHandle, 1, false, translateMatrix, 0)

        GLES30.glUniform4fv(mColorHandle, 1, color, 0)

        //drawAarray, 绘制三角形
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indics.size, GLES30.GL_UNSIGNED_INT, 0)

        // 解绑VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,0)
    }

    fun release(){
        GLES30.glDeleteProgram(mProgram)
        GLES30.glDeleteBuffers(1, vboId, 0)
    }
}