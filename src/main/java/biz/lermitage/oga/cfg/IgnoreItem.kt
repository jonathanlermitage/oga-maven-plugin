package biz.lermitage.oga.cfg

/**
 * Ignored definition.
 *
 * @author Jonathan Lermitage
 */
class IgnoreItem {

    var item: String? = null

    val groupId: String?
        get() = if (isGroupIdOnly) item else item!!.split(":")[0]

    val artifactId: String
        get() = if (isGroupIdOnly) "" else item!!.split(":")[1]

    val isGroupIdOnly: Boolean
        get() = !item!!.contains(":")
}
