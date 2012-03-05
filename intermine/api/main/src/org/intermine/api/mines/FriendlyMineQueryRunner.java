package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2011 FlyMine
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
public final class FriendlyMineQueryRunner
{
    private static final Logger LOG = Logger.getLogger(FriendlyMineQueryRunner.class);
    private static final String WEBSERVICE_URL = "/service";
    private static final String QUERY_PATH = "/query/results?format=xml&query=";
    private static Map<MultiKey, JSONObject> queryResultsCache
        = new CacheMap<MultiKey, JSONObject>();
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final boolean DEBUG = true;

    private FriendlyMineQueryRunner() {
        // don't
    }

    /**
     * Query a mine and recieve map of results.  only processes first two columns set as id and
     * name
     *
     * TODO use Java  webservice client instead.  See #2829
     *
     * @param mine mine to query
     * @param xmlQuery query to run
     * @return map of results
     * @throws IOException if something goes wrong
     */
    public static JSONObject runJSONWebServiceQuery(Mine mine, String xmlQuery)
        throws IOException {
        MultiKey key = new MultiKey(mine, xmlQuery);
        JSONObject jsonMine = queryResultsCache.get(key);
        if (jsonMine != null) {
            return jsonMine;
        }
        List<Map<String, String>> genes = new ArrayList<Map<String, String>>();

        BufferedReader reader = runWebServiceQuery(mine, xmlQuery);
        if (reader == null) {
            LOG.info(String.format("no results found for %s for query \"%s\"",
                    mine.getName(), xmlQuery));
            return null;
        }
        XMLTableResult table = new XMLTableResult(reader);
        for (List<String> row: table.getData()) {
            Map<String, String> gene = new HashMap<String, String>();
            gene.put("id", row.get(0));
            gene.put("name", row.get(1));
            genes.add(gene);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("results", genes);
        jsonMine = new JSONObject(data);
        queryResultsCache.put(key, jsonMine);
        return jsonMine;
    }

    /**
     * Run a query on a mine using XML query
     * @param mine mine to query
     * @param xmlQuery pathQuery.toXML()
     * @return results
     */
    private static BufferedReader runWebServiceQuery(Mine mine, String xmlQuery) {
        try {
            String urlString = mine.getUrl() + WEBSERVICE_URL + QUERY_PATH
                    + URLEncoder.encode("" + xmlQuery, "UTF-8");
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader;
        } catch (Exception e) {
            LOG.info("Unable to access " + mine.getName() + " exception: " + e.getMessage());
            return null;
        }
    }

    /**
     * get release version number for each mine.  if release number is different from the one
     * we have locally, run queries to populate maps
     * @param mines list of mines to update
     */
    public static void updateReleaseVersion(Map<String, Mine> mines) {
        for (Mine mine : mines.values()) {
            String currentReleaseVersion = mine.getReleaseVersion();
            String url = mine.getUrl() + WEBSERVICE_URL + RELEASE_VERSION_URL;
            BufferedReader reader = runWebServiceQuery(url);
            final String msg = "Unable to retrieve release version for " + mine.getName();
            String newReleaseVersion;
            try {
                newReleaseVersion = IOUtils.toString(reader);
            } catch (IOException e) {
                LOG.warn(msg, e);
                continue;
            }

            if (StringUtils.isBlank(newReleaseVersion)
                    && StringUtils.isBlank(currentReleaseVersion)) {
                // didn't get a release version this time or last time
                LOG.warn(msg);
                continue;
            }

            // if release version is different
            if (!StringUtils.equals(newReleaseVersion, currentReleaseVersion)
                    || StringUtils.isBlank(currentReleaseVersion)
                    || DEBUG) {

                // update release version
                mine.setReleaseVersion(newReleaseVersion);

                queryResultsCache = new HashMap<MultiKey, JSONObject>();
            }
        }
    }


    /**
     * Run a query via the web service
     *
     * @param urlString url to query
     * @return reader
     */
    public static BufferedReader runWebServiceQuery(String urlString) {
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }
        BufferedReader reader = null;
        try {
            if (!urlString.contains("?")) {
                // GET
                URL url = new URL(urlString);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                LOG.info("FriendlyMine URL (GET) " + urlString);
            } else {
                // POST
                String[] params = urlString.split("\\?");
                String newUrlString = params[0];
                String queryString = params[1];
                URL url = new URL(newUrlString);
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(queryString);
                wr.flush();
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                LOG.info("FriendlyMine URL (POST) " + urlString);
            }
            return reader;
        } catch (Exception e) {
            LOG.info("Unable to access " + urlString + " exception: " + e.getMessage());
            return null;
        }
    }
}

