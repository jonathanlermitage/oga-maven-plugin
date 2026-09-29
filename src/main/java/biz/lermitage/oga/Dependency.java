package biz.lermitage.oga;

import java.util.Objects;

public class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final DependencyType type;

    public Dependency(String groupId, String artifactId, DependencyType type) {
        this(groupId, artifactId, null, type);
    }

    public Dependency(String groupId, String artifactId, String version, DependencyType type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public DependencyType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(groupId, that.groupId) &&
            Objects.equals(artifactId, that.artifactId) &&
            Objects.equals(version, that.version) &&
            type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, type);
    }

    @Override
    public String toString() {
        return "Dependency(" +
            "groupId='" + groupId + '\'' +
            ", artifactId='" + artifactId + '\'' +
            ", version='" + version + '\'' +
            ", type=" + type +
            ')';
    }
}
