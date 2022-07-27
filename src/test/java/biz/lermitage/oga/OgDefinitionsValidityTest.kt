package biz.lermitage.oga

import biz.lermitage.oga.cfg.Definitions
import com.google.gson.GsonBuilder
import org.apache.commons.io.FileUtils
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class OgDefinitionsValidityTest {

    @Throws(Exception::class)
    @Test
    fun testFindMutuallyExclusiveDefinitions() {
        val definitionsAsString = FileUtils.readFileToString(File("uc/og-definitions.json"))
        val ogDefs = GsonBuilder().create().fromJson(definitionsAsString, Definitions::class.java).migration
        val errors = ArrayList<String>()
        ogDefs?.forEach { mig1 ->
            ogDefs.forEach { mig2 ->
                val old =
                    mig1.oldGroupId + ":" + mig1.oldArtifactId + " <-> " + mig1.newerGroupId + ":" + mig1.newerArtifactId
                val newer =
                    mig2.newerGroupId + ":" + mig2.newerArtifactId + " <-> " + mig2.oldGroupId + ":" + mig2.oldArtifactId
                val revert =
                    mig1.newerGroupId + ":" + mig1.newerArtifactId + " <-> " + mig1.oldGroupId + ":" + mig1.oldArtifactId
                if (old == newer && !errors.contains(revert)) {
                    errors.add(old)
                }
            }
        }
        if (errors.isNotEmpty()) {
            println("Found mutually exclusive definitions:")
            errors.forEach { error -> println(" - $error") }
            fail("Should not contain mutually exclusive definitions")
        }
    }
}
