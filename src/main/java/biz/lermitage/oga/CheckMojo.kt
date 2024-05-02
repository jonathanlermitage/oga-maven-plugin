package biz.lermitage.oga

import biz.lermitage.oga.cfg.DefinitionMigration
import biz.lermitage.oga.cfg.IgnoreList
import biz.lermitage.oga.util.DefinitionsTools
import biz.lermitage.oga.util.IOTools
import biz.lermitage.oga.util.IgnoreListTools
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.resource.ResourceManager
import org.codehaus.plexus.resource.loader.FileResourceLoader
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import java.io.IOException
import java.util.Optional

/**
 * Goal which checks no dependency uses deprecated Maven coordinates.
 *
 * @author Jonathan Lermitage
 */
@Suppress("unused")
@Mojo(name = "check", requiresProject = true, requiresOnline = true, threadSafe = true)
class CheckMojo : AbstractMojo() {

    /** Alternative location for og-definitions.json config file. */
    @Parameter(name = "ogDefinitionsUrl", property = "ogDefinitionsUrl")
    private val ogDefinitionsUrl: String? = null

    /** Location for additional og-definitions.json config file which are evaluated in addition to the ogDefinitionsUrl file. */
    @Parameter(name = "additionalDefinitionFiles", property = "additionalDefinitionFiles")
    private val additionalDefinitionFiles: Array<String>? = null

    /** Alternative location for og-unofficial-definitions.json config file. */
    @Parameter(name = "ogUnofficialDefinitionsUrl", property = "ogUnofficialDefinitionsUrl")
    private val ogUnofficialDefinitionsUrl: String? = null

    /** Ignore unofficial migration rules. */
    @Parameter(name = "ignoreUnofficialMigrations", property = "ignoreUnofficialMigrations")
    private val ignoreUnofficialMigrations: Boolean = false

    /** Location ignore list local file. */
    @Parameter(name = "ignoreListFile", property = "ignoreListFile")
    private val ignoreListFile: String? = null

    /** Location ignore list remote url. */
    @Parameter(name = "ignoreListUrl", property = "ignoreListUrl")
    private val ignoreListUrl: String? = null

    /** Fail on error, otherwise display an error message only. */
    @Parameter(name = "failOnError", property = "failOnError")
    private val failOnError: Boolean = true

    /** Skip Check, for use in multi-branch pipeline or command line override. */
    @Parameter(name = "skip", property = "oga.maven.skip")
    private val skip: Boolean = false

    @Parameter(property = "project", readonly = true)
    var project: MavenProject? = null

    @Parameter(defaultValue = "\${session}", required = true, readonly = true)
    private var session: MavenSession? = null

    @Component
    private val locator: ResourceManager? = null

    /**
     * Execute goal.
     */
    @Throws(MojoExecutionException::class)
    override fun execute() {
        log.info("Old GroupId Alerter - $GITHUB_ISSUES_URL")

        if (skip) {
            log.info("Skipping Check")
            return
        }

        setUpLocator(locator!!)

        try {
            val allDefinitions = mutableListOf(DefinitionsTools.loadDefinitionsFromUrl(ogDefinitionsUrl ?: DEFINITIONS_URL, log, locator))
            if (!ignoreUnofficialMigrations) {
                allDefinitions += DefinitionsTools.loadDefinitionsFromUrl(ogUnofficialDefinitionsUrl ?: UNOFFICIAL_DEFINITIONS_URL, log, locator)
            }

            // Load additional definitions if defined
            additionalDefinitionFiles!!.forEach { allDefinitions += DefinitionsTools.loadDefinitionsFromUrl(it, log, locator) }

            var ignoreList = Optional.empty<IgnoreList>()
            if (!ignoreListFile.isNullOrEmpty()) {
                log.info("Loading ignore list from file $ignoreListFile")
                // TODO given that we now support loading a file from classpath, file and URL we could consolidate configuration to definitions and suppressions (like PMD/Checkstyle)
                ignoreList = Optional.of(IOTools.readIgnoreList(ignoreListFile, locator, log))
            } else if (!ignoreListUrl.isNullOrEmpty()) {
                log.info("Loading ignore list from url $ignoreListUrl")
                ignoreList = Optional.of(IOTools.readIgnoreList(ignoreListUrl, locator, log))
            }

            val dependencies = DefinitionsTools.mapDependenciesToOgaDependencies(project!!.dependencies!!)
            val plugins = DefinitionsTools.mapPluginsToOgaDependencies(project!!.pluginArtifacts!!)
            val projectLibs = dependencies.plus(plugins)

            var deprecatedDependenciesFound = false
            log.info("Checking dependencies and plugins...")

            // Gather all our migrations together
            val migrations = mutableListOf<DefinitionMigration>()
            allDefinitions.forEach { migrations.addAll(it.migration!!) }

            // compare project dependencies to definitions
            migrations.forEach { mig ->
                if (mig.isGroupIdOnly) {
                    projectLibs.forEach { dep ->
                        if (dep.groupId == mig.oldGroupId) {

                            if (IgnoreListTools.shouldIgnoreGroupId(ignoreList, dep, mig)) {
                                val msg =
                                    "'${dep.groupId}' groupId could be replaced by ${mig.proposedMigrationToString()} " +
                                        "but this migration is excluded by ignore list"
                                log.info("(${dep.type.label}) $msg")
                            } else {
                                var msg = "'${dep.groupId}' groupId should be replaced by ${mig.proposedMigrationToString()}"
                                if (!mig.context.isNullOrEmpty()) {
                                    msg += " (context: ${mig.context})"
                                }
                                log.error("(${dep.type.label}) $msg")
                                deprecatedDependenciesFound = true
                            }
                        }
                    }
                } else {
                    projectLibs.forEach { dep ->
                        if (dep.groupId == mig.oldGroupId && dep.artifactId == mig.oldArtifactId) {

                            if (IgnoreListTools.shouldIgnoreArtifactId(ignoreList, dep, mig)) {
                                val msg =
                                    "'${dep.groupId}:${dep.artifactId}' could be replaced by ${mig.proposedMigrationToString()} " +
                                        "but this migration is excluded by ignore list"
                                log.info("(${dep.type.label}) $msg")
                            } else {
                                var msg =
                                    "'${dep.groupId}:${dep.artifactId}' should be replaced by ${mig.proposedMigrationToString()}"
                                if (!mig.context.isNullOrEmpty()) {
                                    msg += " (context: ${mig.context})"
                                }
                                log.error("(${dep.type.label}) $msg")
                                deprecatedDependenciesFound = true
                            }
                        }
                    }
                }
            }

            if (deprecatedDependenciesFound) {
                val errMsg = "Project has old dependencies; see warning/error messages"
                if (failOnError) {
                    throw MojoExecutionException(errMsg)
                } else {
                    log.error(errMsg)
                }
            } else {
                log.info("No problem detected. Good job! :-)")
            }
        } catch (e: IOException) {
            throw MojoExecutionException("Plugin failure, please report it to $GITHUB_ISSUES_URL", e)
        } catch (e: XmlPullParserException) {
            throw MojoExecutionException("Plugin failure, please report it to $GITHUB_ISSUES_URL", e)
        }
    }

    private fun setUpLocator(locator: ResourceManager) {
        val searchPaths = listOf(
            // 0. The locator will read from classpath and URL locations
            // 1. in the directory of the current project's pom file - note: extensions might replace the pom file on the fly
            project!!.file.parentFile.absolutePath,
            // 2. in the current project's directory
            project!!.basedir.absolutePath,
            // 3. in the base directory - that's the directory of the initial pom requested to build, e.g. the root of a multi-module build
            session!!.request!!.baseDirectory
        )
        searchPaths.forEach { searchPath -> locator.addSearchPath(FileResourceLoader.ID, searchPath) }
    }

    companion object {
        private const val GITHUB_PRJ_RAW_URL = "https://raw.githubusercontent.com/jonathanlermitage/oga-maven-plugin/master/"
        private const val DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-definitions.json"
        private const val UNOFFICIAL_DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-unofficial-definitions.json"
        private const val GITHUB_ISSUES_URL = "github.com/jonathanlermitage/oga-maven-plugin"
    }
}
