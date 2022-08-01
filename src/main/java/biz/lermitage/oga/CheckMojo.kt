package biz.lermitage.oga

import biz.lermitage.oga.cfg.DefinitionMigration
import biz.lermitage.oga.cfg.Definitions
import biz.lermitage.oga.cfg.IgnoreList
import biz.lermitage.oga.util.IOTools
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
            val allDefinitions = mutableListOf(loadDefinitionsFromUrl(ogDefinitionsUrl ?: DEFINITIONS_URL))
            if (!ignoreUnofficialMigrations) {
                allDefinitions += loadDefinitionsFromUrl(ogUnofficialDefinitionsUrl ?: UNOFFICIAL_DEFINITIONS_URL)
            }

            // Load additional definitions if defined
            additionalDefinitionFiles!!.forEach { allDefinitions += loadDefinitionsFromUrl(it) }

            var ignoreList = Optional.empty<IgnoreList>()
            if (!ignoreListFile.isNullOrEmpty()) {
                log.info("Loading ignore list from file $ignoreListFile")
                ignoreList = Optional.of(IOTools.readIgnoreList(File(ignoreListFile)))
            } else if (!ignoreListUrl.isNullOrEmpty()) {
                log.info("Loading ignore list from url $ignoreListUrl")
                ignoreList = Optional.of(IOTools.readIgnoreList(URL(ignoreListUrl)))
            }

            val dependencies = project?.dependencies!!.filterNotNull().map { dependency ->
                Dependency(
                    dependency.groupId,
                    dependency.artifactId,
                    DependencyType.DEPENDENCY
                )
            }
            val plugins = project?.pluginArtifacts!!.filterNotNull().map { dependency ->
                Dependency(
                    dependency.groupId,
                    dependency.artifactId,
                    DependencyType.PLUGIN
                )
            }
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

                            if (shouldIgnoreGroupId(ignoreList, dep, mig)) {
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

                            if (shouldIgnoreArtifactId(ignoreList, dep, mig)) {
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
                if (failOnError) {
                    throw MojoExecutionException("Project has old dependencies; see warning/error messages")
                } else {
                    log.error("Project has old dependencies; see warning/error messages")
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

    private fun loadDefinitionsFromUrl(url: String): Definitions {
        log.info("Loading definitions from $url")
        val definitions = IOTools.readDefinitions(url)

        val nbDefinitions = definitions.migration?.size
        var welcomeMsg = "Loaded $nbDefinitions definitions from '$url'"
        if (definitions.date != null) {
            welcomeMsg += ", updated on ${definitions.date}"
        }
        log.info(welcomeMsg)

        return definitions
    }

    private fun shouldIgnoreGroupId(
        ignoreList: Optional<IgnoreList>,
        oldDep: Dependency,
        newDep: DefinitionMigration
    ): Boolean {
        if (ignoreList.isPresent) {
            if (newDep.state == DependencyState.MIGRATED) {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    ignoreItem.isGroupIdOnly && (ignoreItem.groupId == oldDep.groupId || ignoreItem.groupId == newDep.newerGroupId)
                } == true
            } else {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    ignoreItem.isGroupIdOnly && (ignoreItem.groupId == oldDep.groupId /*|| newDep.unofficialGroupIdCandidates.contains(ignoreItem.groupId)*/) // TODO apply ignoreList to candidates
                } == true
            }
        }
        return false
    }

    private fun shouldIgnoreArtifactId(
        ignoreList: Optional<IgnoreList>,
        oldDep: Dependency,
        newDep: DefinitionMigration
    ): Boolean {
        if (ignoreList.isPresent) {
            if (newDep.state == DependencyState.MIGRATED) {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    if (ignoreItem.isGroupIdOnly) {
                        ignoreItem.groupId == oldDep.groupId || ignoreItem.groupId == newDep.newerGroupId
                    } else {
                        (ignoreItem.groupId == oldDep.groupId && ignoreItem.artifactId == oldDep.artifactId
                            || ignoreItem.groupId == newDep.newerGroupId && ignoreItem.artifactId == newDep.newerArtifactId)
                    }
                } == true
            } else {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    if (ignoreItem.isGroupIdOnly) {
                        ignoreItem.groupId == oldDep.groupId || newDep.unofficialGroupIdCandidates.contains(ignoreItem.groupId)
                    } else {
                        (ignoreItem.groupId == oldDep.groupId && ignoreItem.artifactId == oldDep.artifactId
                            /*|| newDep.unofficialGroupIdArtifactIdCandidates.contains(ignoreItem.item)*/) // TODO apply ignoreList to candidates
                    }
                } == true
            }
        }
        return false
    }

    /*private fun isDeprecatedOnMavencentral(groupId: String, artifactId: String): Boolean {
        return try {
            val url = "https://mvnrepository.com/artifact/$groupId/$artifactId"
            log.info("Checking $url")
            Thread.sleep(500) // calling many urls aggressively may lead to IP blacklisting
            val doc = Jsoup.connect(url).execute().parse()
            doc.text().contains("Note: This artifact was moved to:")
        } catch (e: Exception) {
            log.debug(e)
            false
        }
    }*/

    companion object {
        private const val GITHUB_PRJ_RAW_URL = "https://raw.githubusercontent.com/jonathanlermitage/oga-maven-plugin/master/"
        private const val DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-definitions.json"
        private const val UNOFFICIAL_DEFINITIONS_URL = GITHUB_PRJ_RAW_URL + "uc/og-unofficial-definitions.json"
        private const val GITHUB_ISSUES_URL = "github.com/jonathanlermitage/oga-maven-plugin"
    }
}
