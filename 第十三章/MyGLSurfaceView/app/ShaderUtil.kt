import android.content.Context

class ShaderUtil {
    companion object {
        fun readFileFromAssets(fileName: String?, context: Context) : String ?{
            val result: StringBuilder()
            val myIs = context.assets.open(fileName!!)
            val buffer = ByteArray(2048)
            var count = 0
            while(myIs.read(buffer).also{ count = it} != -1){

            }

        }
    }
}