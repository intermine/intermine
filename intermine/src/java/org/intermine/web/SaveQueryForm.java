package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.struts.action.ActionForm;

/**
 * Form bean to represent the inputs to the query saving action
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class SaveQueryForm extends ActionForm
{
    protected String queryName = "";

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
     * @see ActionForm#reset
     */
    public void reset() {
        queryName = "";
    }
}
