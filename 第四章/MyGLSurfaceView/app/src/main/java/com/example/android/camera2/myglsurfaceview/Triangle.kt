package com.example.android.camera2.myglsurfaceview

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Triangle {

    private val vertexShaderCode =
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}"

    private val fragmentShaderCode =
                "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    private val COORDS_PER_VERTEX = 3

    private val triangleCoords = floatArrayOf(
        0.0f,  0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f)

    private val color = floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)

    private lateinit var vertexBuffer: FloatBuffer
    private var mProgram = 0
    private var mPositionHandle = 0
    private var mColorHandle = 0

    constructor(){
        // 初始化顶点缓冲区,存储三角形坐标
        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleCoords)
        vertexBuffer.position(0)

        // 加载并编译顶点着色器
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        // 加载并编译片段着色器
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 创建一个OpenGL程序
        mProgram = GLES30.glCreateProgram()
        // 附加着色器到程序中
        GLES30.glAttachShader(mProgram, vertexShader)
        GLES30.glAttachShader(mProgram, fragmentShader)
        // 链接程序
        GLES30.glLinkProgram(mProgram)
    }

    fun draw() {
        // 使用程序对象
        GLES30.glUseProgram(mProgram);

        // 获取顶点着色器的位置句柄
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");

        // 启用顶点属性数组
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        // 准备三角形坐标数据
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        // 获取片段着色器的颜色句柄
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor")

        // 设置绘制三角形的颜色
        GLES30.glUniform4fv(mColorHandle, 1, color, 0);

        // 绘制三角形
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, triangleCoords.size / COORDS_PER_VERTEX);

        // 禁用顶点属性数组
        GLES30.glDisableVertexAttribArray(mPositionHandle);
    }

    companion object {

        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES30.glCreateShader(type)

            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            return shader
        }

    }
}