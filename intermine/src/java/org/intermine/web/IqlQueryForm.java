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

import org.apache.struts.action.ActionForm;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class IqlQueryForm extends ActionForm
{

    protected String querystring;

    /**
     * Set the query string
     *
     * @param querystring the query string
     */
    public void setQuerystring(String querystring) {
        this.querystring = querystring;
    }

    /**
     * Get the query string
     *
     * @return the query string
     */
    public String getQuerystring() {
        return querystring;
    }

}
