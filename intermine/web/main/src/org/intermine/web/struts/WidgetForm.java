package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.intermine.metadata.StringUtil;

/**
 *
 * @author Julie Sullivan
 */
public class WidgetForm extends ActionForm
{

    private String link;
    private String bagName;
    private String[] selected;
    private String bagType;
    private String action;
    private String widgetid;
    private String exporttype;
    private String selectedExtraAttribute;
    private String errorCorrection;
    private String max;
    private String highlight;
    private String pValue;
    private String numberOpt;
    private String widgetTitle;

    /**
     * returns the value of the checkboxes checked by the user on the form.
     * @return the selectedAsString
     */
    public String getSelectedAsString() {
        Collection<String> c = Arrays.asList(selected);
        return StringUtil.join(c, ",");
    }

    /**
     * @return the bagType
     */
    public String getBagType() {
        return bagType;
    }

    /**
     * @param bagType the bagType to set
     */
    public void setBagType(String bagType) {
        this.bagType = bagType;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * @return the pValue
     */
    public String getPValue() {
        return pValue;
    }

    /**
     * @param pValue the pValue to set
     */
    public void setPValue(String pValue) {
        this.pValue = pValue;
    }

    /**
     * @return the numberOpt
     */
    public String getNumberOpt() {
        return numberOpt;
    }

    /**
     * @param numberOpt the pValue to set
     */
    public void setNumberOpt(String numberOpt) {
        this.numberOpt = numberOpt;
    }

    /**
     * Constructor
     */
    public WidgetForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        link = "";
        bagName = "";
        selected = new String[0];
        selectedExtraAttribute = "";
    }

    /**
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * name of bag that this widget is using.  we need both the bag and the bagname because
     * sometimes we don't have the bag object.
     * @param bagName the bagName to set
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * @return the selected go terms
     */
    public String[] getSelected() {
        return selected;
    }

    /**
     * @param selected the selected  terms
     */
    public void setSelected(String[] selected) {
        this.selected = selected;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the widgetid
     */
    public String getWidgetid() {
        return widgetid;
    }

    /**
     * @param widgetid the widgetid to set
     */
    public void setWidgetid(String widgetid) {
        this.widgetid = widgetid;
    }

    /**
     * @return the exporttype
     */
    public String getExporttype() {
        return exporttype;
    }

    /**
     * @param exporttype the exporttype to set
     */
    public void setExporttype(String exporttype) {
        this.exporttype = exporttype;
    }

    /**
     * @return the selectedExtraAttribute
     */
    public String getSelectedExtraAttribute() {
        return selectedExtraAttribute;
    }

    /**
     * @param selectedExtraAttribute the selectedExtraAttribute to set
     */
    public void setSelectedExtraAttribute(String selectedExtraAttribute) {
        this.selectedExtraAttribute = selectedExtraAttribute;
    }

    /**
     * @return the errorCorrection
     */
    public String getErrorCorrection() {
        return errorCorrection;
    }

    /**
     * @param errorCorrection the errorCorrection to set
     */
    public void setErrorCorrection(String errorCorrection) {
        this.errorCorrection = errorCorrection;
    }

    /**
     * @return the max
     */
    public String getMax() {
        return max;
    }

    /**
     * @return the highlight
     */
    public String getHighlight() {
        return highlight;
    }

    /**
     * @param highlight the highlight to set
     */
    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    /**
     * @param max the max to set
     */
    public void setMax(String max) {
        this.max = max;
    }

    /**
     * used on results page for description: "results for widget XYZ"
     * @param widgetTitle title of the widget
     */
    public void setWidgetTitle(String widgetTitle) {
        this.widgetTitle = widgetTitle;
    }

    /**
    * used on results page for description: "results for widget XYZ"
    * @return title of widget
    */
    public String getWidgetTitle() {
        return widgetTitle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
    }
}
