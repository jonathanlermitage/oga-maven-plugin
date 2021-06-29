package biz.lermitage.oga

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
import java.util.*

/**
 * Goal which checks that no dependency uses a deprecated groupId.
 *
 * @author Jonathan Lermitage
 */
@Suppress("unused")
@Mojo(name = "check", requiresProject = true, requiresOnline = true, threadSafe = true)
class CheckMojo : AbstractMojo() {

    /** Alternative location for og-definitions.json config file. */
    @Parameter(name = "ogDefinitionsUrl", property = "ogDefinitionsUrl")
    private val ogDefinitionsUrl: String? = null

    /** Location ignore list local file. */
    @Parameter(name = "ignoreListFile", property = "ignoreListFile")
    private val ignoreListFile: String? = null

    /** Location ignore list remote url. */
    @Parameter(name = "ignoreListUrl", property = "ignoreListUrl")
    private val ignoreListUrl: String? = null

    /** Fail on error, otherwise display an error message only. */
    @Parameter(name = "failOnError", property = "failOnError")
    private val failOnError: Boolean = true

    @Parameter(property = "project", readonly = true)
    var project: MavenProject? = null

    /**
     * Execute goal.
     */
    @Throws(MojoExecutionException::class)
    override fun execute() {
        log.info("Old GroupId Alerter - $GITHUB_ISSUES_URL")

        try {
            val definitionsUrlInUse = ogDefinitionsUrl ?: DEFINITIONS_URL
            log.info("Loading definitions from $definitionsUrlInUse")
            val definitions = URL(definitionsUrlInUse).let { IOTools.readDefinitions(it) }

            var ignoreList = Optional.empty<IgnoreList>()
            if (!ignoreListFile.isNullOrEmpty()) {
                log.info("Loading ignore list from file $ignoreListFile")
                ignoreList = Optional.of(IOTools.readIgnoreList(File(ignoreListFile)))
            } else if (!ignoreListUrl.isNullOrEmpty()) {
                log.info("Loading ignore list from url $ignoreListUrl")
                ignoreList = Optional.of(IOTools.readIgnoreList(URL(ignoreListUrl)))
            }

            val nbDefinitions = definitions.migration?.size
            var welcomeMsg = "Loaded $nbDefinitions definitions"
            if (definitions.date != null) {
                welcomeMsg += ", updated on ${definitions.date}"
            }
            log.info(welcomeMsg)

            val dependencies = project?.dependencies!!.filterNotNull()
            val deprecatedDependencies: HashSet<String> = HashSet()
            log.info("Checking dependencies...")

            // compare project dependencies to integrated black-list
            definitions.migration!!.forEach { mig ->
                if (mig.isGroupIdOnly) {
                    dependencies.forEach { dep ->

                        if (dep.groupId == mig.oldGroupId) {

                            if (shouldIgnoreGroupId(ignoreList, dep.groupId, mig.newerGroupId)) {
                                val msg =
                                    "'${dep.groupId}' groupId could be replaced by '${mig.newerGroupId}' " +
                                        "but it's excluded by ignore list"
                                log.info(msg)
                            } else {
                                var msg = "'${dep.groupId}' groupId should be replaced by '${mig.newerGroupId}'"
                                if (mig.context != null && mig.context.isNotEmpty()) {
                                    msg += " (context: ${mig.context})"
                                }
                                log.error(msg)
                                deprecatedDependencies.add("${dep.groupId}:${dep.artifactId}")
                            }
                        }
                    }
                } else {
                    dependencies.forEach { dep ->
                        if (dep.groupId == mig.oldGroupId && dep.artifactId == mig.oldArtifactId) {

                            if (shouldIgnoreArtifactId(
                                    ignoreList,
                                    dep.groupId,
                                    dep.artifactId,
                                    mig.newerGroupId,
                                    mig.newerArtifactId
                                )
                            ) {
                                val msg =
                                    "'${dep.groupId}:${dep.artifactId}' could be replaced by '${mig.newerGroupId}:${mig.newerArtifactId}' " +
                                        "but it's excluded by ignore list"
                                log.info(msg)
                            } else {
                                var msg =
                                    "'${dep.groupId}:${dep.artifactId}' should be replaced by '${mig.newerGroupId}:${mig.newerArtifactId}'"
                                if (mig.context != null && mig.context.isNotEmpty()) {
                                    msg += " (context: ${mig.context})"
                                }
                                log.error(msg)
                                deprecatedDependencies.add("${dep.groupId}:${dep.artifactId}")
                            }
                        }
                    }
                }
            }

            //// code commented: mvnrepository.com bans IP too quickly (10 req every 500ms)
            // for every safe dependency, look for deprecation on mvnrepository.com
            /*dependencies
                .filter { dep -> !deprecatedDependencies.contains("${dep.groupId}:${dep.artifactId}") }
                .forEach { dep ->
                    if (isDeprecatedOnMavencentral(dep.groupId, dep.artifactId)) {
                        val msg = "'${dep.groupId}:${dep.artifactId}' has a migration notice " +
                            "on https://mvnrepository.com/artifact/${dep.groupId}/${dep.artifactId}"
                        log.error(msg)
                        deprecatedDependencies.add("${dep.groupId}:${dep.artifactId}")
                    }
                }*/
            if (deprecatedDependencies.isNotEmpty()) {
                if (failOnError) {
                    throw MojoExecutionException("Project has old dependencies; see warning/error messages")
                } else {
                    log.error("Project has old dependencies; see warning/error messages")
                }
            }
            log.info("No problem detected. Good job! :-)")
        } catch (e: IOException) {
            throw MojoExecutionException("Plugin failure, please report it to $GITHUB_ISSUES_URL", e)
        } catch (e: XmlPullParserException) {
            throw MojoExecutionException("Plugin failure, please report it to $GITHUB_ISSUES_URL", e)
        }
    }

    private fun shouldIgnoreGroupId(
        ignoreList: Optional<IgnoreList>,
        oldGroupId: String?,
        newGroupId: String?
    ): Boolean {
        if (ignoreList.isPresent) {
            return ignoreList.get().ignoreList?.any { ignoreItem ->
                ignoreItem.isGroupIdOnly
                    && (ignoreItem.groupId == oldGroupId || ignoreItem.groupId == newGroupId)
            } == true

        }
        return false
    }

    private fun shouldIgnoreArtifactId(
        ignoreList: Optional<IgnoreList>,
        oldGroupId: String?,
        oldArtifactId: String?,
        newGroupId: String?,
        newArtifactId: String?
    ): Boolean {
        if (ignoreList.isPresent) {
            return ignoreList.get().ignoreList?.any { ignoreItem ->
                if (ignoreItem.isGroupIdOnly) {
                    ignoreItem.groupId == oldGroupId || ignoreItem.groupId == newGroupId
                } else {
                    (ignoreItem.groupId == oldGroupId && ignoreItem.artifactId == oldArtifactId
                        || ignoreItem.groupId == newGroupId && ignoreItem.artifactId == newArtifactId)
                }
            } == true

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

        private const val DEFINITIONS_URL =
            "https://raw.githubusercontent.com/jonathanlermitage/oga-maven-plugin/master/uc/og-definitions.json"
        private const val GITHUB_ISSUES_URL = "github.com/jonathanlermitage/oga-maven-plugin"
    }
}
