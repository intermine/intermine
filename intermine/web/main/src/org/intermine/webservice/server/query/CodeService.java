package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.query.result.PathQueryBuilder;
import org.json.JSONObject;

/**
 * A service for generating code based on a query.
 * @author Alex Kalderimis
 *
 */
public class CodeService extends AbstractQueryService
{
    protected static final Logger LOG = Logger.getLogger(CodeService.class);
    private String perlModuleVersion;
    private static final String PERL_MODULE_URI =
            "http://api.metacpan.org/v0/module/Webservice::InterMine";

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public CodeService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.TEXT;
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
            case JSON:
                return true;
            case TEXT:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getDefaultFileName() {
        return "query";
    }

    @Override
    protected String getExtension() {
        String extension = super.getExtension();
        String lang = request.getParameter("lang");
        if ("perl".equals(lang) || "pl".equals(lang)) {
            return ".pl" + extension;
        } else if ("java".equals(lang)) {
            return ".java" + extension;
        } else if ("python".equals(lang) || "py".equals(lang)) {
            return ".py" + extension;
        } else if ("javascript".equals(lang) || "js".equals(lang)) {
            return ".html" + extension;
        } else if ("ruby".equals(lang) || "rb".equals(lang)) {
            return ".rb" + extension;
        } else {
            throw new BadRequestException("Unknown code generation language: " + lang);
        }
    }

    private WebserviceCodeGenerator getCodeGenerator(String lang) {
        lang = StringUtils.lowerCase(lang);

        // Ordered by expected popularity.
        if ("js".equals(lang) || "javascript".equals(lang)) {
            return new WebserviceJavaScriptCodeGenerator();
        } else if ("py".equals(lang) || "python".equals(lang)) {
            return new WebservicePythonCodeGenerator();
        } else if ("java".equals(lang)) {
            return new WebserviceJavaCodeGenerator();
        } else if ("pl".equals(lang) || "perl".equals(lang)) {
            return new WebservicePerlCodeGenerator();
        } else if ("rb".equals(lang) || "ruby".equals(lang)) {
            return new WebserviceRubyCodeGenerator();
        } else {
            throw new BadRequestException("Unknown code generation language: " + lang);
        }
    }

    @Override
    protected void execute() {

        Profile profile = getPermission().getProfile();

        // Ref to OrthologueLinkController and OrthologueLinkManager
        String serviceBaseURL = new URLGenerator(request).getPermanentBaseURL();
        // set in project properties
        String projectTitle = webProperties.getProperty("project.title");
        // set in global.web.properties
        String perlWSModuleVer = getPerlModuleVersion();
        String lang = request.getParameter("lang");
        PathQuery pq = getPathQuery();
        String name = pq.getTitle() != null ? pq.getTitle() : "query";
        String fileName = name.replaceAll("[^a-zA-Z0-9_,.()-]", "_") + getExtension();

        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        WebserviceCodeGenInfo info = new WebserviceCodeGenInfo(
                        pq,
                        serviceBaseURL,
                        projectTitle,
                        perlWSModuleVer,
                        pathQueryIsPublic(pq, im, profile),
                        profile,
                        getLineBreak());
        info.readWebProperties(webProperties);
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

    private String getPerlModuleVersion() {
        if (perlModuleVersion == null) {
            BufferedReader reader = null;
            try {
                URL url = new URL(PERL_MODULE_URI);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "InterMine-" + Constants.WEB_SERVICE_VERSION);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuffer body = new StringBuffer();
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                String json = body.toString();
                JSONObject data = new JSONObject(json);
                perlModuleVersion = data.getString("version");
            } catch (Exception e) {
                return null;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // Ignore.
                    }
                }
            }
        }
        return perlModuleVersion;
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
        String xml = new QueryRequestParser(im.getQueryStore(), request).getQueryXml();
        PathQueryBuilder pqb = getQueryBuilder(xml);
        PathQuery query = pqb.getQuery();
        return query;
    }

}
