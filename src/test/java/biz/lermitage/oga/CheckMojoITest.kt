package biz.lermitage.oga

import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.apache.maven.it.util.ResourceExtractor
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.mockserver.junit.MockServerRule
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response

class CheckMojoITest {

    @Rule
    @JvmField
    var mockServerRule: MockServerRule = MockServerRule(this)

    @Throws(Exception::class)
    @Test
    fun testProjectWithAdditionalDefinitionFiles() {
        val responseContent = """{"version": "1", "date": "2020/01/02", "migration": [{ "old": "junit", "new": "org.junit" }]}"""
        mockServerRule.client.`when`(request().withPath("/remote-og-definitions.json")).respond(response().withBody(responseContent))

        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_additional_definitions")

        val verifier = Verifier(testDir.absolutePath)

        verifier.setSystemProperty("mockserver.host", "localhost:" + mockServerRule.port)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        assertThrows(VerificationException::class.java) { verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check") }

        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")
        verifier.verifyTextInLog("[ERROR] (dependency) 'junit' groupId should be replaced by 'org.junit'")
        verifier.verifyTextInLog("[ERROR] (dependency) 'org.mock-server' groupId should be replaced by 'com.example.do.no.use.this.dependency'")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithDefinitionOverride() {
        val responseContent = """{"version": "1", "date": "2020/01/02", "migration": [{ "old": "junit", "new": "org.junit" }]}"""
        mockServerRule.client.`when`(request().withPath("/remote-og-definitions.json")).respond(response().withBody(responseContent))

        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_definition_url")

        val verifier = Verifier(testDir.absolutePath)

        verifier.setSystemProperty("mockserver.host", "localhost:" + mockServerRule.port)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        assertThrows(VerificationException::class.java) { verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check") }

        verifier.verifyTextInLog("[ERROR] (dependency) 'junit' groupId should be replaced by 'org.junit'")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithoutOldDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("No problem detected. Good job! :-)")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithOldDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        assertThrows(VerificationException::class.java) { verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check") }

        verifier.verifyTextInLog("[ERROR] (dependency) 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'")
        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")
        verifier.verifyTextInLog("[ERROR] (plugin) 'pl.project13.maven:git-commit-id-plugin' should be replaced by 'io.github.git-commit-id:git-commit-id-maven-plugin' (context: version 5 relocated, see https://github.com/git-commit-id/git-commit-id-maven-plugin#relocation-of-the-project)")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithOldDependenciesButDontFail() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_dont_fail")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyTextInLog("[ERROR] (dependency) 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'")
        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithOldDependenciesButIgnored() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_ignored")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("[INFO] (dependency) 'com.graphql-java:graphql-spring-boot-starter' could be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter' but this migration is excluded by ignore list")
        verifier.verifyTextInLog("[INFO] (dependency) 'bouncycastle' groupId could be replaced by 'org.bouncycastle' but this migration is excluded by ignore list")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithAbandonedDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_abandoned")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        assertThrows(VerificationException::class.java) { verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check") }

        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")
        verifier.verifyTextInLog("[ERROR] (dependency) 'com.jcraft:jsch' should be replaced by 'com.github.mwiede:jsch' (unofficial migration) (context: See https://www.matez.de/index.php/2020/06/22/the-future-of-jsch-without-ssh-rsa/)")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithAbandonedDependenciesButGroupIdIgnored() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok_abandoned_but_groupid_ignored")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithAbandonedDependenciesButArtifactIdIgnored() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok_abandoned_but_artifactid_ignored")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithAbandonedDependenciesButProposedGroupIdIgnored() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok_abandoned_but_proposed_groupid_ignored")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list")
    }

    @Throws(Exception::class)
    @Test
    fun testProjectWithAbandonedDependenciesButProposedArtifactIdIgnored() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok_abandoned_but_proposed_artifactid_ignored")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list")
    }
}
