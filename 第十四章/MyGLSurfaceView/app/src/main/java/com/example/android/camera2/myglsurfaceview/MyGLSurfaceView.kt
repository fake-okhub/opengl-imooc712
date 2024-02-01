package com.example.android.camera2.myglsurfaceview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix

class MyGLSurfaceView : GLSurfaceView {
    private lateinit var myGlRender: MyGLRenderer

    constructor(context: Context) : super(context) {
        //使用的OpenGL ES 版本
        setEGLContextClientVersion(3)

        //为GLSurfaceView设置Renderer，在该函数中会启动一个新的线程来构造EGL环境
        myGlRender = MyGLRenderer(context)
        setRenderer(myGlRender)
    }

    fun onDestory() {
        myGlRender.onDestroy()
    }

    fun setFaceInfo(faceInfo: FaceInfo){
        myGlRender.setFaceInfo(faceInfo)
    }
}

class MyGLRenderer(context: Context) : GLSurfaceView.Renderer {
    private lateinit var mTextureRender: TextureRender
    private var mContext = context

    private lateinit var bitmap: Bitmap
    private lateinit var dstBitmap: Bitmap

    private var mIWidth = 0
    private var mIHeigth = 0

    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0

    private var mStartX = 0
    private var mStartY = 0
    private var mViewWidth = 0
    private var mViewHeight = 0

    private var mMM = FloatArray(16)
    private var mMV = FloatArray(16)
    private var mProjM = FloatArray(16)
    private var mViewM = FloatArray(16)
    private var mMVP = FloatArray(16)

    init{
        Matrix.setIdentityM(mMM, 0)
        mMM[0]= -1.0f

        bitmap = loadImage()
        dstBitmap = loadDstImage()
        mIWidth = bitmap.width
        mIHeigth = bitmap.height
    }

    fun setFaceInfo(faceInfo: FaceInfo){
        mTextureRender.setFaceInfo(faceInfo)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        //设置OpenGL ES清屏色为红色， R,G,B,A
        GLES30.glClearColor(1.0f, 0.0f, 0.0f, .8f)
        mTextureRender = TextureRender(mContext, bitmap, dstBitmap)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mSurfaceWidth = width
        mSurfaceHeight = height

        //calculateViewport
        calculateViewport2()
        //设置OpenGLES的视口大小
        //GLES30.glViewport(mStartX, mStartY, mViewWidth, mViewHeight)
        GLES30.glViewport(0, 0, width, height)
    }

    private fun calculateViewport(){
        val imageRatio = mIWidth / mIHeigth.toFloat()
        val surfaceRatio = mSurfaceWidth / mSurfaceHeight.toFloat()

        if(imageRatio > surfaceRatio) {
            mViewWidth = mSurfaceWidth
            mViewHeight = (mSurfaceWidth / imageRatio).toInt()
        }else if (imageRatio < surfaceRatio) {
            mViewHeight = mSurfaceHeight
            mViewWidth = (mSurfaceHeight * imageRatio).toInt()
        }else {
            mViewWidth = mSurfaceWidth
            mViewHeight = mSurfaceHeight
        }

        mStartX = (mSurfaceWidth - mViewWidth) /2
        mStartY = (mSurfaceHeight - mViewHeight) /2
    }

    private  fun calculateViewport2() {
        val imageRatio = mIWidth / mIHeigth.toFloat()
        val surfaceRatio = mSurfaceWidth/ mSurfaceHeight.toFloat()

        if(imageRatio > surfaceRatio) {
            val tb = imageRatio / surfaceRatio
            Matrix.orthoM(mProjM, 0, -1.0f, 1.0f, -tb, tb, -1.0f, 1.0f)
        }else if (imageRatio < surfaceRatio) {
            val lr = surfaceRatio / imageRatio
            Matrix.orthoM(mProjM, 0, -lr, lr, -1.0f, 1.0f, -1.0f, 1.0f)
        }else {
            Matrix.orthoM(mProjM, 0, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f)
        }

        Matrix.setLookAtM(mViewM, 0,
            0.0f , 0.0f, -1.0f,
            0.0f, 0.0f,0.0f,
            0.0f, 1.0f, 0.0f)

    }

    private fun loadImage() : Bitmap {
        val options  = BitmapFactory.Options()
        options.inScaled = false
        return BitmapFactory.decodeResource(mContext.resources, R.drawable.ningj, options)
    }

    private fun loadDstImage(): Bitmap {
        val options  = BitmapFactory.Options()
        options.inScaled = false
        return BitmapFactory.decodeResource(mContext.resources, R.drawable.powerman, options)
    }

    override fun onDrawFrame(gl: GL10?) {
        //清空当前缓冲区的颜色缓冲区
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        Matrix.multiplyMM(mMV, 0, mViewM, 0, mMM, 0)
        Matrix.multiplyMM(mMVP, 0,
            mProjM, 0,
            mMV, 0)
        mTextureRender.draw(mMVP)
    }

    fun onDestroy(){
        mTextureRender.release()
    }
}