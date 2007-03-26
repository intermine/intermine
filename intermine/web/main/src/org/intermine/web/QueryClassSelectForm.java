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

import org.apache.struts.action.ActionForm;

/**
 * Form bean for selection of a new query class.
 *
 * @author Richard Smith
 */
public class QueryClassSelectForm extends ActionForm
{
    protected String className;

    /**
     * Set the class name
     *
     * @param className the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the class name
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }
}
