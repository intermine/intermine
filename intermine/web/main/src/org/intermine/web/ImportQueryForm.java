package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

/**
 * Form bean representing query import form.
 *
 * @author  Thomas Riley
 */
public class ImportQueryForm extends ValidatorForm
{
    private String xml;
    
    /**
     * Creates a new instance of ImportQueryForm.
     */
    public ImportQueryForm() {
        reset();
    }
    
    /**
     * Get the xml.
     * @return query in xml format
     */
    public String getXml() {
        return xml;
    }

    /**
     * Set the xml.
     * @param xml query in xml format
     */
    public void setXml(String xml) {
        this.xml = xml;
    }
    
    /**
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }
    
    /**
     * Reset the form.
     */
    protected void reset() {
        xml = "";
    }

    /**
     * Call inherited method then check whether xml is valid.
     *
     * @see ValidatorForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (errors != null && errors.size() > 0) {
            return errors;
        }
        try {
           Map map = PathQueryBinding.unmarshal(new StringReader(getXml()));
           if (map.size() != 1) {
               if (errors == null) {
                   errors = new ActionErrors();
               }
               errors.add(ActionErrors.GLOBAL_MESSAGE,
                           new ActionMessage("errors.importQuery.notone"));
           }
        } catch (Exception err) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                        new ActionMessage("errors.badqueryxml", err.getMessage()));
        }
        return errors;
    }
}
