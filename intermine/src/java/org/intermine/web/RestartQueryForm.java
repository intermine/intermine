package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
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
 * Form bean to represent the inputs to the query replacement action
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class RestartQueryForm extends ActionForm
{

    protected String queryName = "empty";

    /**
     * Set the query name
     *
     * @param queryName the query name
     */
    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    /**
     * Get the query name
     *
     * @return the query name
     */
    public String getQueryName() {
        return queryName;
    }

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        queryName = "empty";
    }
}
