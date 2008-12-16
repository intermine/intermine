package org.intermine.biomart.retrieve;

import java.util.List;

public class MartImportable
{
    private List filters;
    private String internalName;
    private String linkName;
    private String name;
    private String type;
    /**
     * @return the filters
     */
    public List getFilters() {
        return filters;
    }
    /**
     * @param filters the filters to set
     */
    public void setFilters(List filters) {
        this.filters = filters;
    }
    /**
     * @return the internalName
     */
    public String getInternalName() {
        return internalName;
    }
    /**
     * @param internalName the internalName to set
     */
    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }
    /**
     * @return the linkName
     */
    public String getLinkName() {
        return linkName;
    }
    /**
     * @param linkName the linkName to set
     */
    public void setLinkName(String linkName) {
        this.linkName = linkName;
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
     * @return the type
     */
    public String getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
    
    
}
