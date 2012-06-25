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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.InternalErrorException;
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

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public AbstractQueryService(InterMineAPI im) {
        super(im);
    }

    /**
     * @return The XML Schema url.
     */
    protected String getXMLSchemaUrl() {
        try {
            String relPath = request.getContextPath() + "/"
                    + XML_SCHEMA_LOCATION;
            URL url = new URL(request.getScheme(), request.getServerName(),
                    request.getServerPort(), relPath);
            return url.toString();
        } catch (MalformedURLException e) {
            throw new InternalErrorException(e);
        }
    }

    /**
     * Get a path-query builder.
     * @param xml The query XML.
     * @return A builder for this query.
     */
    protected PathQueryBuilder getQueryBuilder(String xml) {
        ListManager listManager = new ListManager(im, getPermission().getProfile());

        Map<String, InterMineBag> savedBags = new HashMap<String, InterMineBag>();
        for (InterMineBag bag: listManager.getLists()) {
            savedBags.put(bag.getName(), bag);
        }

        if (formatIsJsonObj()) {
            return new PathQueryBuilderForJSONObj(xml, getXMLSchemaUrl(),
                    savedBags);
        } else {
            return new PathQueryBuilder(xml, getXMLSchemaUrl(), savedBags);
        }
    }

}
