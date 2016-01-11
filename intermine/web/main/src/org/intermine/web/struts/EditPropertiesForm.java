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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * An overly verbose way of passing parameters back to the controller.
 * @author Alex Kalderimis
 *
 */
public class EditPropertiesForm extends ActionForm
{
    /**
     * For the serialisation.
     */
    private static final long serialVersionUID = -4283098307719157909L;
    private String propertyName = null;
    private String propertyValue = null;

    /**
     * @return The property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Set the property name. I cannot believe I have to write this pointless doc.
     * @param propertyName The. Name. Of. The. Property.
     **/
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /** @return the value of the property. **/
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * Set the value of the property
     * @param propertyValue The. Value. Of. The. Property.
     **/
    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    @Override
    public void reset(
            ActionMapping mapping,
            HttpServletRequest request) {
        propertyName = null;
        propertyValue = null;
    }
}
