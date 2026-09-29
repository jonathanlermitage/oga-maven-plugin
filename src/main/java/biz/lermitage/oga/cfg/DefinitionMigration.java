package biz.lermitage.oga.cfg;

import biz.lermitage.oga.Dependency;
import biz.lermitage.oga.DependencyState;
import biz.lermitage.oga.MigrationKind;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition migration.
 *
 * @author Jonathan Lermitage
 */
@SuppressWarnings("unused")
public class DefinitionMigration {

    private String old;

    @SerializedName("new")
    private String newer;

    private MigrationKind kind;

    private String newVersion;

    private String context;

    private List<String> proposal;

    public String getOld() {
        return old;
    }

    public void setOld(String old) {
        this.old = old;
    }

    public String getNewer() {
        return newer;
    }

    public void setNewer(String newer) {
        this.newer = newer;
    }

    public MigrationKind getKind() {
        return kind == null ? MigrationKind.RELOCATION : kind;
    }

    public void setKind(MigrationKind kind) {
        this.kind = kind;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public String getOldGroupId() {
        if (isGroupIdOnly()) {
            return old;
        }
        return old.split(":")[0];
    }

    public String getOldArtifactId() {
        if (isGroupIdOnly()) {
            return "";
        }
        return old.split(":")[1];
    }

    public String getNewerGroupId() {
        if (isGroupIdOnly()) {
            return newer;
        }
        return newer.split(":")[0];
    }

    public String getNewerArtifactId() {
        if (isGroupIdOnly()) {
            return "";
        }
        return newer.split(":")[1];
    }

    public boolean isGroupIdOnly() {
        return !old.contains(":");
    }

    public String getContext() {
        return context;
    }

    public List<String> getProposal() {
        return proposal;
    }

    public List<String> getProposedGroupIds() {
        List<String> result = new ArrayList<>();
        for (String s : proposal) {
            if (!s.contains(":")) {
                result.add(s);
            }
        }
        return result;
    }

    public List<String> getProposedGroupIdArtifactIds() {
        List<String> result = new ArrayList<>();
        for (String s : proposal) {
            if (s.contains(":")) {
                result.add(s);
            }
        }
        return result;
    }

    public DependencyState getState() {
        return proposal == null ? DependencyState.MIGRATED : DependencyState.ABANDONED;
    }

    /**
     * Describe the dependency matched by this migration, e.g. {@code 'foo' groupId} or
     * {@code 'foo:bar'}. The dependency version is only included when {@code includeVersion} is
     * {@code true} and known.
     *
     * @param oldDep         the project dependency
     * @param includeVersion whether to append the dependency version
     * @return the dependency description used in check messages
     */
    public String describeOldDependency(Dependency oldDep, boolean includeVersion) {
        if (isGroupIdOnly()) {
            return "'" + oldDep.getGroupId() + "' groupId";
        }
        StringBuilder coordinate = new StringBuilder(oldDep.getGroupId()).append(':').append(oldDep.getArtifactId());
        if (includeVersion && oldDep.getVersion() != null && !oldDep.getVersion().isEmpty()) {
            coordinate.append(':').append(oldDep.getVersion());
        }
        return "'" + coordinate + "'";
    }

    /**
     * Build the message displayed for a dependency matched by this migration. Relocation messages
     * keep the historical wording. Successor messages make it explicit that the new artifact has
     * its own versioning, so the current version must not be reused (see issue #19).
     *
     * @param oldDep  the project dependency
     * @param ignored whether the migration is excluded by the ignore list
     * @return the message to log
     */
    public String buildCheckMessage(Dependency oldDep, boolean ignored) {
        if (ignored) {
            return describeOldDependency(oldDep, false) + " could be replaced by " + replacementDescription(oldDep) +
                " but this migration is excluded by ignore list";
        }
        StringBuilder message = new StringBuilder(describeOldDependency(oldDep, false))
            .append(" should be replaced by ")
            .append(replacementDescription(oldDep));
        if (context != null && !context.isEmpty()) {
            message.append(" (context: ").append(context).append(')');
        }
        return message.toString();
    }

    private String replacementDescription(Dependency oldDep) {
        if (getState() == DependencyState.ABANDONED) {
            return proposedMigrationToString();
        }
        if (getKind() == MigrationKind.SUCCESSOR) {
            StringBuilder result = new StringBuilder("'").append(newer).append('\'')
                .append(" (successor migration, not a drop-in replacement: the new artifact has its own versioning");
            if (newVersion != null && !newVersion.isEmpty()) {
                result.append(" starting at ").append(newVersion);
            }
            if (oldDep.getVersion() != null && !oldDep.getVersion().isEmpty()) {
                result.append("; do not reuse version ").append(oldDep.getVersion());
            }
            result.append(')');
            return result.toString();
        }
        return "'" + newer + "'";
    }

    public String proposedMigrationToString() {
        if (getState() == DependencyState.MIGRATED) {
            return "'" + newer + "'";
        } else {
            StringBuilder buffer = new StringBuilder();
            for (String s : proposal) {
                buffer.append("'").append(s).append("' or ");
            }
            String result = buffer.toString();
            if (result.endsWith(" or ")) {
                result = result.substring(0, result.length() - 4);
            }
            return result + " (unofficial migration" + (proposal.size() > 1 ? "s" : "") + ")";
        }
    }
}
