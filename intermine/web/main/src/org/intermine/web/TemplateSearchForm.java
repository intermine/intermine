package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
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
 * Form bean representing feedback form.
 *
 * @author  Thomas Riley
 */
public class TemplateSearchForm extends ActionForm
{
    private String queryString;
    private String type;
    
    /** Creates a new instance of FeedbackForm */
    public TemplateSearchForm() {
        reset();
    }
    
    /**
     * @return the queryString.
     */
    public String getQueryString() {
        return queryString;
    }
    
    /**
     * @param queryString the queryString to set
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
    
    /**
     * @return the template type
     */
    public String getType() {
        return type;
    }
    
    /**
     * @param type the template type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Reset form bean.
     *
     * @param mapping  the action mapping associated with this form bean
     * @param request  the current http servlet request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }
    
    private void reset() {
        queryString = "";
        type = "global";
    }
}
