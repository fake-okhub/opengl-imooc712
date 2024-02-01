package com.example.android.camera2.myglsurfaceview

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Triangle {
    private val vertexShaderCode =
            "uniform  mat4 mTMatrix; " +
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
        0.0f, 0.5f, 0.0f,  // 顶部
        -0.5f, -0.5f, 0.0f,  // 左下角
        0.5f, -0.5f, 0.0f // 右下角
    )

    private var translateMatrix = FloatArray(16)
    //private var angle = 0f

    private val color = floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)

    private var mProgram = 0
    private var mPositionHandle = 0
    private var mTMatrixHandle = 0
    private var mColorHandle = 0

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

        Matrix.setIdentityM(translateMatrix, 0)
        //Matrix.setRotateM(translateMatrix, 0, 90.0f, 0.0f ,0.0f, 1.0f )
        Matrix.scaleM(translateMatrix, 0, 0.5f, 0.5f, 1.0f)
        Matrix.translateM(translateMatrix, 0, 0.5f, 0f, 0f)

        //创建shader，并为其指定源码
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES30.glCreateProgram()
        GLES30.glAttachShader(mProgram, vertexShader)
        GLES30.glAttachShader(mProgram, fragmentShader)

        GLES30.glLinkProgram(mProgram)

    }

    fun draw() {
        //使用program
        GLES30.glUseProgram(mProgram)

        //将数据传递给shader
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition")
        GLES30.glEnableVertexAttribArray(mPositionHandle)
        GLES30.glVertexAttribPointer(mPositionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        mTMatrixHandle = GLES30.glGetUniformLocation(mProgram, "mTMatrix")
        //Log.d("matrix:", translateMatrix.toString())
        GLES30.glUniformMatrix4fv(mTMatrixHandle, 1, false, translateMatrix, 0)

        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor")
        GLES30.glUniform4fv(mColorHandle, 1, color, 0)

        //drawAarray, 绘制三角形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, triangleCoords.size  / 3)

        GLES30.glDisableVertexAttribArray(mPositionHandle)
    }
}