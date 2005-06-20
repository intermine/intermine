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

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.web.dataset.DataSet;

/**
 * Contoller for a single data set tile embedded in a page. Expects the request parameter
 * "name" to refer to a data set name. Places a reference to the corresponding DataSet object
 * in the tile ComponentContext.
 * 
 * @author Thomas Riley
 */
public class DataSetController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(DataSetController.class);
    
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map dataSets = (Map) servletContext.getAttribute(Constants.DATASETS);
        DataSet set = (DataSet) dataSets.get(request.getParameter("name"));
        if (set == null) {
            LOG.error("no such data set: " + request.getParameter("name"));
            return null;
        }
        context.putAttribute("dataSet", set);
        return null;
    }
}
