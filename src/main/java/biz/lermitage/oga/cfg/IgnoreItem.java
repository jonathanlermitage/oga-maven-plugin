package biz.lermitage.oga.cfg;

/**
 * Ignored definition.
 *
 * @author Jonathan Lermitage
 */
public class IgnoreItem {

    private String item;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getGroupId() {
        if (isGroupIdOnly()) {
            return item;
        }
        return splitItem()[0];
    }

    public String getArtifactId() {
        if (isGroupIdOnly() || splitItem().length < 2) {
            return "";
        }
        return splitItem()[1];
    }

    /**
     * Optional version qualifier of this ignore item, only relevant for
     * {@code groupId:artifactId:version} entries. Returns {@code null} when the item is not
     * version-qualified.
     *
     * @return the ignored version, or {@code null}
     */
    public String getVersion() {
        String[] parts = splitItem();
        if (parts.length >= 3 && !parts[2].isEmpty()) {
            return parts[2];
        }
        return null;
    }

    public boolean isVersionQualified() {
        return getVersion() != null;
    }

    public boolean isGroupIdOnly() {
        return !item.contains(":");
    }

    private String[] splitItem() {
        return item.split(":");
    }
}
