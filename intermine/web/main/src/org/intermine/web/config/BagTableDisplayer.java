package org.intermine.web.config;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Xavier Watkins
 *
 */
public class BagTableDisplayer
{
    private String title;
    private String type;
    private String collectionName;
    private String fields;
    private String description;

    /**
     * Get the type
     * @return the type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the type
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
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
     * Get the collection name
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Set the collectionName
     * @param collectionName the collectionName
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * @return the fields
     */
    public String getFields() {
        return fields;
    }

    /**
     * @param fields the fields to set
     */
    public void setFields(String fields) {
        this.fields = fields;
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
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + title + "\" type=\"" + type + "\" collectionName=\""
               + collectionName + "\"/>";
    }
}
