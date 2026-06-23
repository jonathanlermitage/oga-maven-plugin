package biz.lermitage.oga.util;

import biz.lermitage.oga.Dependency;
import biz.lermitage.oga.DependencyState;
import biz.lermitage.oga.cfg.DefinitionMigration;
import biz.lermitage.oga.cfg.IgnoreItem;
import biz.lermitage.oga.cfg.IgnoreList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Ignore List tools.
 *
 * @author Jonathan Lermitage
 */
public class IgnoreListTools {

    private IgnoreListTools() {
    }

    public static boolean shouldIgnoreGroupId(
        Optional<IgnoreList> ignoreList,
        Dependency oldDep,
        DefinitionMigration newDep
    ) {
        if (ignoreList.isPresent()) {
            List<IgnoreItem> items = ignoreList.get().getIgnoreList();
            if (newDep.getState() == DependencyState.MIGRATED) {
                if (items != null) {
                    for (IgnoreItem ignoreItem : items) {
                        if (ignoreItem.isGroupIdOnly() && (ignoreItem.getGroupId().equals(oldDep.getGroupId()) || ignoreItem.getGroupId().equals(newDep.getNewerGroupId()))) {
                            return true;
                        }
                    }
                }
                return false;
            } else {
                if (items != null) {
                    for (IgnoreItem ignoreItem : items) {
                        if ((ignoreItem.isGroupIdOnly() && ignoreItem.getGroupId().equals(oldDep.getGroupId())) || shouldIgnoreProposal(ignoreList, newDep)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    public static boolean shouldIgnoreArtifactId(
        Optional<IgnoreList> ignoreList,
        Dependency oldDep,
        DefinitionMigration newDep
    ) {
        if (ignoreList.isPresent()) {
            List<IgnoreItem> items = ignoreList.get().getIgnoreList();
            if (newDep.getState() == DependencyState.MIGRATED) {
                if (items != null) {
                    for (IgnoreItem ignoreItem : items) {
                        if (ignoreItem.isGroupIdOnly()) {
                            if (ignoreItem.getGroupId().equals(oldDep.getGroupId()) || ignoreItem.getGroupId().equals(newDep.getNewerGroupId())) {
                                return true;
                            }
                        } else {
                            if ((ignoreItem.getGroupId().equals(oldDep.getGroupId()) && ignoreItem.getArtifactId().equals(oldDep.getArtifactId()))
                                || (ignoreItem.getGroupId().equals(newDep.getNewerGroupId()) && ignoreItem.getArtifactId().equals(newDep.getNewerArtifactId()))) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            } else {
                if (items != null) {
                    for (IgnoreItem ignoreItem : items) {
                        if (ignoreItem.isGroupIdOnly()) {
                            if (ignoreItem.getGroupId().equals(oldDep.getGroupId()) || newDep.getProposedGroupIds().contains(ignoreItem.getGroupId())) {
                                return true;
                            }
                        } else {
                            if ((ignoreItem.getGroupId().equals(oldDep.getGroupId()) && ignoreItem.getArtifactId().equals(oldDep.getArtifactId())) || shouldIgnoreProposal(ignoreList, newDep)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    private static boolean shouldIgnoreProposal(Optional<IgnoreList> ignoreList, DefinitionMigration newDep) {
        if (ignoreList.isPresent()) {
            List<IgnoreItem> items = ignoreList.get().getIgnoreList();
            if (items != null) {
                List<String> ignoredDeps = new ArrayList<>();
                for (IgnoreItem ignoreItem : items) {
                    ignoredDeps.add(ignoreItem.getItem());
                }
                return new HashSet<>(ignoredDeps).containsAll(newDep.getProposal());
            }
        }
        return false;
    }
}
