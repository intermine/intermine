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

import org.apache.struts.action.ActionForm;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class QueryAliasSelectForm extends ActionForm
{

    protected String alias;

    /**
     * Set the alias name of class already in query
     *
     * @param alias an alias for a QueryClass already on form
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Get the class name
     *
     * @return the query string
     */
    public String getAlias() {
        return alias;
    }

}
