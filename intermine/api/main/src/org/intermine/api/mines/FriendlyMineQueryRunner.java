package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2014 FlyMine
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Class to query friendly mines.
 *
 * @author Julie Sullivan
 */
public final class FriendlyMineQueryRunner
{
    private static class URLRequester implements MineRequester
    {

        @Override
        public BufferedReader runQuery(Mine mine, String xmlQuery) {
            try {
                String urlString = mine.getUrl() + WEBSERVICE_URL + QUERY_PATH
                            + URLEncoder.encode("" + xmlQuery, "UTF-8");
                URL url = new URL(urlString);
                return new BufferedReader(new InputStreamReader(url.openStream()));
            } catch (Exception e) {
                LOG.info("Unable to access " + mine.getName() + " exception: " + e.getMessage());
                return null;
            }
        }

        @Override
        public BufferedReader requestURL(String urlString) {
            BufferedReader reader = null;
            try {
                if (!urlString.contains("?")) {
                    // GET
                    URL url = new URL(urlString);
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(CONNECT_TIMEOUT);
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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

    private static final Logger LOG = Logger.getLogger(FriendlyMineQueryRunner.class);
    private static final String WEBSERVICE_URL = "/service";
    private static final String QUERY_PATH = "/query/results?format=json&query=";
    private static Map<MultiKey, JSONObject> queryResultsCache
        = new CacheMap<MultiKey, JSONObject>();
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final boolean DEBUG = false;
    private static final int CONNECT_TIMEOUT = 20000; // 20 seconds
    private final MineRequester requester;

    /**
     * Construct a query runner that will make HTTP web-service requests.
     */
    public FriendlyMineQueryRunner() {
        requester = new URLRequester();
    }

    /**
     * Construct a query runner that will use the injected requester.
     * @param requester The object that makes requests for information.
     */
    public FriendlyMineQueryRunner(MineRequester requester) {
        this.requester = requester;
    }

    /**
     * Query a mine and receive map of results.  only processes first two columns set as id and
     * name.
     *
     * @param mine mine to query
     * @param xmlQuery query to run
     * @return map of results
     * @throws IOException if something goes wrong
     * @throws JSONException bad JSON
     */
    public JSONObject runJSONWebServiceQuery(Mine mine, String xmlQuery)
        throws IOException, JSONException {
        MultiKey key = new MultiKey(mine, xmlQuery);
        JSONObject jsonMine = queryResultsCache.get(key);
        if (jsonMine != null) {
            return jsonMine;
        }
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        BufferedReader reader = requester.runQuery(mine, xmlQuery);
        if (reader == null) {
            LOG.info(String.format("no results found for %s for query \"%s\"",
                    mine.getName(), xmlQuery));
            return null;
        }
        try {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject result = new JSONObject(tokener);
            JSONArray rows = result.getJSONArray("results");
            for (int i = 0, l = rows.length(); i < l; i++) {
                JSONArray row = rows.getJSONArray(i);
                Map<String, Object> found = new HashMap<String, Object>();
                found.put("id", row.get(0));
                found.put("name", row.get(1));
                if (row.length() > 2) {
                    // used for extra value, eg. organism name
                    found.put("ref", row.get(2));
                }
                results.add(found);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Error reading results.", e);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("results", results);
        jsonMine = new JSONObject(data);
        queryResultsCache.put(key, jsonMine);
        return jsonMine;
    }


    /**
     * get release version number for each mine.  if release number is different from the one
     * we have locally, run queries to populate maps
     * @param mines list of mines to update
     */
    public void updateReleaseVersion(Map<String, Mine> mines) {
        boolean clearCache = false;
        for (Mine mine : mines.values()) {
            String currentReleaseVersion = mine.getReleaseVersion();
            String url = mine.getUrl() + WEBSERVICE_URL + RELEASE_VERSION_URL;
            BufferedReader reader = requester.requestURL(url);
            final String msg = "Unable to retrieve release version for " + mine.getName();
            String newReleaseVersion = null;

            if (reader != null) {
                try {
                    newReleaseVersion = IOUtils.toString(reader);
                } catch (Exception e) {
                    LOG.warn(msg, e);
                    continue;
                }
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
                clearCache = true;
            }
        }
        if (clearCache) {
            queryResultsCache = new HashMap<MultiKey, JSONObject>();
        }
    }


    /**
     * Run a query via the web service
     *
     * @param urlString url to query
     * @return reader
     */
    public BufferedReader runWebServiceQuery(String urlString) {
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }
        return requester.requestURL(urlString);
    }
}

