package biz.lermitage.oga.util

import biz.lermitage.oga.Dependency
import biz.lermitage.oga.DependencyState
import biz.lermitage.oga.cfg.DefinitionMigration
import biz.lermitage.oga.cfg.IgnoreList
import java.util.Optional

/**
 * Ignore List tools.
 *
 * @author Jonathan Lermitage
 */
object IgnoreListTools {

    fun shouldIgnoreGroupId(
        ignoreList: Optional<IgnoreList>,
        oldDep: Dependency,
        newDep: DefinitionMigration
    ): Boolean {
        if (ignoreList.isPresent) {
            if (newDep.state == DependencyState.MIGRATED) {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    ignoreItem.isGroupIdOnly && (ignoreItem.groupId == oldDep.groupId || ignoreItem.groupId == newDep.newerGroupId)
                } == true
            } else {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    ignoreItem.isGroupIdOnly && (ignoreItem.groupId == oldDep.groupId /*|| newDep.unofficialGroupIdCandidates.contains(ignoreItem.groupId)*/) // TODO apply ignoreList to candidates
                } == true
            }
        }
        return false
    }

    fun shouldIgnoreArtifactId(
        ignoreList: Optional<IgnoreList>,
        oldDep: Dependency,
        newDep: DefinitionMigration
    ): Boolean {
        if (ignoreList.isPresent) {
            if (newDep.state == DependencyState.MIGRATED) {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    if (ignoreItem.isGroupIdOnly) {
                        ignoreItem.groupId == oldDep.groupId || ignoreItem.groupId == newDep.newerGroupId
                    } else {
                        (ignoreItem.groupId == oldDep.groupId && ignoreItem.artifactId == oldDep.artifactId
                            || ignoreItem.groupId == newDep.newerGroupId && ignoreItem.artifactId == newDep.newerArtifactId)
                    }
                } == true
            } else {
                return ignoreList.get().ignoreList?.any { ignoreItem ->
                    if (ignoreItem.isGroupIdOnly) {
                        ignoreItem.groupId == oldDep.groupId || newDep.proposedGroupIds.contains(ignoreItem.groupId)
                    } else {
                        (ignoreItem.groupId == oldDep.groupId && ignoreItem.artifactId == oldDep.artifactId
                            /*|| newDep.unofficialGroupIdArtifactIdCandidates.contains(ignoreItem.item)*/) // TODO apply ignoreList to candidates
                    }
                } == true
            }
        }
        return false
    }
}
