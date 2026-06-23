package biz.lermitage.oga;

/**
 * Dependency type.
 */
public enum DependencyType {

    DEPENDENCY("dependency"),
    PLUGIN("plugin");

    private final String label;

    DependencyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
