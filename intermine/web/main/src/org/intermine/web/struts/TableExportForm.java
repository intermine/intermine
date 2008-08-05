package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form to hold options for exporting.
 * @author Kim Rutherford
 */
public class TableExportForm extends ActionForm
{
    private Map<String, Object> extraParams = new HashMap<String, Object>();

    // type of export, "tab", "excel", etc.
    private String type = null;

    private String pathsString = null;

    // PagedTable identifier
    private String table = null;

    /**
     * Constructor
     */
    public TableExportForm() {
        initialise();
    }

    /**
     * Get the paths string in the format: "Class.field Class.field2 Class.ref.field"
     * @return the pathsString
     */
    public final String getPathsString() {
        return pathsString;
    }

    /**
     * Set the paths string
     * @param pathsString the pathsString to set
     */
    public final void setPathsString(String pathsString) {
        this.pathsString = pathsString;
    }

    /**
     * Return the export type: "tab", "excel", etc.
     * @return the type
     */
    public final String getType() {
        return type;
    }

    /**
     * Set the type
     * @param type the type to set
     */
    public final void setType(String type) {
        this.type = type;
    }

    /**
     * Get the table identifier to look up the PagedTable in the servlet context
     * @return the table
     */
    public final String getTable() {
        return table;
    }

    /**
     * Set the table identifier to look up in the servlet context
     * @param table the table to set
     */
    public final void setTable(String table) {
        this.table = table;
    }
    /**
     * Extra parameters map for use by sub classes.  The Map is reset when the form is initialised.
     * @return the extraParams
     */
    protected final Map<String, Object> getExtraParams() {
        return extraParams;
    }

    /**
     * Initialiser
     */
    public void initialise() {
        extraParams = new HashMap<String, Object>();
    }

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    @Override
    public void reset(@SuppressWarnings("unused") ActionMapping mapping,
                      @SuppressWarnings("unused") HttpServletRequest request) {
        initialise();
    }
}
