package com.example.android.camera2.myglsurfaceview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.PointF
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

class FaceInfo {
    var mCenterLeftEye = PointF(0.0f, 0.0f)
    var mCenterRightEye = PointF(0.0f, 0.0f)

    var mBeginLeftEye = PointF(0.0f, 0.0f)
    var mEndLeftEye = PointF(0.0f, 0.0f)

    var mBeginRightEye = PointF(0.0f, 0.0f)
    var mEndRightEye = PointF(0.0f, 0.0f)

    var mLeftCheekOne = PointF(0.0f, 0.0f)
    var mLeftCheekTwo = PointF(0.0f, 0.0f)
    var mRightCheekOne = PointF(0.0f, 0.0f)
    var mRightCheekTwo = PointF(0.0f, 0.0f)

    var mNoseOne = PointF(0.0f, 0.0f)
    var mNoseTwo = PointF(0.0f, 0.0f)
}

class MainActivity : AppCompatActivity() {
    private lateinit var glView: MyGLSurfaceView
    private var mDetector: FaceDetector? = null
    private var mFaceInfo: FaceInfo = FaceInfo()

    private fun loadImage() : Bitmap {
        val options  = BitmapFactory.Options()
        options.inScaled = false
        return BitmapFactory.decodeResource(this.resources, R.drawable.ningj, options)
    }

    fun detectFace() {
        //loadImage
        val bitmap = loadImage()

        //通过getClient获得MLKit对象
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

        mDetector = FaceDetection.getClient(options)

        //MLKit对象的process方法
        mDetector?.process(bitmap, 0)?.addOnSuccessListener { faces ->
            for (face in faces) {

                val leftEyePos = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                mFaceInfo.mCenterLeftEye = leftEyePos!!
                val rightEyePos = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                mFaceInfo.mCenterRightEye = rightEyePos!!

                val leftEyeContour = face.getContour(FaceContour.LEFT_EYE)?.points
                if(leftEyeContour != null){
                    mFaceInfo.mBeginLeftEye = leftEyeContour[0]
                    mFaceInfo.mEndLeftEye = leftEyeContour[8]
                }
                val rightEyeContour = face.getContour(FaceContour.RIGHT_EYE)?.points
                if(rightEyeContour != null){
                    mFaceInfo.mBeginRightEye = rightEyeContour[0]
                    mFaceInfo.mEndRightEye = rightEyeContour[8]
                }
                val noseContour = face.getContour(FaceContour.NOSE_BRIDGE)?.points
                if(noseContour != null) {
                    mFaceInfo.mNoseOne = noseContour[0]
                    mFaceInfo.mNoseTwo = noseContour[1]
                }

                val faceContour = face.getContour(FaceContour.FACE)?.points
                if(faceContour != null){
                    mFaceInfo.mLeftCheekOne = faceContour[25]
                    mFaceInfo.mLeftCheekTwo = faceContour[23]

                    mFaceInfo.mRightCheekOne = faceContour[11]
                    mFaceInfo.mRightCheekOne = faceContour[13]
                }

                glView.setFaceInfo(mFaceInfo)
            }
        }?.addOnFailureListener {e ->
            Log.e("MainActivity", e.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = MyGLSurfaceView(this)
        setContentView(glView)

        detectFace()
    }

    override fun onPause() {
        super.onPause()

        glView.onPause()
    }

    override fun onResume() {
        super.onResume()

        glView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        glView.onDestory()
    }
}