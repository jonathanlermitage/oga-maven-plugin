package biz.lermitage.oga.cfg;

import biz.lermitage.oga.DependencyState;
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
