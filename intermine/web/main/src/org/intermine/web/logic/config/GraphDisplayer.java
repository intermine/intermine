package org.intermine.web.logic.config;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Configuration object describing details of a graph displayer
 *
 * @author Xavier Watkins
 */
public class GraphDisplayer
{
    private String title;
    private String domainLabel;
    private String rangeLabel;
    private String dataSetLoader;
    private String toolTipGen;
    private String urlGen;
    private String description;
    
    
    
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }


    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * Get the value of dataSetLoader
     * @return the value of dataSetLoader
     */
    public String getDataSetLoader() {
        return dataSetLoader;
    }
    
    
    /**
     * Set the value of dataSetLoader
     * @param dataSetLoader a String
     */
    public void setDataSetLoader(String dataSetLoader) {
        this.dataSetLoader = dataSetLoader;
    }
    
    
    /**
     * Get the domainLabel
     * @return the domainLabel
     */
    public String getDomainLabel() {
        return domainLabel;
    }
    
    
    /**
     * Set the value of domainLabel
     * @param domainLabel a String
     */
    public void setDomainLabel(String domainLabel) {
        this.domainLabel = domainLabel;
    }
    
    
    /**
     * Get the value of rangeLabel
     * @return the rangeLabel
     */
    public String getRangeLabel() {
        return rangeLabel;
    }
    
    
    /**
     * Set the value of rangeLabel
     * @param rangeLabel a String
     */
    public void setRangeLabel(String rangeLabel) {
        this.rangeLabel = rangeLabel;
    }
    
    
    /**
     * get the title
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    
    /**
     * Set the value of title
     * @param title a String
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    
    /**
     * Get the value of toolTipGen
     * @return the toolTipGen
     */
    public String getToolTipGen() {
        return toolTipGen;
    }
    
    
    /**
     * Set the value of toolTipGen
     * @param toolTipGen a String
     */
    public void setToolTipGen(String toolTipGen) {
        this.toolTipGen = toolTipGen;
    }
    
    
    /**
     * Get the value of urlGen
     * @return the value of urlGen
     */
    public String getUrlGen() {
        return urlGen;
    }
    
    
    /**
     * Set the value of urlGen
     * @param urlGen a String
     */
    public void setUrlGen(String urlGen) {
        this.urlGen = urlGen;
    }

    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + title + " domainLabel=\"" + domainLabel + " rangeLabel=\""
               + rangeLabel + " dataSetLoader=\"" + dataSetLoader + " toolTipGen=\"" + toolTipGen
               + " urlGen=\"" + urlGen + "\" description=\"" + description + "\"/>";
    }

}
