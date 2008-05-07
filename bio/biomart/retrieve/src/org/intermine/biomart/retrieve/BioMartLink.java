package org.intermine.biomart.retrieve;

public class BioMartLink
{
    private String internalName;
    private String linkName;
    private String name;
    private String filters;
    public BioMartLink() {
        super();
        // TODO Auto-generated constructor stub
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
     * @return the filters
     */
    public String getFilters() {
        return filters;
    }
    /**
     * @param filters the filters to set
     */
    public void setFilters(String filters) {
        this.filters = filters;
    }
    
    
    
}
