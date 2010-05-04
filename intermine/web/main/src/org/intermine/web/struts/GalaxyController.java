package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

/**
 * Generates the link to send data to Galaxy
 * @author Xavier Watkins
 *
 */
public class GalaxyController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        // TODO the query will be NULL if the query just ran isn't on the session, eg. a quicksearch
        // This will not be true once we have a query registry.
        if (query == null) {
            return null;
        }
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        String queryXML = PathQueryBinding.marshal(query, "tmpName", model.getName(),
                                                   PathQuery.USERPROFILE_VERSION);
        String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");
        StringBuffer stringUrl = new StringBuffer(new URLGenerator(request).getPermanentBaseURL()
                + "/service/query/results?query=" + encodedQueryXML + "&size=1000000");
        request.setAttribute("urlSendBack", stringUrl.toString());
        return null;
    }
}
