package biz.lermitage.oga.cfg

import biz.lermitage.oga.DependencyState
import com.google.gson.annotations.SerializedName

/**
 * Definition migration.
 *
 * @author Jonathan Lermitage
 */
class DefinitionMigration {

    lateinit var old: String

    @SerializedName("new")
    var newer: String? = null

    val oldGroupId: String
        get() = if (isGroupIdOnly) old else old.split(":")[0]

    val oldArtifactId: String
        get() = if (isGroupIdOnly) "" else old.split(":")[1]

    val newerGroupId: String?
        get() = if (isGroupIdOnly) newer else newer!!.split(":")[0]

    val newerArtifactId: String
        get() = if (isGroupIdOnly) "" else newer!!.split(":")[1]

    val isGroupIdOnly: Boolean
        get() = !old.contains(":")

    val context: String? = null

    val proposal: List<String>? = null

    val proposedGroupIds: List<String>
        get() = proposal!!.filter { s -> !s.contains(":") }.toList()

    val proposedGroupIdArtifactIds: List<String>
        get() = proposal!!.filter { s -> s.contains(":") }.toList()

    val state: DependencyState
        get() = if (proposal == null) DependencyState.MIGRATED else DependencyState.ABANDONED

    fun proposedMigrationToString(): String {
        return if (state == DependencyState.MIGRATED) {
            "'$newer'"
        } else {
            val buffer = StringBuilder()
            proposal!!.forEach { s -> buffer.append("'$s' or ") }
            buffer.toString().removeSuffix(" or ") + " (unofficial migration" + (if (proposal.size > 1) "s" else "") + ")"
        }
    }
}
