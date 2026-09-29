package biz.lermitage.oga;

import biz.lermitage.oga.cfg.DefinitionMigration;
import biz.lermitage.oga.cfg.IgnoreItem;
import biz.lermitage.oga.cfg.IgnoreList;
import biz.lermitage.oga.util.IgnoreListTools;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IgnoreListToolsTest {

    private static Optional<IgnoreList> ignoreList(String item) {
        IgnoreItem ignoreItem = new IgnoreItem();
        ignoreItem.setItem(item);
        IgnoreList list = new IgnoreList();
        list.setIgnoreList(Collections.singletonList(ignoreItem));
        return Optional.of(list);
    }

    private static DefinitionMigration migration() {
        DefinitionMigration migration = new DefinitionMigration();
        migration.setOld("foo:bar");
        migration.setNewer("org.example:bar");
        return migration;
    }

    @Test
    public void version_qualified_ignore_item_should_only_match_the_same_version() {
        DefinitionMigration migration = migration();

        Dependency matchingVersion = new Dependency("foo", "bar", "1.0", DependencyType.DEPENDENCY);
        Dependency otherVersion = new Dependency("foo", "bar", "2.0", DependencyType.DEPENDENCY);

        assertTrue(IgnoreListTools.shouldIgnoreArtifactId(ignoreList("foo:bar:1.0"), matchingVersion, migration));
        assertFalse(IgnoreListTools.shouldIgnoreArtifactId(ignoreList("foo:bar:1.0"), otherVersion, migration));
    }

    @Test
    public void unqualified_ignore_item_should_match_any_version() {
        DefinitionMigration migration = migration();

        Dependency dep = new Dependency("foo", "bar", "2.0", DependencyType.DEPENDENCY);

        assertTrue(IgnoreListTools.shouldIgnoreArtifactId(ignoreList("foo:bar"), dep, migration));
    }

    @Test
    public void group_id_only_ignore_item_should_still_match() {
        DefinitionMigration migration = migration();

        Dependency dep = new Dependency("foo", "bar", "2.0", DependencyType.DEPENDENCY);

        assertTrue(IgnoreListTools.shouldIgnoreArtifactId(ignoreList("foo"), dep, migration));
    }
}
