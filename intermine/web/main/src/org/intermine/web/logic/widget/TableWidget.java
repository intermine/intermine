package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;

/*
 * Copyright (C) 2002-2008 FlyMine
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
public class TableWidget extends Widget
{
    private String type;
    private String collectionName;
    private String fields;
    private WebConfig webConfig;
    private Map classKeys;
    private BagTableWidgetLoader bagWidgLdr;
    
    public void process(InterMineBag bag, ObjectStore os) {
        bagWidgLdr = new BagTableWidgetLoader(getTitle(),
                        getDescription(), type, collectionName, bag, os, webConfig,
                        os.getModel(), classKeys, fields, getLink());
    }
    
    public List getFlattenedResults() {
        return bagWidgLdr.getFlattenedResults();
    }
    
    public List getColumns() {
        return bagWidgLdr.getColumns();
    }
    
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
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + "\" type=\"" + type + "\" collectionName=\""
               + collectionName + "\"/>";
    }
    
    public Collection getExtraAttributes(InterMineBag imBag, ObjectStore os) {
        return null;
    }

    /**
     * @return the webConfig
     */
    public WebConfig getWebConfig() {
        return webConfig;
    }

    /**
     * @param webConfig the webConfig to set
     */
    public void setWebConfig(WebConfig webConfig) {
        this.webConfig = webConfig;
    }

    /**
     * @return the classKeys
     */
    public Map getClassKeys() {
        return classKeys;
    }

    /**
     * @param classKeys the classKeys to set
     */
    public void setClassKeys(Map classKeys) {
        this.classKeys = classKeys;
    }

}
