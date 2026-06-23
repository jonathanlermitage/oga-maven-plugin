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
        return item.split(":")[0];
    }

    public String getArtifactId() {
        if (isGroupIdOnly()) {
            return "";
        }
        return item.split(":")[1];
    }

    public boolean isGroupIdOnly() {
        return !item.contains(":");
    }
}
