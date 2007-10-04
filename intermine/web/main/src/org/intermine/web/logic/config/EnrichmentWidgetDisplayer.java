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
 * @author Julie Sullivan
 */
public class EnrichmentWidgetDisplayer
{
    private String title;
    private String link;
    private String controller;
    private String description;
    private String max;
    private String filters;
    private String filterLabel;
    private String externalLink;
    private String append;
    
    /**
     * Get the link
     * @return the link
     */
    public String getLink() {
        return link;
    }
    
    /**
     * Set the link
     * @param link the link
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Get the title
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the controller
     */
    public String getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    public void setController(String controller) {
        this.controller = controller;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * @return the filters
     */
    public String getFilters() {
        return filters;
    }

    /**
     * @param filters
     */
    public void setFilters(String filters) {
        this.filters = filters;
    }
    
    /**
     * @return the label for the filters
     */
    public String getFilterLabel() {
        return filterLabel;
    }

    /**
     * @param filterLabel the label for the filters
     */
    public void setFilterLabel(String filterLabel) {
        this.filterLabel = filterLabel;
    }
    
    /**
     * @return the maximum value this widget will display
     */
    public String getMax() {
        return max;
    }

    /**
     * @param max maximum value this widget will display
     */
    public void setMax(String max) {
        this.max = max;
    }
    
    /**
     * Get the link
     * @return the link
     */
    public String getExternalLink() {
        return externalLink;
    }
    
    /**
     * Set the link
     * @param externalLink the external link
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }
    
    /**
     * @return the string to append to the end of the external link
     */
    public String getAppend() {
        return append;
    }
    
    /**
     * @param append the string to append to the end of the link
     */
    public void setAppend(String append) {
        this.append = append;
    }
    
    
    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + title + "\" link=\"" + link + "\" controller=\""
               + controller + "\"/>";
    }
}
