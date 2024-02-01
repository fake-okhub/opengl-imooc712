package com.example.android.camera2.cameraxdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageCapture
import com.example.android.camera2.cameraxdemo.databinding.ActivityMainBinding

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

        viewBinding.imageCaptureButton.setOnClickListener( { takePhoto() })
    }

    private fun takePhoto() {
        Log.d("CameraXDemo", "test......")
    }
}