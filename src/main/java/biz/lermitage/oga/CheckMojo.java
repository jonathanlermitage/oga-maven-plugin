package biz.lermitage.oga;

import biz.lermitage.oga.cfg.DefinitionMigration;
import biz.lermitage.oga.cfg.Definitions;
import biz.lermitage.oga.cfg.IgnoreList;
import biz.lermitage.oga.util.DefinitionsTools;
import biz.lermitage.oga.util.IOTools;
import biz.lermitage.oga.util.IgnoreListTools;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Goal which checks no dependency uses deprecated Maven coordinates.
 *
 * @author Jonathan Lermitage
 */
@SuppressWarnings("unused")
@Mojo(name = "check", requiresProject = true, requiresOnline = true, threadSafe = true)
public class CheckMojo extends AbstractMojo {

    private static final String GITHUB_PRJ_RAW_URL = "https://raw.githubusercontent.com/jonathanlermitage/oga-maven-plugin/master/";
    private static final String DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-definitions.json";
    private static final String UNOFFICIAL_DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-unofficial-definitions.json";
    private static final String GITHUB_ISSUES_URL = "github.com/jonathanlermitage/oga-maven-plugin";

    /** Alternative location for og-definitions.json config file. */
    @Parameter(name = "ogDefinitionsUrl", property = "ogDefinitionsUrl")
    private String ogDefinitionsUrl;

    /** Location for additional og-definitions.json config file which are evaluated in addition to the ogDefinitionsUrl file. */
    @Parameter(name = "additionalDefinitionFiles", property = "additionalDefinitionFiles")
    private String[] additionalDefinitionFiles;

    /** Alternative location for og-unofficial-definitions.json config file. */
    @Parameter(name = "ogUnofficialDefinitionsUrl", property = "ogUnofficialDefinitionsUrl")
    private String ogUnofficialDefinitionsUrl;

    /** Ignore unofficial migration rules. */
    @Parameter(name = "ignoreUnofficialMigrations", property = "ignoreUnofficialMigrations")
    private boolean ignoreUnofficialMigrations;

    /** Location ignore list local file. */
    @Parameter(name = "ignoreListFile", property = "ignoreListFile")
    private String ignoreListFile;

    /** Location ignore list remote url. */
    @Parameter(name = "ignoreListUrl", property = "ignoreListUrl")
    private String ignoreListUrl;

    /** Fail on error, otherwise display an error message only. */
    @Parameter(name = "failOnError", property = "failOnError", defaultValue = "true")
    private boolean failOnError;

    /** Skip Check, for use in multi-branch pipeline or command line override. */
    @Parameter(name = "skip", property = "oga.maven.skip")
    private boolean skip;

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Component
    private ResourceManager locator;

    /**
     * Execute goal.
     */
    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Old GroupId Alerter - " + GITHUB_ISSUES_URL);

        if (skip) {
            getLog().info("Skipping Check");
            return;
        }

        setUpLocator(locator);

        try {
            List<Definitions> allDefinitions = new ArrayList<>();
            allDefinitions.add(DefinitionsTools.loadDefinitionsFromUrl(
                ogDefinitionsUrl != null ? ogDefinitionsUrl : DEFINITIONS_URL, getLog(), locator));
            if (!ignoreUnofficialMigrations) {
                allDefinitions.add(DefinitionsTools.loadDefinitionsFromUrl(
                    ogUnofficialDefinitionsUrl != null ? ogUnofficialDefinitionsUrl : UNOFFICIAL_DEFINITIONS_URL, getLog(), locator));
            }

            // Load additional definitions if defined
            if (additionalDefinitionFiles != null) {
                for (String file : additionalDefinitionFiles) {
                    allDefinitions.add(DefinitionsTools.loadDefinitionsFromUrl(file, getLog(), locator));
                }
            }

            Optional<IgnoreList> ignoreList = Optional.empty();
            if (ignoreListFile != null && !ignoreListFile.isEmpty()) {
                getLog().info("Loading ignore list from file " + ignoreListFile);
                // TODO given that we now support loading a file from classpath, file and URL we could consolidate configuration to definitions and suppressions (like PMD/Checkstyle)
                ignoreList = Optional.of(IOTools.readIgnoreList(ignoreListFile, locator, getLog()));
            } else if (ignoreListUrl != null && !ignoreListUrl.isEmpty()) {
                getLog().info("Loading ignore list from url " + ignoreListUrl);
                ignoreList = Optional.of(IOTools.readIgnoreList(ignoreListUrl, locator, getLog()));
            }

            List<Dependency> dependencies = DefinitionsTools.mapDependenciesToOgaDependencies(project.getDependencies());
            List<Dependency> plugins = DefinitionsTools.mapPluginsToOgaDependencies(project.getPluginArtifacts());
            List<Dependency> projectLibs = new ArrayList<>(dependencies);
            projectLibs.addAll(plugins);

            boolean deprecatedDependenciesFound = false;
            getLog().info("Checking dependencies and plugins...");

            // Gather all our migrations together
            List<DefinitionMigration> migrations = new ArrayList<>();
            for (Definitions defs : allDefinitions) {
                if (defs.getMigration() != null) {
                    migrations.addAll(defs.getMigration());
                }
            }

            // compare project dependencies to definitions
            for (DefinitionMigration mig : migrations) {
                if (mig.isGroupIdOnly()) {
                    for (Dependency dep : projectLibs) {
                        if (dep.getGroupId().equals(mig.getOldGroupId())) {
                            if (IgnoreListTools.shouldIgnoreGroupId(ignoreList, dep, mig)) {
                                String msg = "'" + dep.getGroupId() + "' groupId could be replaced by " + mig.proposedMigrationToString() +
                                    " but this migration is excluded by ignore list";
                                getLog().info("(" + dep.getType().getLabel() + ") " + msg);
                            } else {
                                String msg = "'" + dep.getGroupId() + "' groupId should be replaced by " + mig.proposedMigrationToString();
                                if (mig.getContext() != null && !mig.getContext().isEmpty()) {
                                    msg += " (context: " + mig.getContext() + ")";
                                }
                                getLog().error("(" + dep.getType().getLabel() + ") " + msg);
                                deprecatedDependenciesFound = true;
                            }
                        }
                    }
                } else {
                    for (Dependency dep : projectLibs) {
                        if (dep.getGroupId().equals(mig.getOldGroupId()) && dep.getArtifactId().equals(mig.getOldArtifactId())) {
                            if (IgnoreListTools.shouldIgnoreArtifactId(ignoreList, dep, mig)) {
                                String msg = "'" + dep.getGroupId() + ":" + dep.getArtifactId() + "' could be replaced by " + mig.proposedMigrationToString() +
                                    " but this migration is excluded by ignore list";
                                getLog().info("(" + dep.getType().getLabel() + ") " + msg);
                            } else {
                                String msg = "'" + dep.getGroupId() + ":" + dep.getArtifactId() + "' should be replaced by " + mig.proposedMigrationToString();
                                if (mig.getContext() != null && !mig.getContext().isEmpty()) {
                                    msg += " (context: " + mig.getContext() + ")";
                                }
                                getLog().error("(" + dep.getType().getLabel() + ") " + msg);
                                deprecatedDependenciesFound = true;
                            }
                        }
                    }
                }
            }

            if (deprecatedDependenciesFound) {
                String errMsg = "Project has old dependencies; see warning/error messages";
                if (failOnError) {
                    throw new MojoExecutionException(errMsg);
                } else {
                    getLog().error(errMsg);
                }
            } else {
                getLog().info("No problem detected. Good job! :-)");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Plugin failure, please report it to " + GITHUB_ISSUES_URL, e);
        }
    }

    private void setUpLocator(ResourceManager locator) {
        List<String> searchPaths = Arrays.asList(
            // 0. The locator will read from classpath and URL locations
            // 1. in the directory of the current project's pom file - note: extensions might replace the pom file on the fly
            project.getFile().getParentFile().getAbsolutePath(),
            // 2. in the current project's directory
            project.getBasedir().getAbsolutePath(),
            // 3. in the base directory - that's the directory of the initial pom requested to build, e.g. the root of a multi-module build
            session.getRequest().getBaseDirectory()
        );
        for (String searchPath : searchPaths) {
            locator.addSearchPath(FileResourceLoader.ID, searchPath);
        }
    }
}
