package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.CacheMap;
import org.intermine.webservice.client.results.XMLTableResult;
import org.json.JSONObject;

/**
 * Class to query friendly mines.
 *
 * @author Julie Sullivan
 */
public final class TestFriendlyMineQueryRunner
{
    private static final Logger LOG = Logger.getLogger(TestFriendlyMineQueryRunner.class);
    private static final String WEBSERVICE_URL = "/service";
    private static final String QUERY_PATH = "/query/results?format=xml&query=";
    private static Map<MultiKey, JSONObject> queryResultsCache
        = new CacheMap<MultiKey, JSONObject>();
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final boolean DEBUG = true;

    private TestFriendlyMineQueryRunner() {
        // don't
    }

    /**
     * Query a mine and recieve map of results.  only processes first two columns set as id and
     * name
     *
     * @param mine mine to query
     * @param xmlQuery query to run
     * @return map of results
     * @throws IOException if something goes wrong
     */
    public static JSONObject runJSONWebServiceQuery(Mine mine, String xmlQuery) {
        return null;
    }

    /**
     * Run a query on a mine using XML query
     * @param mine mine to query
     * @param xmlQuery pathQuery.toXML()
     * @return results
     */
    private static BufferedReader runWebServiceQuery(Mine mine, String xmlQuery) {
        return null;
    }

    /**
     * get release version number for each mine.  if release number is different from the one
     * we have locally, run queries to populate maps
     * @param mines list of mines to update
     */
    public static void updateReleaseVersion(Map<String, Mine> mines) {
        return;
    }


    /**
     * Run a query via the web service
     *
     * @param urlString url to query
     * @return reader
     */
    public static BufferedReader runWebServiceQuery(String urlString) {
        return null;
    }
}

