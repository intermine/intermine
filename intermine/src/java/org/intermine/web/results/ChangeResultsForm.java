package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class ChangeResultsForm extends ActionForm
{

    protected String pageSize = "10";
    protected String[] selectedObjects = {};

    /**
     * Set the page size
     *
     * @param pageSize the page size to display
     */
    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get the page size
     *
     * @return the page size
     */
    public String getPageSize() {
        return pageSize;
    }


    public void setSelectedObjects(String[] selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    public String[] getSelectedObjects() {
        return selectedObjects;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        selectedObjects = new String[] {};
    }

}
