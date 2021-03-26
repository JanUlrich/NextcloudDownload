package de.who.nextclouddownload

import android.os.Bundle
import android.util.Xml
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.stream.Stream
import kotlin.sequences.generateSequence

data class PropfindEntry(val href: String, val folder: Boolean?)

sealed class XMLElement
class XMLTagStart(val name : String) : XMLElement()
class XMLTagEnd(val name : String) : XMLElement()
class XMLText(val value : String) : XMLElement()


interface XMLParser
{
    fun match(tagName : String) : TagMatcher
}


class TagMatcher(val stream : Sequence<XMLElement>, val tagName : String) : XMLParser {
    override fun match(tagName : String) : TagMatcher{
        return TagMatcher(stream, tagName)
    }

    fun getText() : Sequence<String> =
        stream.filter{}
}

/**
 * propfinds = parser.match("propfind") -> XMLParser
 * hrefs = parser.match("propfind").match("href").getText() --> Sequence<String>
 * hrefs = parser.match("propfind").match("resourceType").getText().map
 * hrefs = propfinds.match("resourcetype").getText()
 * parser.match("propfind").map{
 *      val href = it.match("href").map(parser.getText())
 *      val isFolder it.match(
 * }
 */
class XMLParserInit(val input: Reader) : XMLParser{
    override fun match(tagName : String) : TagMatcher{
        return TagMatcher(this, listOf(tagName))
    }


    private fun toSequence () = generateSequence {
        val i = input.read()
        if(i == -1){
            input.close()
            null
        }else{
            i.toChar()
        }
    }

    private fun takeTill(char : Char): Int{
        var i : Int = input.read()
        while(i != -1 && i.toChar() != char){
            i = input.read()
        }
        if(i == -1)return i
        return input.read()
    }

    private fun nextStartTag() : String? {
        val stream = toSequence()
        stream.dropWhile{ it != '<'}
        stream.drop(1)

        val tagName : StringBuilder = java.lang.StringBuilder();
        stream.takeWhile { it != ' ' }.map { tagName.append(it) }
        return tagName.toString()
    }

    private fun tagParse(tagFilter: String,stream: Reader): String?{;
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
}

class XMLParserMatcher()

/**
    Returns every href in the input xml
 */
fun xmlparse(stream: Reader) : Sequence<String> = generateSequence {
        var i : Int = stream.read()
        while(i != -1){
            val c : Char = i.toChar()
            if(c == '<'){
                val tag = tagParse(stream)
                if(tag != null){
                    tag
                }
            }
            i = stream.read()
        }
        stream.close()
        null
    }

private fun tagParse(tagFilter: String,stream: Reader): String?{;
    val ret : StringBuilder = java.lang.StringBuilder();
    var i : Int = stream.read()
    while(i != -1){
        val c : Char = i.toChar()
        if(c == '>'){
            if(ret.toString().equals("d:href")){
                return dataParse(stream)
            }
        }
        ret.append(c)
        i = stream.read()
    }
    return null;
}

private fun dataParse(stream: Reader): String?{
    val ret : StringBuilder = java.lang.StringBuilder();
    var i : Int = stream.read()
    while(i != -1){
        val c : Char = i.toChar()
        if(c == '<'){
            return xmlEscape(ret.toString())
        }
        ret.append(c)
        i = stream.read()
    }
    return null
}

/*
"   &quot;
'   &apos;
<   &lt;
>   &gt;
&   &amp;
 */
private fun xmlEscape(xml: String): String =
    xml
        .replace("&quot;","\"")
        .replace("&apos;","'")
        .replace("&lt;","<")
        .replace("&quot;",">")
        .replace("&amp;","&")



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