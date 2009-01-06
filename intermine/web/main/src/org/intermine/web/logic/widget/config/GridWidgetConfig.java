package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.GridWidget;

/**
 * @author Dominik Grimm
 */
public class GridWidgetConfig extends WidgetConfig
{
    private String gridLabel;
    private String externalLink, externalLinkLabel;
    private String filters, editable, width;
    
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
    /**
     * @return the label
     */
    public String getgridLabel() {
        return gridLabel;
    }

    /**
     * @param label the label to set
     */
    public void setgridLabel(String label) {
        this.gridLabel = label;
    }

    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + "\" gridLabel=\"" + gridLabel + "\" dataSetLoader=\""
               + getDataSetLoader() + "\" link=\"" + getLink() + "\"/>";
    }
    /**
     * {@inheritDoc}
     */
    public String geteditable() {
        return editable;
    }

    /**
     * @param editable editable
     */
    public void seteditable(String editable) {
        this.editable = editable;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getwidth() {
        return width;
    }

    /**
     * @param width width
     */
    public void setwidth(String width) {
        this.width = width;
    }
  
    /**
     * {@inheritDoc}
     */
    public Map<String, Collection<String>> getExtraAttributes(@SuppressWarnings("unused")
                                                              InterMineBag imBag,
                                                              @SuppressWarnings("unused")
                                                              ObjectStore os) {
        Map<String, Collection<String>> returnMap = new HashMap<String, Collection<String>>();
        returnMap.put("pValue", Arrays.asList(getFilters().split(",")));
        if (editable != null && editable.equals("true")) {
            returnMap.put("Editable", new ArrayList<String>());
        }
        if (width != null) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(getwidth());
            returnMap.put("Width", tmp);
        }
        return returnMap;
    }
    /**
     * @return the externalLinkLabel
     */
    public String getExternalLinkLabel() {
        return externalLinkLabel;
    }

    /**
     * @param externalLinkLabel the externalLinkLabel to set
     */
    public void setExternalLinkLabel(String externalLinkLabel) {
        this.externalLinkLabel = externalLinkLabel;
    }

    /**
     * {@inheritDoc}
     */
    public GridWidget getWidget(InterMineBag imBag, ObjectStore os,
                                      List<String> selectedExtraAttribute) {
        return new GridWidget(this, imBag, os, selectedExtraAttribute.get(0),
                selectedExtraAttribute.get(3), selectedExtraAttribute.get(4),
                selectedExtraAttribute.get(5));
    }
    /**
     * {@inheritDoc}
     */
    public String getExternalLink() {
        // TODO Auto-generated method stub
        return externalLink;
    }
    /**
     * {@inheritDoc}
     */
    public void setExternalLink(String externalLink) {
        // TODO Auto-generated method stub
        this.externalLink = externalLink;
    }

}