package biz.lermitage.oga.util;

import biz.lermitage.oga.Dependency;
import biz.lermitage.oga.DependencyType;
import biz.lermitage.oga.cfg.Definitions;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.resource.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Definitions tools.
 *
 * @author Jonathan Lermitage
 */
public class DefinitionsTools {

    private DefinitionsTools() {
    }

    public static Definitions loadDefinitionsFromUrl(String url, Log log, ResourceManager locator) throws Exception {
        log.info("Loading definitions from " + url);
        Definitions definitions = IOTools.readDefinitions(url, locator, log);

        Integer nbDefinitions = definitions.getMigration() != null ? definitions.getMigration().size() : null;
        String welcomeMsg = "Loaded " + nbDefinitions + " definitions from '" + url + "'";
        if (definitions.getDate() != null) {
            welcomeMsg += ", updated on " + definitions.getDate();
        }
        log.info(welcomeMsg);

        return definitions;
    }

    public static List<Dependency> mapDependenciesToOgaDependencies(List<org.apache.maven.model.Dependency> dependencies) {
        List<Dependency> result = new ArrayList<>();
        for (org.apache.maven.model.Dependency dependency : dependencies) {
            result.add(new Dependency(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                DependencyType.DEPENDENCY
            ));
        }
        return result;
    }

    public static List<Dependency> mapPluginsToOgaDependencies(Set<org.apache.maven.artifact.Artifact> pluginArtifacts) {
        List<Dependency> result = new ArrayList<>();
        for (org.apache.maven.artifact.Artifact artifact : pluginArtifacts) {
            result.add(new Dependency(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                DependencyType.PLUGIN
            ));
        }
        return result;
    }
}
