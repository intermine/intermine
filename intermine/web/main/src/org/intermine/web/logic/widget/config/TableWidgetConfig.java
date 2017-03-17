package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2016 FlyMine
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

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.widget.TableWidget;
import org.intermine.web.logic.widget.WidgetOptions;

/*
 * Copyright (C) 2002-2016 FlyMine
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
public class TableWidgetConfig extends WidgetConfig
{

    private String displayFields, exportField;
    private WebConfig webConfig;
    private Map<String, List<FieldDescriptor>> classKeys;
    private String pathStrings, externalLink, externalLinkLabel;
    private String columnTitle = null;


    /**
     * @return the fields
     */
    public String getDisplayFields() {
        return displayFields;
    }

    /**
     * @param fields the fields to set
     */
    public void setDisplayFields(String fields) {
        this.displayFields = fields;
    }



    /**
     * @return the title for the count column
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
    public Map<String, Collection<String>> getExtraAttributes(InterMineBag imBag,
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
     * @param classKeys the classKeys to set
     */
    public void setClassKeys(Map<String, List<FieldDescriptor>> classKeys) {
        this.classKeys = classKeys;
    }

    /**
     * Get the classKeys
     * @return the class keys
     */
    public Map<String, List<FieldDescriptor>> getClassKeys() {
        return classKeys;
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

    /**
     * @return the exportField
     */
    public String getExportField() {
        return exportField;
    }

    /**
     * @param exportField the exportField to set
     */
    public void setExportField(String exportField) {
        this.exportField = exportField;
    }

    /**
     * {@inheritDoc}
     */
    public TableWidget getWidget(
            InterMineBag imBag,
            InterMineBag populationBag,
            ObjectStore os,
            WidgetOptions options,
            String ids, String populationIds) {
        return new TableWidget(this, imBag, os, ids);
    }

}
