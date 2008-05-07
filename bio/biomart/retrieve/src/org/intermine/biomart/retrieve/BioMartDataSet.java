package org.intermine.biomart.retrieve;

import java.util.ArrayList;
import java.util.List;

public class BioMartDataSet
{
    private String name;
    private List<BioMartField> attributes;
    private List<String> collections;
    private List<BioMartLink> exportables;
    private List<BioMartLink> importables;
    
    public BioMartDataSet() {
        super();
        attributes = new ArrayList<BioMartField>();
        exportables = new ArrayList<BioMartLink>();
        importables = new ArrayList<BioMartLink>();
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
    public List<BioMartLink> getExportables() {
        return exportables;
    }

    /**
     * @param exportables the exportables to set
     */
    public void addExportable(BioMartLink link) {
        this.exportables.add(link);
    }

    /**
     * @return the importables
     */
    public List<BioMartLink> getImportables() {
        return importables;
    }

    /**
     * @param importables the importables to set
     */
    public void addImportable(BioMartLink link) {
        this.importables.add(link);
    }
    
}
