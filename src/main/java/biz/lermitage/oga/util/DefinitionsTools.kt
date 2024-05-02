package biz.lermitage.oga.util

import biz.lermitage.oga.Dependency
import biz.lermitage.oga.DependencyType
import biz.lermitage.oga.cfg.Definitions
import org.apache.maven.plugin.logging.Log
import org.codehaus.plexus.resource.ResourceManager

/**
 * Definitions tools.
 *
 * @author Jonathan Lermitage
 */
object DefinitionsTools {

    fun loadDefinitionsFromUrl(url: String, log: Log, locator: ResourceManager): Definitions {
        log.info("Loading definitions from $url")
        val definitions = IOTools.readDefinitions(url, locator, log)

        val nbDefinitions = definitions.migration?.size
        var welcomeMsg = "Loaded $nbDefinitions definitions from '$url'"
        if (definitions.date != null) {
            welcomeMsg += ", updated on ${definitions.date}"
        }
        log.info(welcomeMsg)

        return definitions
    }

    fun mapDependenciesToOgaDependencies(dependencies: List<org.apache.maven.model.Dependency>): List<Dependency> {
        return dependencies.map { dependency ->
            Dependency(
                dependency.groupId,
                dependency.artifactId,
                DependencyType.DEPENDENCY
            )
        }
    }

    fun mapPluginsToOgaDependencies(pluginArtifacts: Set<org.apache.maven.artifact.Artifact>): List<Dependency> {
        return pluginArtifacts.toList().map { dependency ->
            Dependency(
                dependency.groupId,
                dependency.artifactId,
                DependencyType.PLUGIN
            )
        }
    }
}
