package com.example.android.camera2.myglsurfaceview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextureRender {

    private lateinit var mContext : Context
    private val KERNEL_SIZE = 9
    private val LOOPCOUNT = 200
    private var mFrameIdx = 0

    private val vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 aPosition;" +
            "attribute vec2 aTexCoord;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * aPosition;" +
            "  vTexCoord = aTexCoord;" +
            "}"

    private val fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D uSampler ;" +
            "varying vec2 vTexCoord ;" +
            "void main() {" +
            "  gl_FragColor = texture2D(uSampler, vTexCoord);" +
            "}"

    private val fragmentShader_WB =
                "precision mediump float;" +
                "uniform sampler2D uSampler ;" +
                "varying vec2 vTexCoord ;" +
                "void main() {" +
                "  vec4 tc = texture2D(uSampler, vTexCoord);" +
                "  float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;" +
                "  gl_FragColor = vec4(color, color, color, 1.0); " +
                "}"

    private val fragmentShader_filter = """
                #define KERNEL_SIZE ${KERNEL_SIZE} 
                precision mediump float; 
                uniform samplerfilter2D uSampler ; 
                uniform float uKernel[KERNEL_SIZE]; 
                uniform vec2 uTexOffset[KERNEL_SIZE]; 
                uniform float uColorOffset; 
                varying vec2 vTexCoord ; 
                void main() { 
                  int i = 0; 
                  vec4 sum = vec4(0.0); 
                  for (i = 0; i < KERNEL_SIZE; i++){ 
                      vec4 tc = texture2D(uSampler, vTexCoord + uTexOffset[i]); 
                      sum += tc * uKernel[i]; 
                  } 
                  sum += uColorOffset; 
                  gl_FragColor = sum; 
                }
                """

    private val fragmentShader_sketch = """
                #define KERNEL_SIZE ${KERNEL_SIZE} 
                precision mediump float; 
                uniform sampler2D uSampler ; 
                uniform float uKernel[KERNEL_SIZE]; 
                uniform vec2 uTexOffset[KERNEL_SIZE]; 
                uniform float uColorOffset; 
                varying vec2 vTexCoord ; 
                void main() { 
                  int i = 0; 
                  vec4 sum = vec4(0.0); 
                  for (i = 0; i < KERNEL_SIZE; i++){ 
                      vec4 tc = texture2D(uSampler, vTexCoord + uTexOffset[i]); 
                      sum += tc * uKernel[i]; 
                  } 
                  sum += uColorOffset; 
                  float gray = dot(sum.rgb, vec3(0.3, 0.59, 0.11));
                  float alpha = 1.0 - smoothstep(0.2,0.8, gray);
                  gl_FragColor = mix(vec4(1.0), vec4(0.0,0.0,0.0,1.0), alpha); 
                }
                """

    private val fragmentShader_lut = """
                precision mediump float;
                uniform sampler2D uSampler;
                uniform sampler2D uLutSampler;
                varying vec2 vTexCoord;
                void main() {
                    vec4 texColor = texture2D(uSampler, vTexCoord);
                    float blueColor = texColor.b * 63.0;
                    vec2 quad1, quad2;
                    quad1.y = floor(floor(blueColor) / 8.0);
                    quad1.x = floor(blueColor) - (quad1.y * 8.0);

                    quad2.y = floor(ceil(blueColor) / 8.0);
                    quad2.x = ceil(blueColor) - (quad2.y * 8.0);
                    
                    vec2 texPos1, texPos2;
                    texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + (0.125 - 1.0/512.0) * texColor.r;
                    texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + (0.125 - 1.0/512.0) * texColor.g;
                    texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + (0.125 - 1.0/512.0) * texColor.r;
                    texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + (0.125 - 1.0/512.0) * texColor.g;
                    
                    vec4 newColor1 = texture2D(uLutSampler, texPos1);
                    vec4 newColor2 = texture2D(uLutSampler, texPos2);
                    
                    vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
                    gl_FragColor = newColor;
                }
    """

    private val coordData = floatArrayOf(
        //顶点坐标           纹理坐标
        -1.0f, 1.0f, 0.0f, 0.0f, 0.0f,  //左上角
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, //左下角
        1.0f, 1.0f, 0.0f, 1.0f, 0.0f,   //右上角
        1.0f, -1.0f, 0.0f, 1.0f, 1.0f   //右下角
    )

    private val color = floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)

    private var mProgram = -1
    private var mPositionHandle = -1
    private var mTexCoordHandle = -1
    private var mMVPMatrixHandle = -1
    private var mSamplerHandle = -1
    private var mDstSamplerHandle = -1
    private var mProgressHandle = -1

    private var mCenterLeftEyeHandle = -1
    private var mCenterRightEyeHandle = -1

    private var mBeginLeftEyeHandle = -1
    private var mEndLeftEyeHandle = -1
    private var mBeginRightEyeHandle = -1
    private var mEndRightEyeHandle = -1

    private var mNoseOneHandle = -1
    private var mNoseTwoHandle = -1
    private var mLeftCheekOneHandle = -1
    private var mLeftCheekTwoHandle = -1
    private var mRightCheekOneHandle = -1
    private var mRightCheekTwoHandle = -1

    private var mImageSizeHandle = -1

    private val mKernel = FloatArray(KERNEL_SIZE)
    private lateinit var mTexOffset: FloatArray
    private var mColorOffset = 0.0f

    private var mTextureId = -1
    private var mDstTextureId = -1

    private var mFaceInfo: FaceInfo? = null
    private var mImageWidth = -1
    private var mImageHeight = -1

    // VBO相关
    private var vboId = IntArray(1)

    private lateinit var coordBuffer: FloatBuffer

    fun setKernel(kenel: FloatArray, colorOffset: Float) {
        System.arraycopy(kenel, 0, mKernel, 0, KERNEL_SIZE)
        mColorOffset = colorOffset
    }

    fun setFaceInfo(faceInfo: FaceInfo){
        mFaceInfo = faceInfo
    }

    fun setTexSize(width: Int, height: Int){
        val tw = 1.0f/width
        val th = 1.0f/height

        mTexOffset = floatArrayOf(-tw, -th, 0f, -th, tw, -th,
                                  -tw, 0f, 0f, 0f, tw, 0f,
                                  -tw, th, 0f, th, tw, th)

    }

    constructor(context: Context, bitmap: Bitmap, dstBitmap: Bitmap) {

        mContext = context
        mImageWidth = bitmap.width
        mImageHeight = bitmap.height

        mTextureId = uploadTexture(bitmap)
        mDstTextureId = uploadTexture(dstBitmap)


        val bb = ByteBuffer.allocateDirect(coordData.size * 4)
        bb.order(ByteOrder.nativeOrder())
        coordBuffer = bb.asFloatBuffer()
        coordBuffer.put(coordData)
        coordBuffer.position(0)

        //创建shader，并为其指定源码
        val vertexShaderSource = ShaderUtil.readFileFromAssets("vertex.glsl",context)
        val fragmentShaderSource = ShaderUtil.readFileFromAssets("fragment_slimmingface.glsl",context)
        mProgram = ShaderUtil.createGLProgram(vertexShaderSource, fragmentShaderSource)

        // 生成VBO
        GLES30.glGenBuffers(1, vboId, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bb.capacity() ,
            bb, GLES30.GL_STATIC_DRAW)

        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "aPosition")
        GLES30.glEnableVertexAttribArray(mPositionHandle)

        mTexCoordHandle = GLES30.glGetAttribLocation(mProgram, "aTexCoord")
        GLES30.glEnableVertexAttribArray(mTexCoordHandle)

        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix")
        mSamplerHandle = GLES30.glGetUniformLocation(mProgram, "uSampler")
        mDstSamplerHandle = GLES30.glGetUniformLocation(mProgram, "uDstSampler")
        mProgressHandle = GLES30.glGetUniformLocation(mProgram, "uProgress")

        mCenterLeftEyeHandle = GLES30.glGetUniformLocation(mProgram, "uCenterLeftEye")
        mCenterRightEyeHandle = GLES30.glGetUniformLocation(mProgram, "uCenterRightEye")

        mBeginLeftEyeHandle = GLES30.glGetUniformLocation(mProgram, "uBeginLeftEye")
        mEndLeftEyeHandle = GLES30.glGetUniformLocation(mProgram, "uEndLeftEye")
        mBeginRightEyeHandle = GLES30.glGetUniformLocation(mProgram, "uBeginRightEye")
        mEndRightEyeHandle = GLES30.glGetUniformLocation(mProgram, "uEndRightEye")

        mNoseOneHandle = GLES30.glGetUniformLocation(mProgram, "uNoseOne")
        mNoseTwoHandle = GLES30.glGetUniformLocation(mProgram, "uNoseTwo")
        mLeftCheekOneHandle = GLES30.glGetUniformLocation(mProgram, "uLeftCheekOne")
        mLeftCheekTwoHandle = GLES30.glGetUniformLocation(mProgram, "uLeftCheekTwo")
        mRightCheekOneHandle = GLES30.glGetUniformLocation(mProgram, "uRightCheekOne")
        mRightCheekTwoHandle = GLES30.glGetUniformLocation(mProgram, "uRightCheekTwo")

        mImageSizeHandle = GLES30.glGetUniformLocation(mProgram, "uImageSize")


        // Unbind the VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun transData(handle: Int, point: PointF){
        val floatArray = floatArrayOf(point.x, point.y)
        val floatBuffer = ByteBuffer.allocateDirect(2*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        floatBuffer.put(floatArray)
        floatBuffer.position(0)

        GLES30.glUniform2fv(handle, 1, floatBuffer)
    }

    fun draw(mvpM: FloatArray) {
        val progress = (mFrameIdx++ % LOOPCOUNT) * 1.0f/LOOPCOUNT

        //使用program
        GLES30.glUseProgram(mProgram)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId[0])

        GLES30.glVertexAttribPointer(mPositionHandle, 3, GLES30.GL_FLOAT, false, 5*Float.SIZE_BYTES, 0)
        GLES30.glVertexAttribPointer(mTexCoordHandle, 2, GLES30.GL_FLOAT, false, 5*Float.SIZE_BYTES, 3*Float.SIZE_BYTES)

        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpM, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)
        GLES30.glUniform1i(mSamplerHandle, 0)

        if(mDstSamplerHandle >=0) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mDstTextureId)
            GLES30.glUniform1i(mDstSamplerHandle, 1)
        }
        GLES30.glUniform1f(mProgressHandle, progress)

        if(mFaceInfo != null) {

            transData(mCenterLeftEyeHandle, mFaceInfo!!.mCenterLeftEye)
            transData(mCenterRightEyeHandle, mFaceInfo!!.mCenterRightEye)
            transData(mBeginLeftEyeHandle, mFaceInfo!!.mBeginLeftEye)
            transData(mEndLeftEyeHandle, mFaceInfo!!.mEndLeftEye)
            transData(mBeginRightEyeHandle, mFaceInfo!!.mBeginRightEye)
            transData(mEndRightEyeHandle, mFaceInfo!!.mEndRightEye)

            transData(mNoseOneHandle, mFaceInfo!!.mNoseOne)
            transData(mNoseTwoHandle, mFaceInfo!!.mNoseTwo)
            transData(mLeftCheekOneHandle, mFaceInfo!!.mLeftCheekOne)
            transData(mLeftCheekTwoHandle, mFaceInfo!!.mLeftCheekTwo)
            transData(mRightCheekOneHandle, mFaceInfo!!.mRightCheekOne)
            transData(mRightCheekTwoHandle, mFaceInfo!!.mRightCheekTwo)

            transData(mImageSizeHandle, PointF(mImageWidth.toFloat(), mImageHeight.toFloat()))
        }

        //drawAarray, 绘制矩型
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        // 解绑VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun release(){
        GLES30.glDeleteProgram(mProgram)

        GLES30.glDeleteBuffers(1, vboId, 0)
        //GLES30.glDeleteVertexArrays(1, vaoId, 0)
    }

    private fun uploadTexture(bitmap: Bitmap): Int {

        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                                GLES30.GL_TEXTURE_MIN_FILTER,
                                GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                                GLES30.GL_TEXTURE_MAG_FILTER,
                                GLES30.GL_LINEAR)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,
                            0,
                           GLES30.GL_RGBA,
                           bitmap,
                     0)
        bitmap.recycle()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return textureIds[0]
    }
}