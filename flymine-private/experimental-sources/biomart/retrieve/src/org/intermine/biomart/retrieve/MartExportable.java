package org.intermine.biomart.retrieve;

import java.util.List;

public class MartExportable
{
    private String internalName;
    public List attributes;
    public String linkVersion;
    private String linkName;
    private String name;

    public MartExportable() {
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
     * @return the attributes
     */
    public List getAttributes() {
        return attributes;
    }
    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(List attributes) {
        this.attributes = attributes;
    }
    /**
     * @return the linkVersion
     */
    public String getLinkVersion() {
        return linkVersion;
    }
    /**
     * @param linkVersion the linkVersion to set
     */
    public void setLinkVersion(String linkVersion) {
        this.linkVersion = linkVersion;
    }
    
}
