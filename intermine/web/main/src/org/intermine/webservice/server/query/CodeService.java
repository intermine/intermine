package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.URLGenerator;

import javax.servlet.http.HttpSession;
import org.intermine.pathquery.PathQuery;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.codegen.WebserviceCodeGenInfo;
import org.intermine.api.query.codegen.WebserviceCodeGenerator;
import org.intermine.api.query.codegen.WebserviceJavaCodeGenerator;
import org.intermine.api.query.codegen.WebserviceJavaScriptCodeGenerator;
import org.intermine.api.query.codegen.WebservicePerlCodeGenerator;
import org.intermine.api.query.codegen.WebservicePythonCodeGenerator;
import org.intermine.api.query.codegen.WebserviceRubyCodeGenerator;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

/**
 * A service for generating code based on a query.
 * @author Alex Kalderimis
 *
 */
public class CodeService extends AbstractQueryService
{
    protected static final Logger LOG = Logger.getLogger(CodeService.class);

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public CodeService(InterMineAPI im) {
        super(im);
    }

    private WebserviceCodeGenerator getCodeGenerator(String lang) {
        lang = StringUtils.lowerCase(lang);

        if ("perl".equals(lang) || "pl".equals(lang)) {
            return new WebservicePerlCodeGenerator();
        } else if ("java".equals(lang)) {
            return new WebserviceJavaCodeGenerator();
        } else if ("python".equals(lang) || "py".equals(lang)) {
            return new WebservicePythonCodeGenerator();
        } else if ("javascript".equals(lang) || "js".equals(lang)) {
            return new WebserviceJavaScriptCodeGenerator();
        } else if ("ruby".equals(lang) || "rb".equals(lang)) {
            return new WebserviceRubyCodeGenerator();
        } else {
            throw new BadRequestException("Unknown code generation language: " + lang);
        }
    }

    @Override
    protected void execute() {

        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        // Ref to OrthologueLinkController and OrthologueLinkManager
        Properties webProperties = SessionMethods.getWebProperties(session
                .getServletContext());

        String serviceBaseURL = new URLGenerator(request).getPermanentBaseURL();
        // set in project properties
        String projectTitle = webProperties.getProperty("project.title");
        // set in global.web.properties
        String perlWSModuleVer = webProperties.getProperty("perl.wsModuleVer");

        String lang = request.getParameter("lang");
        PathQuery pq = getPathQuery();

        WebserviceCodeGenInfo info = new WebserviceCodeGenInfo(
                        pq,
                        serviceBaseURL,
                        projectTitle,
                        perlWSModuleVer,
                        pathQueryIsPublic(pq, im, profile),
                        profile.getUsername());
        WebserviceCodeGenerator codeGen = getCodeGenerator(lang);
        String sc = codeGen.generate(info);
        if (formatIsJSON()) {
            ResponseUtil.setJSONHeader(response, "querycode.json");
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (formatIsJSONP()) {
                String callback = getCallback();
                if (callback == null || "".equals(callback)) {
                    callback = DEFAULT_CALLBACK;
                }
                attributes.put(JSONFormatter.KEY_CALLBACK, callback);
            }
            attributes.put(JSONFormatter.KEY_INTRO, "\"code\":");
            attributes.put(JSONFormatter.KEY_OUTRO, "");
            output.setHeaderAttributes(attributes);
            // Oddly, here escape Java is correct, not escapeJavaScript.
            // This is due to syntax errors thrown by escaped single quotes.
            sc = "\"" + StringEscapeUtils.escapeJava(sc) + "\"";
        }
        output.addResultItem(Arrays.asList(sc));
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

    private PathQuery getPathQuery() {
        String xml = QueryRequestParser.getQueryXml(request);
        PathQueryBuilder pqb = getQueryBuilder(xml);
        PathQuery query = pqb.getQuery();
        return query;
    }

}
