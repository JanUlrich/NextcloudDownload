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

class PropfindXmlParser {

    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): Sequence<*> {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readMultistatus(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readMultistatus(parser: XmlPullParser): Sequence<PropfindEntry> = generateSequence {
        parser.require(XmlPullParser.START_TAG, ns, "d:multistatus")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.name == "d:prop") {
                parser.
            } else {
                skip(parser)
            }
        }
        null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readResponse(parser: XmlPullParser): PropfindEntry? {
        parser.require(XmlPullParser.START_TAG, ns, "d:response")
        var href: String? = null
        var isFolder: Boolean? = null
        var link: String? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "d:href" -> href = readHref(parser)
                "d:resourcetype" -> isFolder = readIsFolder(parser)
                else -> skip(parser)
            }
        }
        if(href == null)null
        return PropfindEntry(href!!, isFolder)
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readIsFolder(parser: XmlPullParser): Boolean {
        if(parser.isEmptyElementTag || !parser.name.equals("d:resourcetype")) return false
        parser.next()
        if(!parser.name.equals("d:collection")) return false
        parser.next()
        parser.require(XmlPullParser.END_TAG, ns, "d:resourcetype")
        return true
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readHref(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "d:href")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "d:href")
        return title
    }

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

}

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