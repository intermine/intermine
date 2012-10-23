package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.codegen.WebserviceCodeGenInfo;
import org.intermine.api.query.codegen.WebserviceCodeGenerator;
import org.intermine.api.query.codegen.WebserviceJavaCodeGenerator;
import org.intermine.api.query.codegen.WebserviceJavaScriptCodeGenerator;
import org.intermine.api.query.codegen.WebservicePerlCodeGenerator;
import org.intermine.api.query.codegen.WebservicePythonCodeGenerator;
import org.intermine.api.query.codegen.WebserviceRubyCodeGenerator;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.pathquery.PathQuery;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

/**
 * Action to handle the web service code generation.
 * Multiple-query is not supported.
 *
 * @author Fengyuan Hu
 * @author Alexis Kalderimis
 */
public class WebserviceCodeGenAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(WebserviceCodeGenAction.class);

    private WebserviceCodeGenerator getCodeGenerator(String method) {
        if ("perl".equals(method)) {
            return new WebservicePerlCodeGenerator();
        } else if ("java".equals(method)) {
            return new WebserviceJavaCodeGenerator();
        } else if ("python".equals(method)) {
            return new WebservicePythonCodeGenerator();
        } else if ("javascript".equals(method)) {
            return new WebserviceJavaScriptCodeGenerator();
        } else if ("ruby".equals(method)) {
            return new WebserviceRubyCodeGenerator();
        } else {
            throw new IllegalArgumentException("Unknown code generation language: " + method);
        }
    }

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

            WebserviceCodeGenInfo info = null;
            if ("templateQuery".equals(source)) {
                TemplateQuery template = getTemplateQuery(profile, request);
                info = getWebserviceCodeGenInfo(
                        template,
                        serviceBaseURL,
                        projectTitle,
                        perlWSModuleVer,
                        templateIsPublic(template, im, profile),
                        profile.getUsername());

            } else if ("pathQuery".equals(source)) {
                PathQuery pq = getPathQuery(session);
                info = getWebserviceCodeGenInfo(
                        pq,
                        serviceBaseURL,
                        projectTitle,
                        perlWSModuleVer,
                        pathQueryIsPublic(pq, im, profile),
                        profile.getUsername());
            }
            WebserviceCodeGenerator codeGen = getCodeGenerator(method);
            String sc = codeGen.generate(info);
            sendCode(sc, info.getFileName(), getExtension(method), response);
        } catch (Exception e) {
            LOG.error(e);
            e.printStackTrace();
            sendCode(e.toString(), "exception", "e", response);
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
    private TemplateQuery getTemplateQuery(Profile profile, HttpServletRequest request) {

        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String originalTemplate = request.getParameter("originalTemplate");

        TemplateManager templateManager = SessionMethods.getInterMineAPI(request).getTemplateManager();
        if (name == null) {
            throw new IllegalArgumentException("Cannot find a template in context "
                                                   + scope);
        } else {
            TemplateQuery template = (originalTemplate != null)
                                     ? templateManager.getTemplate(profile, name, scope)
                                     : (TemplateQuery) SessionMethods.getQuery(request);
            if (template != null) {
                return template;
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
        PathQuery query =  SessionMethods.getQuery(session);

        if (query != null) {
            // Use copy constructor, as we need to return an object of this exact type,
            // since the CodeGenerators use the class to determine how to generate code.
            return new PathQuery(query);
        } else {
            throw new IllegalArgumentException("Cannot find a query");
        }
    }

    /**
     * Utility function to determine whether the template is publicly accessible.
     * This is determined by checking whether this template is within the current
     * user's profile, and whether the underlying query is itself public.
     * @param t The template
     * @param im A reference to the API
     * @param p A reference to the current user's Profile
     * @return whether or not Joe Public could run this without logging in.
     */
    protected static boolean templateIsPublic(TemplateQuery t, InterMineAPI im, Profile p) {
        Map<String, ApiTemplate> templates = p.getSavedTemplates();

        return !templates.keySet().contains(t.getName()) && pathQueryIsPublic(t, im, p);
    }

    /**
     * Utility function to determine whether the PathQuery is publicly accessible.
     * PathQueries are accessibly publicly as long as they do not reference
     * private lists.
     * @param pq The query to interrogate
     * @param im A reference to the InterMine API
     * @param p A user's profile
     * @return whether the query is accessible publicly or not
     */
    protected static boolean pathQueryIsPublic(PathQuery pq, InterMineAPI im, Profile p) {
        Set<String> listNames = pq.getBagNames();
        TagManager tm = im.getTagManager();
        for (String name: listNames) {
            Set<String> tags = tm.getObjectTagNames(name, TagTypes.BAG, p.getUsername());
            if (!tags.contains(TagNames.IM_PUBLIC)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to set a new WebserviceCodeGenInfo object.
     * @param query a PathQuery or TemplateQuery object
     * @param serviceRootURL the base url of web service
     * @param projectName the InterMine project name
     * @return a WebserviceCodeGenInfo object with query, serviceRootURL and projectName set
     */
    private WebserviceCodeGenInfo getWebserviceCodeGenInfo(PathQuery query,
            String serviceRootURL, String projectTitle, String perlWSModuleVer,
            boolean isPublic, String user) {

        WebserviceCodeGenInfo wsCodeGenInfo = new WebserviceCodeGenInfo(query,
                serviceRootURL, projectTitle, perlWSModuleVer, isPublic, user);
        return wsCodeGenInfo;
    }

    /**
     * Method called to print the source code.
     * @param sourceCodeString a string representing the source code
     * @param extension the file extension to add (pl/py/java/...)
     * @param filename The name for the file (template_query/path_query)
     * @param response HttpServletResponse
     */
    private void sendCode(String sourceCodeString, String extension, String filename,
            HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename="
                + filename + "." + extension);

        PrintStream out;
        try {
            out = new PrintStream(response.getOutputStream());
            out.print(sourceCodeString);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the extension for the method type. By default it returns the
     * method itself, unless there is a predefined translation.
     * @param method the method to translate
     * @return the extension to use
     */
    private static String getExtension(String method) {
        if ("perl".equalsIgnoreCase(method)) {
            return "pl";
        }
        if ("python".equalsIgnoreCase(method)) {
            return "py";
        }
        if ("javascript".equalsIgnoreCase(method)) {
            return "html"; // javascript is meant to be embedded
        }
        return method;
    }
}
