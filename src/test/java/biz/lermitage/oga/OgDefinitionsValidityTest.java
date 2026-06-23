package biz.lermitage.oga;

import biz.lermitage.oga.cfg.DefinitionMigration;
import biz.lermitage.oga.cfg.Definitions;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class OgDefinitionsValidityTest {

    @Test
    public void migrations_should_not_be_mutually_exclusive() throws Exception {
        String definitionsAsString = FileUtils.readFileToString(new File("uc/og-definitions.json"), "UTF-8");
        List<DefinitionMigration> ogDefs = new GsonBuilder().create().fromJson(definitionsAsString, Definitions.class).getMigration();
        List<String> errors = new ArrayList<>();
        if (ogDefs != null) {
            for (DefinitionMigration mig1 : ogDefs) {
                for (DefinitionMigration mig2 : ogDefs) {
                    String old = mig1.getOldGroupId() + ":" + mig1.getOldArtifactId() + " <-> " + mig1.getNewerGroupId() + ":" + mig1.getNewerArtifactId();
                    String newer = mig2.getNewerGroupId() + ":" + mig2.getNewerArtifactId() + " <-> " + mig2.getOldGroupId() + ":" + mig2.getOldArtifactId();
                    String revert = mig1.getNewerGroupId() + ":" + mig1.getNewerArtifactId() + " <-> " + mig1.getOldGroupId() + ":" + mig1.getOldArtifactId();
                    if (old.equals(newer) && !errors.contains(revert)) {
                        errors.add(old);
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            fail("Should not contain mutually exclusive definitions: " + errors);
        }
    }

    @Test
    public void migrations_should_not_be_official_and_unofficial_simultaneously() throws Exception {
        String definitionsAsString = FileUtils.readFileToString(new File("uc/og-definitions.json"), "UTF-8");
        String unofficialDefinitionsAsString = FileUtils.readFileToString(new File("uc/og-unofficial-definitions.json"), "UTF-8");
        List<DefinitionMigration> ogDefsMigrations = new GsonBuilder().create()
            .fromJson(definitionsAsString, Definitions.class).getMigration();
        List<DefinitionMigration> ogUnofficialDefsMigrations = new GsonBuilder().create()
            .fromJson(unofficialDefinitionsAsString, Definitions.class).getMigration();
        List<String> ogDefs = new ArrayList<>();
        for (DefinitionMigration dm : ogDefsMigrations) {
            ogDefs.add(dm.getOld());
        }
        List<String> ogUnofficialDefs = new ArrayList<>();
        for (DefinitionMigration dm : ogUnofficialDefsMigrations) {
            ogUnofficialDefs.add(dm.getOld());
        }
        List<String> errors = new ArrayList<>();
        for (String unofficialDef : ogUnofficialDefs) {
            if (ogDefs.contains(unofficialDef)) {
                errors.add(unofficialDef);
            }
        }
        if (!errors.isEmpty()) {
            fail("Should not contain definitions declared as official and unofficial: " + errors);
        }
    }
}
