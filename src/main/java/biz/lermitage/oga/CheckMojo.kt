package biz.lermitage.oga

import biz.lermitage.oga.cfg.DefinitionMigration
import biz.lermitage.oga.cfg.IgnoreList
import biz.lermitage.oga.util.DefinitionsTools
import biz.lermitage.oga.util.IOTools
import biz.lermitage.oga.util.IgnoreListTools
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import java.io.File
import java.io.IOException
import java.net.URL
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

        try {
            val allDefinitions = mutableListOf(DefinitionsTools.loadDefinitionsFromUrl(ogDefinitionsUrl ?: DEFINITIONS_URL, log))
            if (!ignoreUnofficialMigrations) {
                allDefinitions += DefinitionsTools.loadDefinitionsFromUrl(ogUnofficialDefinitionsUrl ?: UNOFFICIAL_DEFINITIONS_URL, log)
            }

            // Load additional definitions if defined
            additionalDefinitionFiles!!.forEach { allDefinitions += DefinitionsTools.loadDefinitionsFromUrl(it, log) }

            var ignoreList = Optional.empty<IgnoreList>()
            if (!ignoreListFile.isNullOrEmpty()) {
                log.info("Loading ignore list from file $ignoreListFile")
                ignoreList = Optional.of(IOTools.readIgnoreList(File(ignoreListFile)))
            } else if (!ignoreListUrl.isNullOrEmpty()) {
                log.info("Loading ignore list from url $ignoreListUrl")
                ignoreList = Optional.of(IOTools.readIgnoreList(URL(ignoreListUrl)))
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
                                    "'${dep.groupId}' groupId could be replaced by '${mig.proposedMigrationToString()}' " +
                                        "but it's excluded by ignore list"
                                log.info("(${dep.type.label}) $msg")
                            } else {
                                var msg = "'${dep.groupId}' groupId should be replaced by '${mig.proposedMigrationToString()}'"
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
                                    "'${dep.groupId}:${dep.artifactId}' could be replaced by '${mig.proposedMigrationToString()}' " +
                                        "but it's excluded by ignore list"
                                log.info("(${dep.type.label}) $msg")
                            } else {
                                var msg =
                                    "'${dep.groupId}:${dep.artifactId}' should be replaced by '${mig.proposedMigrationToString()}'"
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

    companion object {
        private const val GITHUB_PRJ_RAW_URL = "https://raw.githubusercontent.com/jonathanlermitage/oga-maven-plugin/master/"
        private const val DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-definitions.json"
        private const val UNOFFICIAL_DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-unofficial-definitions.json"
        private const val GITHUB_ISSUES_URL = "github.com/jonathanlermitage/oga-maven-plugin"
    }
}
