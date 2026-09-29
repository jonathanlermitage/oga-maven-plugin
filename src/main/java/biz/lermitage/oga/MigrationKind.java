package biz.lermitage.oga;

import com.google.gson.annotations.SerializedName;

/**
 * Kind of a migration.
 */
public enum MigrationKind {

    /**
     * Same artifact relocated to new coordinates: the version is unchanged, so the new coordinates
     * can generally be used as a drop-in replacement (e.g. {@code ant:ant} to {@code org.apache.ant:ant}).
     */
    @SerializedName("relocation")
    RELOCATION,

    /**
     * Different artifact that supersedes the old one: the new artifact has its own versioning and
     * probably its own API, so the current version must not be reused (e.g.
     * {@code commons-collections:commons-collections} to {@code org.apache.commons:commons-collections4}).
     */
    @SerializedName("successor")
    SUCCESSOR
}
