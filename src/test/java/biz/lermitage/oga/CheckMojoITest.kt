package biz.lermitage.oga

import junit.framework.TestCase
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.apache.maven.it.util.ResourceExtractor

class CheckMojoITest : TestCase() {

    @Throws(Exception::class)
    fun testProjectWithoutOldDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ok")

        val verifier: Verifier

        verifier = Verifier(testDir.absolutePath)
        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("No problem detected. Good job! :-)")

        verifier.resetStreams()
    }

    @Throws(Exception::class)
    fun testProjectWithOldDependencies() {
        val testDir = ResourceExtractor.simpleExtractResources(javaClass, "/biz/lermitage/oga/ko")

        val verifier: Verifier

        verifier = Verifier(testDir.absolutePath)
        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom")

        try {
            verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check")
            fail("invocation should fail")
        } catch (e: VerificationException) {
            // plugin should fail here. Assert message log later
        }

        verifier.verifyTextInLog("[ERROR] 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'")
        verifier.verifyTextInLog("[ERROR] 'bouncycastle' groupId should be replaced by 'org.bouncycastle'")
        //verifier.verifyTextInLog("[ERROR] 'org.apache.commons:commons-io' has a migration notice on https://mvnrepository.com/artifact/org.apache.commons/commons-io")

        verifier.resetStreams()
    }
}
