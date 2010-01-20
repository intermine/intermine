package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;


/**
 * Form to handle input from the template page
 * @author Mark Woodbridge
 */
public class TemplateForm extends ActionForm
{
    private Map<String, Object> attributeOps;
    private Map<String, Object> attributeValues;
    private Map<String, Boolean> useBagConstraint;
    private Map<String, Object> extraValues, selectedBags;
    private Map<String, String> bagOps;
    private String type, name, view;

    /**
     * Constructor
     */
    public TemplateForm() {
        super();
        reset();
    }

    /**
     * Set the attribute ops
     * @param attributeOps the attribute ops
     */
    public void setAttributeOps(Map<String, Object> attributeOps) {
        this.attributeOps = attributeOps;
    }

    /**
     * Get the attribute ops
     * @return the attribute ops
     */
    public Map<String, Object> getAttributeOps() {
        return attributeOps;
    }

    /**
     * Set an attribute op
     * @param key the key
     * @param value the value
     */
    public void setAttributeOps(String key, String value) {
        attributeOps.put(key, value);
    }

    /**
     * Get an attribute op
     * @param key the key
     * @return the value
     */
    public Object getAttributeOps(String key)  {
        return attributeOps.get(key);
    }

    /**
     * Set the attribute values
     * @param attributeValues the attribute values
     */
    public void setAttributeValues(Map<String, Object> attributeValues) {
        this.attributeValues = attributeValues;
    }

    /**
     * Get the attribute values
     * @return the attribute values
     */
    public Map<String, Object> getAttributeValues() {
        return attributeValues;
    }

    /**
     * Set an attribute value
     * @param key the key
     * @param value the value
     */
    public void setAttributeValues(String key, Object value) {
        attributeValues.put(key, value);
    }

    /**
     * Get an attribute value
     * @param key the key
     * @return the value
     */
    public Object getAttributeValues(String key)  {
        return attributeValues.get(key);
    }

    /**
     * Sets the extra values
     * @param extraValues the extra values
     */
    public void setExtraValues(Map<String, Object> extraValues) {
        this.extraValues = extraValues;
    }

    /**
     * Get the extra values
     * @return the extra values
     */
    public Map<String, Object> getExtraValues() {
        return extraValues;
    }

    /**
     * Set an extra value
     * @param key the key
     * @param value the value
     */
    public void setExtraValues(String key, Object value) {
        extraValues.put(key, value);
    }

    /**
     * Get an extra value
     * @param key the key
     * @return the value
     */
    public Object getExtraValues(String key) {
        return extraValues.get(key);
    }

    /**
     * Set value of useBagConstraint for given constraint key.
     * @param key the key
     * @param value the value
     */
    public void setUseBagConstraint(String key, boolean value) {
        useBagConstraint.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Get the value of useBagConstraint for given constraint key.
     * @param key the key
     * @return the value
     */
    public boolean getUseBagConstraint(String key) {
        return Boolean.TRUE.equals(useBagConstraint.get(key));
    }

    /**
     * Set the bag name.
     * @param key the key
     * @param bag bag name
     */
    public void setBag(String key, Object bag) {
        selectedBags.put(key, bag);
    }

    /**
     * Get the bag name selected.
     * @param key the key
     * @return the bag selected
     */
    public Object getBag(String key) {
        return selectedBags.get(key);
    }

    /**
     * Get the bag operation selected.
     * @param key the key
     * @return the bag operation selected
     */
    public String getBagOp(String key) {
        return bagOps.get(key);
    }

    /**
     * Set bag operation.
     * @param bagOp the bag operation selected
     * @param key the key
     */
    public void setBagOp(String key, String bagOp) {
        bagOps.put(key, bagOp);
    }

    /**
     * Get the template name.
     * @return the template name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the template name.
     * @param templateName the template name
     */
    public void setName(String templateName) {
        this.name = templateName;
    }

    /**
     * Get the selected alternative view name.
     * @return selected alternative view name
     */
    public String getView() {
        return view;
    }

    /**
     * Set the selected alternative view name.
     * @param view selected alternative view name
     */
    public void setView(String view) {
        this.view = view;
    }

    /**
     * Get the template type.
     * @return the template type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the template type.
     * @param templateType the template type
     */
    public void setType(String templateType) {
        this.type = templateType;
    }

    /**
     * {@inheritDoc}

     */
    public void reset(@SuppressWarnings("unused") ActionMapping mapping,
                      @SuppressWarnings("unused") HttpServletRequest request) {
        reset();
    }

    /**
     * Reset the form
     */
    protected void reset() {
        attributeOps = new HashMap<String, Object>();
        attributeValues = new HashMap<String, Object>();
        useBagConstraint = new HashMap<String, Boolean>();
        selectedBags = new HashMap<String, Object>();
        bagOps = new HashMap<String, String>();
        extraValues = new HashMap<String, Object>();
        name = null;
        type = null;
        view = "";
    }
}
