package de.who.nextclouddownload

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.stream.Stream

data class XMLElement(val tagName: String, val data : Stream<XMLElement>)

fun xmlparse(stream: Reader){
    var i : Int = stream.read()
    while(i != -1){
        val c : Char = i.toChar()
        if(c == '<'){
            tagParse(stream)
        }

        i = stream.read()
    }
    stream.close()
}

private fun tagParse(stream: Reader): String?{;
    val ret : StringBuilder = java.lang.StringBuilder();
    var i : Int = stream.read()
    while(i != -1){
        val c : Char = i.toChar()
        if(c == '>'){
            return ret.toString()
        }
        ret.append(c)
        i = stream.read()
    }
    return null;
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        run("/remote.php/dav/files/janulrich/Music/")
    }

    val credential: String = Credentials.basic("janulrich", "i17uH8chbGxu")
    private val client = OkHttpClient.Builder().eventListener(PrintingEventListener())
        .build();

    fun run(folder: String) {
        // body
        val body = """<d:propfind xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/">
<d:prop>
 <d:displayname />
 <d:getetag />
</d:prop>
</d:propfind>"""
        val request: Request = Request.Builder()
                .url("https://homeserv.balja.org$folder")
                .method("PROPFIND", body.toRequestBody())
                .header("DEPTH", "1")
                .header("Authorization", credential)
                .header("Content-Type", "text/xml")
                .build()

        val downloadsDir = this.getExternalFilesDir(null)

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                println("Test")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val propString = response.body!!.string()
                    val stream = response.body!!.charStream()


                    val regexFiles = """<d:href>([^<]+[^/])</d:href>""".toRegex()
                    var matchResult = regexFiles.find(propString)


                    while (matchResult != null) {
                        val hrefResult = matchResult!!.destructured.component1()
                        println(hrefResult)

                        val toSave = File(downloadsDir, URLDecoder.decode(hrefResult))
                        if (!toSave.getParentFile().exists())
                            toSave.getParentFile().mkdirs();
                        if (!toSave.exists())
                            toSave.createNewFile();
                        //TODO Hier keine Retrys sondern komplett neu versuchen?
                        // bzw. nach einer bestimmten Anzahl retrys
                        while(!saveToFile("https://homeserv.balja.org$hrefResult", toSave)){
                            println("RETRY")
                        }

                        matchResult = matchResult.next()
                    }

                    val regexFolders = """<d:href>([^<]+/)</d:href>""".toRegex()
                    matchResult = regexFolders.find(propString)
                    while (matchResult != null) {
                        val hrefResult = matchResult!!.destructured.component1()
                        println("Downloading folder $hrefResult")
                        run(hrefResult)

                        matchResult = matchResult.next()

                    }
                }
            }
        })
    }

    fun saveToFile(url: String, file: File): Boolean {
        println("calling $url")
        val request = Request.Builder()
                .header("Authorization", credential)
                .url(url)
                .build()
        client.newCall(request).execute().use { response ->
            try{
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val initialStream: InputStream = response.body!!.byteStream()
                Files.copy(
                    initialStream,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e : IOException){
                return false;
            }finally {
                response.close()
            }
            return true;
        }
    }

}