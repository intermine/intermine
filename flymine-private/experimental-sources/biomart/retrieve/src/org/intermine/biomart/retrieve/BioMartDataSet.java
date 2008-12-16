package org.intermine.biomart.retrieve;

import java.util.ArrayList;
import java.util.List;

/**
 * @author watkins
 *
 */
public class BioMartDataSet
{
    private String name;
    private String type;
    private String displayName;
    private boolean visible;
    private List<BioMartField> attributes;
    private List<String> collections;
    private List<MartExportable> exportables;
    private List<MartImportable> importables;
    
    public BioMartDataSet() {
        super();
        attributes = new ArrayList<BioMartField>();
        exportables = new ArrayList<MartExportable>();
        importables = new ArrayList<MartImportable>();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the attributes
     */
    public List<BioMartField> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(List<BioMartField> attributes) {
        this.attributes = attributes;
    }
    
    public void addAttribute(BioMartField field) {
        this.attributes.add(field);
    }

    /**
     * @return the collections
     */
    public List<String> getCollections() {
        return collections;
    }

    /**
     * @param collections the collections to set
     */
    public void setCollections(List<String> collections) {
        this.collections = collections;
    }
    
    public void addCollection(String tableName) {
        this.collections.add(tableName);
    }

    /**
     * @return the exportables
     */
    public List<MartExportable> getExportables() {
        return exportables;
    }

    /**
     * @param exportables the exportables to set
     */
    public void addExportable(MartExportable link) {
        this.exportables.add(link);
    }

    /**
     * @return the importables
     */
    public List<MartImportable> getImportables() {
        return importables;
    }

    /**
     * @param importables the importables to set
     */
    public void addImportable(MartImportable importable) {
        this.importables.add(importable);
    }
    
}
