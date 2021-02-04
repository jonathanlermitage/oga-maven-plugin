package biz.lermitage.oga

import biz.lermitage.oga.cfg.Definitions
import com.google.gson.GsonBuilder
import junit.framework.TestCase
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*
import java.util.function.Consumer

class OgDefinitionsValidityTest : TestCase() {

    @Throws(Exception::class)
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
                if (old == newer) {
                    errors.add(old)
                }
            }
        }
        if (errors.isNotEmpty()) {
            println("Found mutually exclusive definitions:")
            errors.forEach(Consumer { println(it) })
            fail("Should not contain mutually exclusive definitions")
        }
    }
}
