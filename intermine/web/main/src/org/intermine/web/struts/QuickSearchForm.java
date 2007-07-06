package org.intermine.web.struts;

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
 * @author Xavier Watkins
 *
 */
public class QuickSearchForm extends ActionForm
{

    private String value;
    private String quickSearchType;
    
    /**
     * 
     */
    public QuickSearchForm() {
        reset();
    }
    
   
    /**
     * @return the quickSearchType
     */
    public String getQuickSearchType() {
        return quickSearchType;
    }


    /**
     * @param quickSearchType the quickSearchType to set
     */
    public void setQuickSearchType(String quickSearchType) {
        this.quickSearchType = quickSearchType;
    }


    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }


    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }


    /**
     * Reset form bean.
     *
     * @param mapping  the action mapping associated with this form bean
     * @param request  the current http servlet request
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }
    
    /**
     * 
     */
    public void reset() {
        value = "";
        quickSearchType = "";
    }

}
