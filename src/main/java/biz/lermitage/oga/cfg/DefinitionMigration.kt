package biz.lermitage.oga.cfg

import com.google.gson.annotations.SerializedName

/**
 * Definition migration.
 *
 * @author Jonathan Lermitage
 * @version 1
 */
class DefinitionMigration {

    var old: String? = null

    @SerializedName("new")
    var newer: String? = null

    val oldGroupId: String?
        get() = if (isGroupIdOnly) old else old!!.split(":")[0]

    val oldArtifactId: String
        get() = if (isGroupIdOnly) "" else old!!.split(":")[1]

    val newerGroupId: String?
        get() = if (isGroupIdOnly) newer else newer!!.split(":")[0]

    val newerArtifactId: String
        get() = if (isGroupIdOnly) "" else newer!!.split(":")[1]

    val isGroupIdOnly: Boolean
        get() = !old!!.contains(":")

    val context: String? = null
}
