package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

/**
 * @author Fengyuan Hu
 */
public class ApiJavaController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        request.setAttribute("path", "WEB-INF/lib/");
        request.setAttribute("fileName", "java-intermine-webservice-client.zip");
        // for jar - application/java-archive or application/x-jar
        request.setAttribute("mimeType", "application/zip");
        // for jar - jar
        request.setAttribute("mimeExtension", "zip");
        request.setAttribute("baseURL", new URLGenerator(request).getPermanentBaseURL());

        // Find project title and make a Javanised package name
        Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                .getServletContext());
        String projectTitle = webProperties.getProperty("project.title");
        String javasieProjectTitle = TypeUtil.javaisePackageName(projectTitle);
        request.setAttribute("javasieProjectTitle", javasieProjectTitle);

        return null;

    }
}
