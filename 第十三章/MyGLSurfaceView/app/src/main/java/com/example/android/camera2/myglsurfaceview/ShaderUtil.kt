package com.example.android.camera2.myglsurfaceview

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.lang.Exception
import java.lang.StringBuilder

class ShaderUtil {
    companion object {
        fun readFileFromAssets(fileName: String?, context: Context): String? {
            val result = StringBuilder()
            try {
                val myIs = context.assets.open(fileName!!)
                val buffer = ByteArray(1024)
                var count = 0
                while(myIs.read(buffer).also { count = it } != -1){
                    result.append(String(buffer, 0, count))
                }
                myIs.close()
            }catch(e: Exception){
                e.printStackTrace()
            }
            return result.toString()
        }

        fun loadShader(shaderType: Int, source: String): Int {
            var shader = GLES30.glCreateShader(shaderType)
            if(shader != 0){
                GLES30.glShaderSource(shader, source)
                GLES30.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
                if(compiled[0] == 0) {
                    Log.e("ShaderUtil", GLES30.glGetShaderInfoLog(shader))
                    GLES30.glDeleteShader(shader)
                    shader = 0
                }
            }

            return shader
        }

        fun createGLProgram(vertexShader: String?, fragmentShader: String?) : Int {
            if(vertexShader == null || fragmentShader == null){
                return 0
            }

            val vShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShader)
            if(vShader == 0){
                return 0
            }

            val fShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader)
            if(fShader == 0) {
                return 0
            }

            var program = GLES30.glCreateProgram()
            if(program !=0 ){
                GLES30.glAttachShader(program, vShader)
                GLES30.glAttachShader(program, fShader)

                GLES30.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
                if(linkStatus[0] != GLES30.GL_TRUE){
                    Log.e("ShaderUtil", GLES30.glGetProgramInfoLog(program))
                    GLES30.glDeleteProgram(program)
                    program = 0
                }
            }
            return program
        }
    }
}