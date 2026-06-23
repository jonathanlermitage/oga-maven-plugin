package biz.lermitage.oga.cfg;

import java.util.List;

/**
 * List of definitions.
 *
 * @author Jonathan Lermitage
 */
public class Definitions {

    private List<DefinitionMigration> migration;
    private String version;
    private String date;

    public List<DefinitionMigration> getMigration() {
        return migration;
    }

    public void setMigration(List<DefinitionMigration> migration) {
        this.migration = migration;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
