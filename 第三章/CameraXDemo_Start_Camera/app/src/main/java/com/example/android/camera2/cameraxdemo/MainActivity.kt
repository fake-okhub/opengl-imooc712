package com.example.android.camera2.cameraxdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.android.camera2.cameraxdemo.databinding.ActivityMainBinding
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * ActivityMainBinding.inflate它的作用是viewBinding绑定activity_main.xml
         * - 保证了数据类型的安全
         * - 防止了空指针
         * - 简洁
         * 总之，比以前使用findViewById的方法要好
         * layoutInflater是一个视图导入器，用于加载XML并将XML转成相应的视图
         */
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if(allPermissionsGranted()){
            //start camera
            startCamera()
        }else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }

        viewBinding.imageCaptureButton.setOnClickListener( { takePhoto() })
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_PERMISSION) {
            if(allPermissionsGranted()){
                //....
                startCamera()
                //Log.d(TAG, "have been granted!")
            }else {
                Toast.makeText(this,
                    "Permissions not granted by the user!",
                    Toast.LENGTH_SHORT)
                finish()
            }
        }
    }

    private fun startCamera(){
        //var 定义一个正常的变量
        //val 定义一个变量，这个变量只能被赋值一次
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        //添加一个侦听方法
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //also说明
            //val preview = Preview.Builder().build()
            //preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                val cameraInfo = camera.cameraInfo
                val cameraControl = camera.cameraControl
                val meteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    viewBinding.viewFinder.width.toFloat(), 
                    viewBinding.viewFinder.height.toFloat())
                val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onScroll(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float
                    ): Boolean {
                        val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 1f
                        val delta = (e1?.y ?: 0f) - (e2?.y ?: 0f)
                        val scale = delta / viewBinding.viewFinder.height.toFloat()
                        val zoomRatio = currentZoomRatio + scale * ZOOM_SENSITIVITY
                        cameraControl.setZoomRatio(zoomRatio.coerceIn(0f, cameraInfo.zoomState.value?.maxZoomRatio ?: 1f))
                        return true
                    }
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        e?.let {
                            val meteringPoint = meteringPointFactory.createPoint(it.x, it.y)
                            val action = FocusMeteringAction.Builder(meteringPoint).build()
                            cameraControl.startFocusAndMetering(action)
                            //cameraControl.setLinearZoom(0.5f)
                        }
                        return true
                    }
                })

                viewBinding.viewFinder.setOnTouchListener { _, event ->
                    gestureDetector.onTouchEvent(event)
                    true
                }

            }catch (exec:Exception) {
                Log.e(TAG, "Failed:", exec)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    private fun takePhoto() {
        Log.d(TAG, "test......")
    }

    companion object {
        private const val ZOOM_SENSITIVITY = 0.1f
        private const val TAG = "CameraXDemo"
        private const val REQUEST_CODE_PERMISSION = 10
        //camera
        //record audio
        //write
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}