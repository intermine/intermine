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
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.CacheMap;
import org.json.JSONException;
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
    private static final String QUERY_PATH = "/query/results?size=1000&format=tab&query=";
    private static Map<MultiKey, JSONObject> queryResultsCache
        = new CacheMap<MultiKey, JSONObject>();
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final boolean DEBUG = false;

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
        Set<JSONObject> results = new LinkedHashSet<JSONObject>();
        BufferedReader reader = runWebServiceQuery(mine, xmlQuery);
        if (reader == null) {
            LOG.info("no results found for " + mine.getName() + " for query " + xmlQuery);
            return null;
        }
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] bits = line.split("\\t");
            if (bits.length == 0) {
                return null;
            }
            JSONObject gene = new JSONObject();
            try {
                gene.put("id", bits[0]);
                gene.put("name", bits[1]);
                results.add(gene);
            } catch (JSONException e) {
                LOG.info("couldn't parse results for " + mine.getName() + " for query " + xmlQuery);
                continue;
            }
        }
        jsonMine = new JSONObject();
        try {
            jsonMine.put("results", results);
        } catch (JSONException e) {
            LOG.info("couldn't process results for " + mine.getName() + " for query " + xmlQuery);
            return null;
        }
        queryResultsCache.put(key, jsonMine);
        return jsonMine;
    }

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
            String newReleaseVersion = null;
            try {
                if (reader != null) {
                    newReleaseVersion = reader.readLine();
                } else {
                    LOG.info(msg);
                    continue;
                }
            } catch (Exception e) {
                LOG.warn(msg);
                continue;
            }

            if (StringUtils.isEmpty(newReleaseVersion)
                    && StringUtils.isEmpty(currentReleaseVersion)) {
                // didn't get a release version this time or last time
                LOG.warn(msg);
                continue;
            }

            // if release version is different
            if (StringUtils.isEmpty(newReleaseVersion)
                    || StringUtils.isEmpty(currentReleaseVersion)
                    || !newReleaseVersion.equals(currentReleaseVersion)
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
    private static BufferedReader runWebServiceQuery(String urlString) {
        try {
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader;
        } catch (Exception e) {
            LOG.info("Unable to access " + urlString + " exception: " + e.getMessage());
            return null;
        }
    }
}

