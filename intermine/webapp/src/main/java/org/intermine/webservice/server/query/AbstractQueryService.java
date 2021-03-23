package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.query.result.PathQueryBuilder;
import org.intermine.webservice.server.query.result.PathQueryBuilderForJSONObj;

/**
 * A base class for query services.
 * @author Alex Kalderimis
 *
 */
public abstract class AbstractQueryService extends WebService
{

    private static final String XML_SCHEMA_LOCATION = "webservice/query.xsd";
    private static final String JSON_SCHEMA_LOCATION = "webservice/query.schema";

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public AbstractQueryService(InterMineAPI im) {
        super(im);
    }

    /**
     * @param queryFormat JSON or XML
     * @return The XML Schema url.
     */
    protected String getSchemaUrl(String queryFormat) {
        return AbstractQueryService.getSchemaLocation(request, queryFormat);
    }

    /**
     * @param request A request for a mine, so we can work out where the schema probably is.
     * @param queryFormat JSON or XML
     * @return The Schema url.
     */
    public static String getSchemaLocation(HttpServletRequest request, String queryFormat) {
        String schemaLocation = XML_SCHEMA_LOCATION;
        if ("JSON".equalsIgnoreCase(queryFormat)) {
            schemaLocation = JSON_SCHEMA_LOCATION;
        }
        try {
            final Properties webProperties = InterMineContext.getWebProperties();
            String baseUrl = webProperties.getProperty("webapp.baseurl");
            String path = webProperties.getProperty("webapp.path");
            String relPath = path + "/" + schemaLocation;
            URL url = new URL(baseUrl + "/" + relPath);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Get a path-query builder.
     * @param input The query XML or JSON.
     * @return A builder for this query.
     */
    protected PathQueryBuilder getQueryBuilder(String input) {
        final ListManager listManager = new ListManager(im, getPermission().getProfile());
        String queryFormat = "XML";
        if (!input.startsWith("<query")) {
            queryFormat = "JSON";
        }
        if (formatIsJsonObj()) {
            return new PathQueryBuilderForJSONObj(input, getSchemaUrl(queryFormat), listManager);
        } else {
            return new PathQueryBuilder(im, input, getSchemaUrl(queryFormat), listManager);
        }
    }

    /**
     * @return Whether or not the format is for JSON-Objects
     */
    protected boolean formatIsJsonObj() {
        return getFormat() == Format.OBJECTS;
    }

}
