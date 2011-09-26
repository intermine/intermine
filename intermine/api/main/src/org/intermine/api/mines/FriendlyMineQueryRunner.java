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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.config.Constants;
import org.intermine.util.CacheMap;
import org.intermine.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to manage orthologue linkouts to other intermines on the list analysis page
 *
 *  1. works out friendly mines
 *  2. for friendly mines and local mine, runs two queries:
 *     a. which genes for which organisms are available to query
 *     b. which organisms and datasets for orthologues are available
 *  3. Cache the results of these two queries and update every day/hour
 *  4. uses webservice to retrieve release version
 * @author Julie Sullivan
 */
public final class FriendlyMineQueryRunner
{
    private static final boolean DEBUG = true;
    private static final Logger LOG = Logger.getLogger(FriendlyMineQueryRunner.class);
    private static final String WEBSERVICE_URL = "/service";
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final String TEMPLATE_PATH = "/template/results?size=1000&format=tab&name=";
    private static final String QUERY_PATH = "/query/results?size=1000&format=tab&query=";
    private static final String VALUES_URL = WEBSERVICE_URL + TEMPLATE_PATH
        + Constants.VALUES_TEMPLATE + Constants.IDENTIFIER_CONSTRAINT;
    private static final String MAP_URL = WEBSERVICE_URL + TEMPLATE_PATH + Constants.MAP_TEMPLATE
        + Constants.IDENTIFIER_CONSTRAINT;
    private static final String WILDCARD = "*";
    private static Map<MultiKey, JSONObject> queryResultsCache
        = new CacheMap<MultiKey, JSONObject>();
    private static final String EMPTY = "\"\""; // webservices returns "" as empty

    private FriendlyMineQueryRunner() {
        // don't
    }

    /**
     * Test for value in a mine.  Returns array (length=2) of identifiers.
     *
     *  [0] = primaryIdentifier
     *  [1] = symbol or (if symbol is NULL) primaryIdentifier
     *
     * Identifier returned my be different because the given identifier may be a synonym or a
     * better identifier was found (eg. symbol over primaryIdentifier)
     *
     * @param mine Mine to test
     * @param constraintValue extra constraint value
     * @param identifier identifier of object
     * @return identifier of the object if presents or NULL if not.
     */
    public static String[] getObjectInOtherMine(Mine mine, String constraintValue,
            String identifier) {
        if (mine == null || mine.getReleaseVersion() == null) {
            return null;
        }
        final String webserviceURL = mine.getUrl() + WEBSERVICE_URL + TEMPLATE_PATH
            + Constants.REPORT_TEMPLATE + Constants.LOOKUP_CONSTRAINT + identifier
            + Constants.EXTRA_VALUE_CONSTRAINT + constraintValue;
        String[] identifiers = new String[2];
        try {
            BufferedReader reader = runWebServiceQuery(webserviceURL);
            if (reader == null) {
                LOG.error(mine.getName() + " does not have template " + webserviceURL);
                return null;
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] bits = line.split("\\t");
                if (bits.length != 2) {
                    final String msg = "Couldn't process links for " + mine.getName()
                        + ".  Expected two columns, found " + bits.length + " columns instead."
                        + webserviceURL;
                    LOG.info(msg);
                    return null;
                }
                String newIdentifier = bits[0];
                String symbol = bits[1];
                if (!StringUtils.isEmpty(newIdentifier) && !EMPTY.equals(newIdentifier)) {
                    identifiers[0] = newIdentifier;
                    identifiers[1] = newIdentifier;
                }
                if (!StringUtils.isEmpty(symbol) && !EMPTY.equals(symbol)) {
                    identifiers[1] = symbol;
                    if (StringUtils.isEmpty(newIdentifier)) {
                        identifiers[0] = symbol;
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return null;
        }
        return identifiers;
    }

    /**
     * Test for values in a mine.  Returns array (length=2) of identifiers.
     *
     *  [0] = primaryIdentifier OR NULL
     *  [1] = symbol or (if symbol is NULL) primaryIdentifier OR NULL
     *
     * Identifier returned my be different because the given identifier may be a synonym or a
     * better identifier was found (eg. symbol over primaryIdentifier)
     *
     * @param mine Mine to test
     * @param constraintValue extra constraint value
     * @param identifiers identifier of objects
     * @return identifier of the object if presents or NULL if not.
     */
    public static Set<String[]> getObjectsInOtherMine(Mine mine, String constraintValue,
            String identifiers) {
        if (mine == null || mine.getReleaseVersion() == null) {
            return null;
        }
        final String webserviceURL = mine.getUrl() + WEBSERVICE_URL + TEMPLATE_PATH
            + Constants.REPORT_TEMPLATE + Constants.LOOKUP_CONSTRAINT + identifiers
            + Constants.EXTRA_VALUE_CONSTRAINT + constraintValue;

        Set<String[]> results = new HashSet<String[]>();
        try {
            BufferedReader reader = runWebServiceQuery(webserviceURL);
            if (reader == null) {
                LOG.error(mine.getName() + " does not have template " + webserviceURL);
                return null;
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] bits = line.split("\\t");
                String[] identifierPair = new String[2];
                if (bits.length != 2) {
                    final String msg = "Couldn't process links for " + mine.getName()
                        + ".  Expected two columns, found " + bits.length + " columns instead."
                        + webserviceURL;
                    LOG.info(msg);
                    return null;
                }
                String newIdentifier = bits[0];
                String symbol = bits[1];
                if (!StringUtils.isEmpty(newIdentifier) && !EMPTY.equals(newIdentifier)) {
                    identifierPair[0] = newIdentifier;
                    identifierPair[1] = newIdentifier;
                }
                if (!StringUtils.isEmpty(symbol) && !EMPTY.equals(symbol)) {
                    identifierPair[1] = symbol;
                    if (StringUtils.isEmpty(newIdentifier)) {
                        identifierPair[0] = symbol;
                    }
                }
                results.add(identifierPair);
            }
        } catch (Exception e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return null;
        }
        return results;
    }

    /**
     * used on REPORT page
     *
     * Runs queries and builds data structure for display on report pages.
     *
     * @param mine to query
     * @param identifier identifier for object from report page
     * @param constraintValue optional additonal constraint, eg. organism
     * @return the list of valid mines for the given object
     */
    public static Map<String, Set<String[]>> runRelatedDataQuery(Mine mine, String constraintValue,
            String identifier) {
        if (mine == null || mine.getReleaseVersion() == null) {
            return null;
        }
        final String webserviceURL = mine.getUrl() + WEBSERVICE_URL + TEMPLATE_PATH
            + Constants.RELATED_DATA_TEMPLATE + Constants.RELATED_DATA_CONSTRAINT_1 + identifier
            + Constants.RELATED_DATA_CONSTRAINT_2 + constraintValue;
        Map<String, Set<String[]>> results = new HashMap<String, Set<String[]>>();
        try {
            BufferedReader reader = runWebServiceQuery(webserviceURL);
            if (reader == null) {
                LOG.error(mine.getName() + " does not have template " + webserviceURL);
                return null;
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] bits = line.split("\\t");
                if (bits.length != 3) {
                    String msg = "Couldn't process links for " + mine.getName()
                        + ".  Expected three columns, found " + bits.length + " columns instead."
                        + webserviceURL;
                    LOG.info(msg);
                    return null;
                }
                String key = bits[0];
                String newIdentifier = bits[1];
                String symbol = bits[2];
                String[] identifiers = new String[2];
                if (!StringUtils.isEmpty(newIdentifier)) {
                    identifiers[0] = newIdentifier;
                    identifiers[1] = newIdentifier;
                }
                if (!StringUtils.isEmpty(symbol)) {
                    identifiers[1] = symbol;
                }
                Util.addToSetMap(results, key, identifiers);
            }
        } catch (Exception e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return null;
        }
        return results;
    }

    /**
     * get release version number for each mine.  if release number is different from the one
     * we have locally, run queries to populate maps
     * @param mines list of mines to update
     */
    public static void updateData(Map<String, Mine> mines) {
        for (Mine mine : mines.values()) {
            String currentReleaseVersion = mine.getReleaseVersion();
            String url = mine.getUrl() + WEBSERVICE_URL + RELEASE_VERSION_URL;
            BufferedReader reader = runWebServiceQuery(url);
            String newReleaseVersion = null;
            final String msg = "Unable to retrieve release version for " + mine.getName();
            try {
                if (reader != null) {
                    newReleaseVersion = reader.readLine();
                } else {
                    LOG.info(msg);
                    continue;
                }
            } catch (Exception e) {
                LOG.info(msg, e);
                continue;
            }

            if (StringUtils.isEmpty(newReleaseVersion)
                    && StringUtils.isEmpty(currentReleaseVersion)) {
                // didn't get a release version this time or last time
                LOG.error(msg);
                continue;
            }

            // if release version is different
            if (StringUtils.isEmpty(newReleaseVersion)
                    || StringUtils.isEmpty(currentReleaseVersion)
                    || !newReleaseVersion.equals(currentReleaseVersion)
                    || DEBUG) {

                // update release version
                mine.setReleaseVersion(newReleaseVersion);

                // update orthologues
                FriendlyMineQueryRunner.updateRemoteMine(mine);
            }
        }
    }

    private static void updateRemoteMine(Mine mine) {
        // query for which organisms are available;
        if (!FriendlyMineQueryRunner.setValues(mine)) {
            LOG.warn("No organisms found for " + mine.getName());
            return;
        }
        // query for which orthologues are available
        FriendlyMineQueryRunner.setMaps(mine);
    }

    // sets available values for Mine, eg. organisms
    private static boolean setValues(Mine mine) {
        Set<String> names = new HashSet<String>();
        String webserviceURL = null;
        try {
            webserviceURL = mine.getUrl() + VALUES_URL + WILDCARD;
            BufferedReader reader = runWebServiceQuery(webserviceURL);
            if (reader == null) {
                LOG.info("no values found for " + mine.getName());
                return false;
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotEmpty(line)) {
                    names.add(line);
                }
            }
        } catch (Exception e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return false;
        }

        mine.setMineValues(names);
        if (!names.isEmpty()) {
            return true;
        }
        LOG.info("no values found for " + mine.getName());
        return false;
    }

    // set key value pairs representing data in a mine
    private static void setMaps(Mine mine) {
        Map<String, Set<String>> mineMap = new HashMap<String, Set<String>>();
        String url = null;
        try {
            url = mine.getUrl() + MAP_URL + WILDCARD;
            BufferedReader reader = runWebServiceQuery(url);
            if (reader == null) {
                LOG.info("no results found for " + mine.getName() + " for " + url);
                return;
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] bits = line.split("\\t");
                if (bits.length != 3) {
                    String msg = "Couldn't process orthologue links for " + mine.getName()
                        + ".  Expected three columns, found " + bits.length + " columns instead."
                        + url;
                    LOG.info(msg);
                    return;
                }
                String key = bits[0];
//                String dataSet = bits[1];
                String value = bits[2];
                if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value)) {
                    Util.addToSetMap(mineMap, key, value);
                }
            }
        } catch (Exception e) {
            LOG.error("Unable to access " + mine.getName() + " at " + url);
        }

        // adds orthologues for this remote mine
        // merging with any matching orthologues in the local mine
        mine.setMineMap(mineMap);
        if (mineMap.isEmpty()) {
            LOG.info("no data found for " + mine.getName());
        }
    }

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

    /**
     * Query a mine and recieve map of results.  only processes first two columns set as id and
     * name
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
                + URLEncoder.encode(xmlQuery, "UTF-8");
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader;
        } catch (Exception e) {
            LOG.info("Unable to access " + mine.getName() + " exception: " + e.getMessage());
            return null;
        }
    }
}

