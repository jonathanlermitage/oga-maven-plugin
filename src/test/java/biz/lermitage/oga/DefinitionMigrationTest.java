package biz.lermitage.oga;

import biz.lermitage.oga.cfg.DefinitionMigration;
import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefinitionMigrationTest {

    @Test
    public void kind_should_default_to_relocation() {
        assertEquals(MigrationKind.RELOCATION, new DefinitionMigration().getKind());
    }

    @Test
    public void kind_should_be_read_from_json() {
        DefinitionMigration successor = new GsonBuilder().create()
            .fromJson("{\"old\":\"a:b\",\"new\":\"c:d\",\"kind\":\"successor\"}", DefinitionMigration.class);
        assertEquals(MigrationKind.SUCCESSOR, successor.getKind());

        DefinitionMigration relocation = new GsonBuilder().create()
            .fromJson("{\"old\":\"a:b\",\"new\":\"c:d\"}", DefinitionMigration.class);
        assertEquals(MigrationKind.RELOCATION, relocation.getKind());
    }

    @Test
    public void relocation_message_should_keep_historical_wording() {
        DefinitionMigration migration = new DefinitionMigration();
        migration.setOld("ant:ant");
        migration.setNewer("org.apache.ant:ant");

        Dependency dep = new Dependency("ant", "ant", "1.9.0", DependencyType.DEPENDENCY);

        assertEquals("'ant:ant' should be replaced by 'org.apache.ant:ant'", migration.buildCheckMessage(dep, false));
    }

    @Test
    public void successor_message_should_warn_against_reusing_the_version() {
        DefinitionMigration migration = new DefinitionMigration();
        migration.setOld("commons-collections:commons-collections");
        migration.setNewer("org.apache.commons:commons-collections4");
        migration.setKind(MigrationKind.SUCCESSOR);
        migration.setNewVersion("4.x");

        Dependency dep = new Dependency("commons-collections", "commons-collections", "3.2.2", DependencyType.DEPENDENCY);
        String message = migration.buildCheckMessage(dep, false);

        assertFalse(message.contains("could be replaced by"));
        assertTrue(message.contains("successor migration, not a drop-in replacement"));
        assertTrue(message.contains("starting at 4.x"));
        assertTrue(message.contains("do not reuse version 3.2.2"));
    }

    @Test
    public void successor_message_should_not_mention_an_unknown_version() {
        DefinitionMigration migration = new DefinitionMigration();
        migration.setOld("commons-collections:commons-collections");
        migration.setNewer("org.apache.commons:commons-collections4");
        migration.setKind(MigrationKind.SUCCESSOR);

        Dependency dep = new Dependency("commons-collections", "commons-collections", null, DependencyType.DEPENDENCY);

        assertFalse(migration.buildCheckMessage(dep, false).contains("do not reuse version"));
    }

    @Test
    public void ignored_successor_message_should_mention_the_ignore_list() {
        DefinitionMigration migration = new DefinitionMigration();
        migration.setOld("commons-collections:commons-collections");
        migration.setNewer("org.apache.commons:commons-collections4");
        migration.setKind(MigrationKind.SUCCESSOR);

        Dependency dep = new Dependency("commons-collections", "commons-collections", "3.2.2", DependencyType.DEPENDENCY);
        String message = migration.buildCheckMessage(dep, true);

        assertTrue(message.contains("could be replaced by"));
        assertTrue(message.contains("excluded by ignore list"));
    }
}
