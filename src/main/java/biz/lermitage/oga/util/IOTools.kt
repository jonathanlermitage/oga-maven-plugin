package biz.lermitage.oga.util

import biz.lermitage.oga.cfg.Definitions
import biz.lermitage.oga.cfg.IgnoreList
import com.google.gson.GsonBuilder
import org.apache.commons.io.FileUtils
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.logging.Log
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceCreationException
import org.codehaus.plexus.resource.loader.ResourceNotFoundException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.pathString

/**
 * IO tools.
 *
 * @author Jonathan Lermitage
 */
object IOTools {

    private val GSON = GsonBuilder().create()

    @Throws(IOException::class)
    fun readDefinitions(location: String, locator: ResourceManager, log: Log): Definitions {
        return readFromLocation(location, locator, log, Definitions::class.java)
    }

    @Throws(IOException::class)
    fun readIgnoreList(location: String, locator: ResourceManager, log: Log): IgnoreList {
        return readFromLocation(location, locator, log, IgnoreList::class.java)
    }

    @Throws(IOException::class)
    private fun <T> readFromLocation(location: String, locator: ResourceManager, log: Log, clazz: Class<T>): T {
        val file = locationToFile(location, locator, log)
        val asString = FileUtils.readFileToString(file, Charsets.UTF_8)
        return GSON.fromJson(asString, clazz)
    }

    @Throws(MojoExecutionException::class)
    private fun locationToFile(location: String, locator: ResourceManager, log: Log): File {
        try {
            val resolvedLocation = Files.createTempFile("oga-", ".json")
            resolvedLocation.toFile().deleteOnExit()
            log.debug("Resolved file from '$location' to '$resolvedLocation'")
            return locator.getResourceAsFile(location, resolvedLocation.pathString)
                ?: throw MojoExecutionException("Could not resolve $location")
        } catch (e: ResourceNotFoundException) {
            throw MojoExecutionException(e.message, e)
        } catch (e: FileResourceCreationException) {
            throw MojoExecutionException(e.message, e)
        }
    }
}
