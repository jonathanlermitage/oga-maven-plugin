package biz.lermitage.oga;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;

public class CheckMojoITest {

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    @Test
    public void testProjectWithClasspathDefinitionFiles() throws Exception {
        // GIVEN a project that contains our definition file
        File classpathResourcesDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/classpath_build_config");
        Verifier classpathVerifier = new Verifier(classpathResourcesDir.getAbsolutePath());
        classpathVerifier.deleteArtifact("biz.lermitage.oga", "classpath-build-config", "1.0.0-SNAPSHOT", "jar");
        classpathVerifier.executeGoal("install");

        // AND a project which uses the definition file as a classpath resource
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko_classpath_definitions");
        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        // WHEN checking the projects dependencies
        // THEN the build fails
        Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check"));

        // AND the build contains a failure message from the classpath definition file
        verifier.verifyTextInLog("[ERROR] (dependency) 'org.mock-server' groupId should be replaced by 'com.example.classpath.dependency'");
    }

    @Test
    public void testProjectWithAdditionalDefinitionFiles() throws Exception {
        String responseContent = "{\"version\": \"1\", \"date\": \"2020/01/02\", \"migration\": [{ \"old\": \"junit\", \"new\": \"org.junit\" }]}";
        mockServerRule.getClient().when(HttpRequest.request().withPath("/remote-og-definitions.json")).respond(HttpResponse.response().withBody(responseContent));

        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko_additional_definitions");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.setSystemProperty("mockserver.host", "localhost:" + mockServerRule.getPort());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check"));

        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'");
        verifier.verifyTextInLog("[ERROR] (dependency) 'junit' groupId should be replaced by 'org.junit'");
        verifier.verifyTextInLog("[ERROR] (dependency) 'org.mock-server' groupId should be replaced by 'com.example.do.no.use.this.dependency'");
    }

    @Test
    public void testProjectWithDefinitionOverride() throws Exception {
        String responseContent = "{\"version\": \"1\", \"date\": \"2020/01/02\", \"migration\": [{ \"old\": \"junit\", \"new\": \"org.junit\" }]}";
        mockServerRule.getClient().when(HttpRequest.request().withPath("/remote-og-definitions.json")).respond(HttpResponse.response().withBody(responseContent));

        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko_definition_url");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.setSystemProperty("mockserver.host", "localhost:" + mockServerRule.getPort());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check"));

        verifier.verifyTextInLog("[ERROR] (dependency) 'junit' groupId should be replaced by 'org.junit'");
    }

    @Test
    public void testProjectWithoutOldDependencies() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ok");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("No problem detected. Good job! :-)");
    }

    @Test
    public void testProjectWithOldDependencies() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check"));

        verifier.verifyTextInLog("[ERROR] (dependency) 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'");
        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'");
        verifier.verifyTextInLog("[ERROR] (plugin) 'pl.project13.maven:git-commit-id-plugin' should be replaced by 'io.github.git-commit-id:git-commit-id-maven-plugin' (context: version 5 relocated, see https://github.com/git-commit-id/git-commit-id-maven-plugin#relocation-of-the-project)");
    }

    @Test
    public void testProjectWithOldDependenciesButDontFail() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko_dont_fail");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyTextInLog("[ERROR] (dependency) 'com.graphql-java:graphql-spring-boot-starter' should be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter'");
        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'");
    }

    @Test
    public void testProjectWithOldDependenciesButIgnored() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko_ignored");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] (dependency) 'com.graphql-java:graphql-spring-boot-starter' could be replaced by 'com.graphql-java-kickstart:graphql-spring-boot-starter' but this migration is excluded by ignore list");
        verifier.verifyTextInLog("[INFO] (dependency) 'bouncycastle' groupId could be replaced by 'org.bouncycastle' but this migration is excluded by ignore list");
    }

    @Test
    public void testProjectWithAbandonedDependencies() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ko_abandoned");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        Assert.assertThrows(VerificationException.class, () -> verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check"));

        verifier.verifyTextInLog("[ERROR] (dependency) 'bouncycastle' groupId should be replaced by 'org.bouncycastle'");
        verifier.verifyTextInLog("[ERROR] (dependency) 'com.jcraft:jsch' should be replaced by 'com.github.mwiede:jsch' (unofficial migration) (context: See https://www.matez.de/index.php/2020/06/22/the-future-of-jsch-without-ssh-rsa/)");
    }

    @Test
    public void testProjectWithAbandonedDependenciesButGroupIdIgnored() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ok_abandoned_but_groupid_ignored");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list");
    }

    @Test
    public void testProjectWithAbandonedDependenciesButArtifactIdIgnored() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ok_abandoned_but_artifactid_ignored");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list");
    }

    @Test
    public void testProjectWithAbandonedDependenciesButProposedGroupIdIgnored() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ok_abandoned_but_proposed_groupid_ignored");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list");
    }

    @Test
    public void testProjectWithAbandonedDependenciesButProposedArtifactIdIgnored() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/biz/lermitage/oga/ok_abandoned_but_proposed_artifactid_ignored");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());

        verifier.deleteArtifact("biz.lermitage.oga", "project-to-test", "1.0.0-SNAPSHOT", "pom");

        verifier.executeGoal("biz.lermitage.oga:oga-maven-plugin:check");

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] (dependency) 'com.jcraft:jsch' could be replaced by 'com.github.mwiede:jsch' (unofficial migration) but this migration is excluded by ignore list");
    }
}
