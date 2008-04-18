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

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.FieldConfig;
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

    private String fields;
    private WebConfig webConfig;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagTableWidgetLoader bagWidgLdr;
    private String pathStrings, externalLink, externalLinkLabel;
    private String columnTitle = null;
    
    /**
     * {@inheritDoc}
     */
    public void process(InterMineBag bag, ObjectStore os) throws Exception {
        bagWidgLdr = new BagTableWidgetLoader(pathStrings, bag, os, webConfig,
                        os.getModel(), classKeys, fields, getLink(), getColumnTitle(),
                        getExternalLink());
    }
    
    /**
     * Get the flattened results
     * @return the List of flattened results
     */
    public List getFlattenedResults() {
        return bagWidgLdr.getFlattenedResults();
    }
    
    /**
     * Get the columns
     * @return the columns
     */
    public List getColumns() {
        return bagWidgLdr.getColumns();
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
     * @return the totle for the count column
     */
    public String getColumnTitle() {
        return columnTitle;
    }

    /**
     * @param columnTitle set title for count column
     */
    public void setColumnTitle(String columnTitle) {
        this.columnTitle = columnTitle;
    }
    
    /**
     * Do-nothing implementation of superclass method
     * @param imBag a bag
     * @param os the objectstore
     * @return null
     */
    public Map<String, Collection> getExtraAttributes(InterMineBag imBag,
                                                      ObjectStore os) {
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
    public Map<String, List<FieldDescriptor>> getClassKeys() {
        return classKeys;
    }

    /**
     * @param classKeys the classKeys to set
     */
    public void setClassKeys(Map<String, List<FieldDescriptor>> classKeys) {
        this.classKeys = classKeys;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean getHasResults() {
        return (bagWidgLdr.getFlattenedResults().size() > 0);
    }

    /**
     * Comma separated list of path strings to appear in the widget, ie Employee.firstName, 
     * Employee.lastName
     * @return the pathStrings
     */
    public String getPathStrings() {
        return pathStrings;
    }

    /**
     * @param pathStrings the pathString to set
     */
    public void setPathStrings(String pathStrings) {
        this.pathStrings = pathStrings;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean getToggleOn() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    /**
     * {@inheritDoc}
     */
    public String getExternalLinkLabel() {
        return externalLinkLabel;
    }

    /**
    * {@inheritDoc}
     */
    public void setExternalLinkLabel(String externalLinkLabel) {
        this.externalLinkLabel = externalLinkLabel;
    }
    
}
