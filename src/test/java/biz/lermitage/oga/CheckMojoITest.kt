package biz.lermitage.oga

import junit.framework.TestCase
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.apache.maven.it.util.ResourceExtractor

class CheckMojoITest : TestCase() {

    @Throws(Exception::class)
    fun testProjectWithoutOldDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("No problem detected. Good job! :-)")

        verifier.resetStreams()
    }

    @Throws(Exception::class)
    fun testProjectWithOldDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        try {
            verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")
            fail("invocation should fail")
        } catch (e: VerificationException) {
            // plugin should fail here. Assert message log later
        }

        verifier.verifyTextInLog("[ERROR] 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'")
        verifier.verifyTextInLog("[ERROR] 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")
        verifier.verifyTextInLog("[ERROR] 'pl.project13.maven:git-commit-id-plugin' should be replaced by 'io.github.git-commit-id:git-commit-id-maven-plugin' (context: version 5 relocated, see https://github.com/git-commit-id/git-commit-id-maven-plugin#relocation-of-the-project)")

        verifier.resetStreams()
    }

    @Throws(Exception::class)
    fun testProjectWithOldDependenciesButDontFail() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_dont_fail")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        try {
            verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")
        } catch (e: VerificationException) {
            fail("invocation should not fail")
        }

        verifier.verifyTextInLog("[ERROR] 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'")
        verifier.verifyTextInLog("[ERROR] 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")

        verifier.resetStreams()
    }

    @Throws(Exception::class)
    fun testProjectWithOldDependenciesButIgnored() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko_ignored")

        val verifier = Verifier(testDir.absolutePath)

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        try {
            verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")
        } catch (e: VerificationException) {
            fail("invocation should not fail")
        }

        verifier.verifyTextInLog("[INFO] 'com.graphql-java:graphql-spring-boot-starter' could be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter' but it's excluded by ignore list")
        verifier.verifyTextInLog("[INFO] 'bouncycastle' groupId could be replaced by 'org.bouncycastle' but it's excluded by ignore list")

        verifier.resetStreams()
    }
}
