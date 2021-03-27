package biz.lermitage.oga.util

import biz.lermitage.oga.cfg.Definitions
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * IO tools.
 *
 * @author Jonathan Lermitage
 */
object IOTools {

    private val GSON = GsonBuilder().create()

    @Throws(IOException::class)
    fun readDefinitionsFromUrl(url: URL): Definitions {
        val definitionsAsString = readContentFromUrl(url)
        return GSON.fromJson(definitionsAsString, Definitions::class.java)
    }

    @Throws(IOException::class)
    fun readContentFromUrl(url: URL): String {
        val content = StringBuilder()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        BufferedReader(InputStreamReader(conn.inputStream)).use { rd ->
            var line: String? = rd.readLine()
            while (line != null) {
                content.append(line)
                line = rd.readLine()
            }
        }
        return content.toString()
    }
}
