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

import java.net.URLEncoder;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.util.URLGenerator;

/**
 * Generates the link to send data to Galaxy
 * @author Xavier Watkins
 *
 */
public class GalaxyController extends TilesAction
{

    /**
     * Initialise attributes for the bagUploadConfirmIssue.
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String queryXML = PathQueryBinding.marshal(query, "tmpName", os.getModel().getName());
        String encodedQueryXML = URLEncoder.encode(queryXML, "UTF-8");
        Properties webProperties = InterMineAction.getWebProperties(request);
        StringBuffer stringUrl = new StringBuffer(webProperties.getProperty("project.sitePrefix") 
                        + new URLGenerator(request).getPermanentBaseURL() 
                        + "/service/query/results?query=" 
                        + encodedQueryXML 
                        + "&size=1000000");
        request.setAttribute("urlSendBack", stringUrl.toString());
        return null;
    }

}
