package biz.lermitage.oga.util;

import biz.lermitage.oga.cfg.Definitions;
import biz.lermitage.oga.cfg.IgnoreList;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * IO tools.
 *
 * @author Jonathan Lermitage
 */
public class IOTools {

    private static final Gson GSON = new GsonBuilder().create();

    private IOTools() {
    }

    public static Definitions readDefinitions(String location, ResourceManager locator, Log log) throws IOException, MojoExecutionException {
        return readFromLocation(location, locator, log, Definitions.class);
    }

    public static IgnoreList readIgnoreList(String location, ResourceManager locator, Log log) throws IOException, MojoExecutionException {
        return readFromLocation(location, locator, log, IgnoreList.class);
    }

    private static <T> T readFromLocation(String location, ResourceManager locator, Log log, Class<T> clazz) throws IOException, MojoExecutionException {
        File file = locationToFile(location, locator, log);
        String asString = FileUtils.readFileToString(file, "UTF-8");
        return GSON.fromJson(asString, clazz);
    }

    private static File locationToFile(String location, ResourceManager locator, Log log) throws MojoExecutionException, IOException {
        try {
            java.nio.file.Path resolvedLocation = Files.createTempFile("oga-", ".json");
            resolvedLocation.toFile().deleteOnExit();
            log.debug("Resolved file from '" + location + "' to '" + resolvedLocation + "'");
            File result = locator.getResourceAsFile(location, resolvedLocation.toString());
            if (result == null) {
                throw new MojoExecutionException("Could not resolve " + location);
            }
            return result;
        } catch (ResourceNotFoundException | FileResourceCreationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
