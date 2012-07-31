package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * @author Richard Smith
 *
 */
public class ModMineSearchForm extends ActionForm
{

    private static final long serialVersionUID = 1L;

    private String searchTerm;

    /**
     *
     */
    public ModMineSearchForm() {
        reset();
    }

    /**
     * @return the search term
     */
    public String getSearchTerm() {
        return searchTerm;
    }

    /**
     * @param searchTerm the value to set
     */
    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
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
        searchTerm = "";
    }

}

