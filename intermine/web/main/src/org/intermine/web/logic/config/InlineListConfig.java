package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Represents an inline list to be shown on a Report page
 * @author radek
 *
 */
public class InlineListConfig
{

    private String path = null;
    private Boolean showLinksToObjects = false;
    private Boolean showInHeader = false;
    private Integer lineLength = null;

    /**
     * Path set from WebConfig, ie "probeSets.primaryIdentifier"
     * @param path String
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Shall we show links to the objects's report pages?
     * @param showLinksToObjects set from WebConfig
     */
    public void setShowLinksToObjects(Boolean showLinksToObjects) {
        this.showLinksToObjects = showLinksToObjects;
    }

    /**
     * Show this list in the header?
     * @param showInHeader Passed from WebConfig
     */
    public void setShowInHeader(Boolean showInHeader) {
        this.showInHeader = showInHeader;
    }

    /**
     * Sets the amount of entries to show based on their total length
     * @see the number is approximate as we do not break inside the text
     * @param lineLength total character length (spaces, commas included!)
     */
    public void setLineLength(Integer lineLength) {
        this.lineLength = lineLength;
    }

    /**
     *
     * @see our JavaScript (jQuery) expects non set values to be "0"
     * @return total character length (spaces, commas included) to show
     */
    public Integer getLineLength() {
        return (lineLength != null) ? lineLength : 0;
    }

    /**
     *
     * @return String path so that ReportObject can resolve the actual Objects
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @return are we to show links to the objects'? report page? the JSP asks...
     */
    public Boolean getShowLinksToObjects() {
        return showLinksToObjects;
    }

    /**
     *
     * @return are we to show this inline list in the header of the report page?
     */
    public Boolean getShowInHeader() {
        return (showInHeader);
    }

}
