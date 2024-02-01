package com.example.camera2app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.Size

class MyGLSurfaceView : GLSurfaceView {
    private lateinit var myGlRender: MyGLRenderer

    constructor(context: Context) : super(context) {
        //使用的OpenGL ES 版本
        setEGLContextClientVersion(3)

        //为GLSurfaceView设置Renderer，在该函数中会启动一个新的线程来构造EGL环境
        myGlRender = MyGLRenderer(this)
        setRenderer(myGlRender)
    }

    fun onDestory() {
        myGlRender.onDestroy()
    }

    fun getRender(): MyGLRenderer {
        return myGlRender
    }
}

class MyGLRenderer(glView: MyGLSurfaceView) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private lateinit var mTextureRender: TextureRender
//    private var mContext = context

    private var mGLView = glView

//    private lateinit var bitmap: Bitmap

    private var mIWidth = 0
    private var mIHeight = 0

    private var mOldIWidth = 0
    private var mOldIHeight = 0

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

    private var mHandler: Handler? = null

    private var mSurfaceTexture: SurfaceTexture? = null
    private var mDegree = 0
    private var mScreenState = -1

    init{
        Matrix.setIdentityM(mMM, 0)
        //mMM[0]= -1.0f

//        bitmap = loadImage()
//        mIWidth = bitmap.width
//        mIHeigth = bitmap.height
    }

    fun setOrientation(degree: Int){
        if(mOldIWidth ==0 && mOldIHeight == 0) return

        mDegree = degree
        if(mDegree % 180 > 45 && mDegree % 180 < 135){
            if(mScreenState != 0) {
                mIWidth = mOldIHeight
                mIHeight = mOldIWidth
            }
            //Log.e("Renderer:", "$mIWidth, $mIHeight")
            mScreenState =0
        }else {
            mIWidth = mOldIWidth
            mIHeight = mOldIHeight
            mScreenState = 1
        }
    }

    fun setVideoSize(size: Size){
        mOldIWidth = size.width
        mOldIHeight = size.height
    }

    fun setHandler(handler: Handler?){
        if(handler != null) {
            mHandler = handler
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        mGLView?.requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        //设置OpenGL ES清屏色为红色， R,G,B,A
        GLES30.glClearColor(1.0f, 0.0f, 0.0f, .8f)
        mTextureRender = TextureRender()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mSurfaceWidth = width
        mSurfaceHeight = height

        mSurfaceTexture = SurfaceTexture(mTextureRender.getTextureId())
        mSurfaceTexture?.setOnFrameAvailableListener(this)

        val msg = Message.obtain()
        msg.obj = mSurfaceTexture
        msg.what = 1

        if(mHandler != null) {
            mHandler?.sendMessage(msg)
        }

        //calculateViewport
        //calculateViewport2()
        //设置OpenGLES的视口大小
        //GLES30.glViewport(mStartX, mStartY, mViewWidth, mViewHeight)
        GLES30.glViewport(0, 0, width, height)
    }

    private fun calculateViewport(){
        val imageRatio = mIWidth / mIHeight.toFloat()
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
        val imageRatio = mIWidth / mIHeight.toFloat()
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

//    private fun loadImage() : Bitmap {
//        val options  = BitmapFactory.Options()
//        options.inScaled = false
//        return BitmapFactory.decodeResource(mContext.resources, R.drawable.cub, options)
//    }

    override fun onDrawFrame(gl: GL10?) {
        //清空当前缓冲区的颜色缓冲区
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        calculateViewport2()
        Matrix.setRotateM(mMM, 0, mDegree.toFloat(), 0.0f, 0.0f, -1.0f)
        Matrix.multiplyMM(mMV, 0, mViewM, 0, mMM, 0)
        Matrix.multiplyMM(mMVP, 0,
            mProjM, 0,
            mMV, 0)
//        Matrix.setIdentityM(mMVP, 0)
        mSurfaceTexture?.updateTexImage()
        mTextureRender.draw(mMVP, mMVP)
    }

    fun onDestroy(){
//        mTextureRender.release()
    }
}