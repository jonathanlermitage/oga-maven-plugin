package biz.lermitage.oga

import biz.lermitage.oga.util.IOTools
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URL

/**
 * Goal which checks that no dependency uses a deprecated groupId.
 *
 * @author Jonathan Lermitage
 * @version 1
 */
@Mojo(name = "check", requiresProject = true, requiresOnline = true, threadSafe = true)
class CheckMojo : AbstractMojo() {

    /** Alternative location for og-definitions.json config file. */
    @Parameter(name = "ogDefinitionsUrl")
    private val ogDefinitionsUrl: String? = null

    /**
     * Execute goal.
     */
    @Throws(MojoExecutionException::class)
    override fun execute() {
        log.info("Old GroupId Alerter - $GITHUB_ISSUES_URL")

        try {
            val definitions = URL(ogDefinitionsUrl ?: DEFINITIONS_URL).let { IOTools.readDefinitionsFromUrl(it) }
            log.debug("Loaded definitions file version: ${definitions.version}, ${definitions.date}")

            val model = readModel(File("pom.xml"))
            val dependencies = listDependencies(model)//.filter { dep -> !definitions.whitelist!!.contains("${dep.groupId}:${dep.artifactId}") }
            val deprecatedDependencies: HashSet<String> = HashSet()
            log.info("Checking dependencies...")

            // compare project dependencies to integrated black-list
            definitions.migration!!.forEach { mig ->
                if (mig.isGroupIdOnly) {
                    dependencies.forEach { dep ->
                        if (dep.groupId == mig.oldGroupId) {
                            val msg = "'${dep.groupId}' groupId should be replaced by '${mig.newerGroupId}'"
                            log.error(msg)
                            deprecatedDependencies.add("${dep.groupId}:${dep.artifactId}")
                        }
                    }
                } else {
                    dependencies.forEach { dep ->
                        if (dep.groupId == mig.oldGroupId && dep.artifactId == mig.oldArtifactId) {
                            val msg = "'${dep.groupId}:${dep.artifactId}' should be replaced by '${mig.newerGroupId}:${mig.newerArtifactId}'"
                            log.error(msg)
                            deprecatedDependencies.add("${dep.groupId}:${dep.artifactId}")
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
                throw MojoExecutionException("Project has old dependencies; see warning/error messages")
            }
            log.info("No problem detected. Good job! :-)")
        } catch (e: IOException) {
            throw MojoExecutionException("Plugin failure, please report it to $GITHUB_ISSUES_URL", e)
        } catch (e: XmlPullParserException) {
            throw MojoExecutionException("Plugin failure, please report it to $GITHUB_ISSUES_URL", e)
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readModel(pomFile: File?): Model {
        // parse pom.xml, see https://stackoverflow.com/questions/4838591/is-there-a-library-for-reading-maven2-3-pom-xml-files
        return MavenXpp3Reader().read(FileReader(pomFile!!))
    }

    private fun listDependencies(model: Model): List<Dependency> {
        log.debug("Listing dependencies:")
        val dependencies = model.dependencies
        dependencies.sortWith(Comparator { o1, o2 -> ("${o1.groupId}:${o1.artifactId}").compareTo("${o2.groupId}:${o2.artifactId}") })
        for (dep in dependencies) {
            log.debug("  ${dep.groupId}:${dep.artifactId}")
        }
        return dependencies
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

        private const val DEFINITIONS_URL = "https://raw.githubusercontent.com/jonathanlermitage/oga-maven-plugin/master/uc/og-definitions.json"
        private const val GITHUB_ISSUES_URL = "github.com/jonathanlermitage/oga-maven-plugin"
    }
}
