package biz.lermitage.oga.util

import biz.lermitage.oga.cfg.Definitions
import biz.lermitage.oga.cfg.IgnoreList
import com.google.gson.GsonBuilder
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
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
    fun readDefinitions(url: URL): Definitions {
        val definitionsAsString = readContent(url)
        return GSON.fromJson(definitionsAsString, Definitions::class.java)
    }

    @Throws(IOException::class)
    fun readIgnoreList(url: URL): IgnoreList {
        val ignoreListAsString = readContent(url)
        return GSON.fromJson(ignoreListAsString, IgnoreList::class.java)
    }

    @Throws(IOException::class)
    fun readIgnoreList(file: File): IgnoreList {
        val ignoreListAsString = FileUtils.readFileToString(file)
        return GSON.fromJson(ignoreListAsString, IgnoreList::class.java)
    }

    @Throws(IOException::class)
    fun readContent(url: URL): String {
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
