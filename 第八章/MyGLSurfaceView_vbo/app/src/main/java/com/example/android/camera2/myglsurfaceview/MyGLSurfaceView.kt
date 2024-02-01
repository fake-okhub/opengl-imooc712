package com.example.android.camera2.myglsurfaceview

import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES30
import android.opengl.GLSurfaceView

class MyGLSurfaceView : GLSurfaceView {
    private lateinit var myGlRender: MyGLRenderer

    constructor(context: Context) : super(context) {
        //使用的OpenGL ES 版本
        setEGLContextClientVersion(3)

        //为GLSurfaceView设置Renderer，在该函数中会启动一个新的线程来构造EGL环境
        myGlRender = MyGLRenderer()
        setRenderer(myGlRender)
    }

    fun onDestory() {
        myGlRender.onDestroy()
    }
}

class MyGLRenderer : GLSurfaceView.Renderer {
    private lateinit var mTriangle: Triangle

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        //设置OpenGL ES清屏色为红色， R,G,B,A
        GLES30.glClearColor(1.0f, 0.0f, 0.0f, .8f)
        mTriangle = Triangle()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        //设置OpenGLES的视口大小
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        //清空当前缓冲区的颜色缓冲区
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        mTriangle.draw()
    }

    fun onDestroy(){
        mTriangle.release()
    }
}