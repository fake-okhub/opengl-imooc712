package com.example.camera2app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.camera2app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private var mCamera: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null

    private var mHandler: Handler? = null
    private var mSurfaceTexture: SurfaceTexture? = null

    private var mGLView: MyGLSurfaceView? = null
    private var mRender: MyGLRenderer? = null

    private var mPreviewSize: Size = Size(0,0)

    private lateinit var mOrientationEventListener: MyOrientationEventListener
    private var mSensorOrientation = 0
    private var mPhoneOrientation = 0

    class MyOrientationEventListener(context: Context): OrientationEventListener(context){
        private var activity: MainActivity? = null
        override fun onOrientationChanged(orientation: Int) {
            Log.e("Orientation:", "$orientation")
            if(activity != null) {
                val rotation = activity?.getRotation()
                activity?.setPhoneOrietation(rotation!!)
                activity?.calculatePreviewRotation()
            }
        }

        fun setActivity(act: MainActivity){
            this.activity = act
        }
    }

    fun getRotation(): Int {
        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        val screenRotation = display.rotation

        when(screenRotation){
            Surface.ROTATION_0 -> {
                return 0
            }
            Surface.ROTATION_90 -> {
                return 90
            }
            Surface.ROTATION_180 -> {
                return 180
            }
            Surface.ROTATION_270 -> {
                return 270
            }
            else -> {
                return 0
            }
        }
    }

    fun setPhoneOrietation(orientation: Int){
        this.mPhoneOrientation = orientation
    }

    fun startOrientationChangeListener() {
        mOrientationEventListener = MyOrientationEventListener(this)
        mOrientationEventListener.setActivity(this)
        mOrientationEventListener.enable()
    }

    fun calculatePreviewRotation(){
        val result = ( mSensorOrientation - mPhoneOrientation * -1 + 360 ) % 360
        //Log.e("calculatePreviewRotation:", "$result")
        mRender?.setOrientation(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(mGLView == null) {
            mGLView = MyGLSurfaceView(this)
            setContentView(mGLView)
            mGLView?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

            mRender = mGLView?.getRender()
        }

        startOrientationChangeListener()

        if(mHandler == null) {
            mHandler = object: Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    when(msg.what){
                        1 -> {
                            val texture = msg.obj as SurfaceTexture
                            mSurfaceTexture = texture

                            captureVideo()
                        }

                        else -> {
                            Log.w(TAG, "others message: ${msg.what}")
                        }
                    }

                }
            }
        }

        //Request Camera permissions
        if(allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUIRED_CODE_PERMISSIONS)
        }
    }

    override fun onPause() {
        super.onPause()

        mRender?.setHandler(null)

        mGLView?.onPause()
        mCaptureSession?.close()
        mCaptureSession = null

        mCamera?.close()
        mCamera = null
    }

    //代码中的all相当于一个for循环
    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUIRED_CODE_PERMISSIONS) {
            if(allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun captureVideo() {

        if(mCamera == null) return
        if(mSurfaceTexture  == null) return

        mSurfaceTexture?.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
        val surface = Surface(mSurfaceTexture)


        val requestBuilder = mCamera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        requestBuilder?.addTarget(surface)

        val outputs = listOf(surface)
        mCamera?.createCaptureSession(outputs, object: CameraCaptureSession.StateCallback(){
            override fun onConfigured(session: CameraCaptureSession) {
                //TODO("Not yet implemented")
                mCaptureSession= session
                //capture
                session.setRepeatingRequest(requestBuilder!!.build(), null, null)
            }

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                //TODO("Not yet implemented")
            }
        }, null)


    }

    fun chooseBestSize(sizes: Array<Size>, width: Int, height: Int): Size{
        var bestSize: Size = sizes[0]

        var minDiffWidth = Int.MAX_VALUE
        var minDiffHeight = Int.MAX_VALUE

        val targetWidth = width
        val targetHeight = height

        for (size in sizes) {
            val diffWidth = Math.abs(size.width - targetWidth)
            val diffHeight = Math.abs(size.height - targetHeight)

            // 检查分辨率的宽高比例是否是4:3或16:9
            val isAspectRatioValid =
                Math.abs(size.width.toFloat() / size.height - 4.0f / 3.0f) < 0.01f ||
                Math.abs(size.width.toFloat() / size.height - 16.0f / 9.0f) < 0.01f

            if (isAspectRatioValid && (diffWidth < minDiffWidth ||
                                      (diffWidth == minDiffWidth && diffHeight < minDiffHeight))) {
                bestSize = size
                minDiffWidth = diffWidth
                minDiffHeight = diffHeight
            }
        }

        return bestSize
    }

    private fun startCamera() {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var cameraId = manager.cameraIdList[0]
        val characteristics = manager.getCameraCharacteristics(cameraId)
        mSensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]!!
        Log.e("startCamera:", "$mSensorOrientation")
        calculatePreviewRotation()

        val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!

        mPreviewSize = chooseBestSize(map.getOutputSizes(SurfaceTexture::class.java), 1280, 720)

        try {
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    //...
                    mCamera = camera
                    mRender?.setHandler(mHandler)
                    mRender?.setVideoSize(mPreviewSize)
                }

                override fun onDisconnected(p0: CameraDevice) {
                    //TODO("Not yet implemented")
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                    //TODO("Not yet implemented")
                }
            }, null)
        }catch (se: SecurityException){
            Log.e(TAG, se.toString())
        }
    }

    //伴生对象
    //相当于Java中的静态成员,静态方法
    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUIRED_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}