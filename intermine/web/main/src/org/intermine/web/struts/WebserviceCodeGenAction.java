package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.codegen.WebserviceCodeGenInfo;
import org.intermine.api.query.codegen.WebserviceCodeGenerator;
import org.intermine.api.query.codegen.WebserviceJavaCodeGenerator;
import org.intermine.api.query.codegen.WebservicePerlCodeGenerator;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

/**
 * Action to handle the web service code generation.
 * Multiple-query is not supported.
 *
 * @author Fengyuan Hu
 */
public class WebserviceCodeGenAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(WebserviceCodeGenAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        // Ref to OrthologueLinkController and OrthologueLinkManager
        Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                .getServletContext());

        String serviceBaseURL = new URLGenerator(request).getPermanentBaseURL();
        // set in project properties
        String projectTitle = webProperties.getProperty("project.title");
        // set in global.web.properties
        String perlWSModuleVer = webProperties.getProperty("perl.wsModuleVer");

        try {
            String method = request.getParameter("method");
            String source = request.getParameter("source");

            if ("perl".equals(method)) {
                WebserviceCodeGenerator wsPerlCG = new WebservicePerlCodeGenerator();

                if ("templateQuery".equals(source)) {
                    String sc = wsPerlCG.generate(setWebserviceCodeGenInfo(
                            getTemplateQuery(im, profile, request, session),
                            serviceBaseURL, projectTitle, perlWSModuleVer));
                    output(sc, method, source, response);
                } else if ("pathQuery".equals(source)) {
                    String sc = wsPerlCG
                            .generate(setWebserviceCodeGenInfo(
                                    getPathQuery(session), serviceBaseURL,
                                    projectTitle, perlWSModuleVer));
                    output(sc, method, source, response);
                }
            } else if ("java".equals(method)) {
                WebserviceCodeGenerator wsJavaCG = new WebserviceJavaCodeGenerator();

                if ("templateQuery".equals(source)) {
                    String sc = wsJavaCG.generate(setWebserviceCodeGenInfo(
                            getTemplateQuery(im, profile, request, session),
                            serviceBaseURL, projectTitle, null));
                    output(sc, method, source, response);
                } else if ("pathQuery".equals(source)) {
                    String sc = wsJavaCG
                            .generate(setWebserviceCodeGenInfo(
                                    getPathQuery(session), serviceBaseURL,
                                    projectTitle, null));
                    output(sc, method, source, response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            output(e.toString(), "exception", "e", response);
            return mapping.findForward("begin");
        }

        return null;
    }

    /**
     * Method called to get the template query.
     * @param im InterMineAPI object
     * @param profile Profile object
     * @param request HttpServletRequest object
     * @param session HttpSession object
     * @return PathQuery object
     */
    private PathQuery getTemplateQuery(InterMineAPI im, Profile profile,
            HttpServletRequest request, HttpSession session) {

        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String originalTemplate = request.getParameter("originalTemplate");

        TemplateManager templateManager = im.getTemplateManager();
        if (name == null) {
            throw new IllegalArgumentException("Cannot find a template in context "
                                                   + scope);
        } else {
            TemplateQuery template = (originalTemplate != null)
                                     ? templateManager.getTemplate(profile, name, scope)
                                     : (TemplateQuery) SessionMethods.getQuery(session);
            if (template != null) {
                // User's template, convert to PathQuery
                if (!im.getTagManager().getObjectTagNames(template.getName(),
                                TagTypes.TEMPLATE, profile.getUsername())
                        .contains(TagNames.IM_PUBLIC)) {
                    PathQuery query = template.getPathQuery();
                    return query;
                } else {
                    return template;
                }
            } else {
                throw new IllegalArgumentException("Cannot find template " + name + " in context "
                        + scope);
            }
        }
    }

    /**
     * Method called to get the path query.
     * @param session HttpSession object
     * @return PathQuery object
     */
    private PathQuery getPathQuery(HttpSession session) {
        // path query name is empty
        PathQuery query =  SessionMethods.getQuery(session);

        if (query != null) {
            // If Class is Template, convert it to PathQuery
            if ("TemplateQuery".equals(TypeUtil.unqualifiedName(query.getClass().toString()))) {
                query = ((TemplateQuery) query).getPathQuery();
            }
            return query;
        } else {
            throw new IllegalArgumentException("Cannot find a query");
        }
    }

    /**
     * Method to set a new WebserviceCodeGenInfo object.
     * @param query a PathQuery or TemplateQuery object
     * @param serviceRootURL the base url of web service
     * @param projectName the InterMine project name
     * @return a WebserviceCodeGenInfo object with query, serviceRootURL and projectName set
     */
    private WebserviceCodeGenInfo setWebserviceCodeGenInfo(PathQuery query,
            String serviceRootURL, String projectTitle, String perlWSModuleVer) {

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(query, serviceRootURL, projectTitle, perlWSModuleVer);
        return wsCodeGenInfo;
    }

    /**
     * Method called to print the source code.
     * @param sourceCodeString a string representing the source code
     * @param method perl/java
     * @param source template query/path query
     * @param response HttpServletResponse
     */
    private void output(String sourceCodeString, String method, String source,
            HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=" + source + "." + method);

        PrintStream out;
        try {
            out = new PrintStream(response.getOutputStream());
            out.print(sourceCodeString);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
