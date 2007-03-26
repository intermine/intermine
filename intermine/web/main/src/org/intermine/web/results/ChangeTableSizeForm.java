package org.intermine.web.results;

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
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.SessionMethods;

/**
 * Form bean for changing the page size.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class ChangeTableSizeForm extends ActionForm
{
    /**  */
    protected String pageSize;

    /**
     * Constructor
     */
    public ChangeTableSizeForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {
        pageSize = "10";
    }

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

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
        HttpSession session = request.getSession();
        //PagedTable pr = (PagedTable) request.getSession().getAttribute(Constants.RESULTS_TABLE);
        PagedTable pr = SessionMethods.getResultsTable(session, request.getParameter("table"));
        if (pr != null) {
            pageSize = "" + pr.getPageSize();
        }
    }
}
