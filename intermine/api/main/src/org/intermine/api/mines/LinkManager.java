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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.Util;

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
public class LinkManager
{
    private static final boolean DEBUG = true;
    private static boolean cached = false;
    private static LinkManager linkManager = null;
    private static long lastCacheRefresh = 0;
    private static final long ONE_HOUR = 3600000;
    private static final Logger LOG = Logger.getLogger(LinkManager.class);
    private static Map<String, Mine> mines = null;
    private static Mine localMine = null;
    private static final String WEBSERVICE_URL = "/service";
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static String valuesTemplate, mapTemplate, reportTemplate1, reportTemplate2,
    identifierConstraint, lookupConstraint, constraint1, constraint2;
    private static String valuesURL, mapURL;
    private static final String TEMPLATE_PATH = "/template/results?size=1000&format=tab&name=";
    private static Properties webProperties;
    private static final String WILDCARD = "*";

/**
 * @param im intermine api
 * @param webProperties the web properties
 */
    public LinkManager(InterMineAPI im, Properties webProperties) {
        LinkManager.webProperties = webProperties;

        // TODO put in constants
        valuesTemplate = (String) webProperties.get("intermine.template.queryableValues");
        mapTemplate = (String) webProperties.get("intermine.template.queryableMap");
        identifierConstraint
            = (String) webProperties.get("intermine.template.identifierConstraint");
        lookupConstraint = (String) webProperties.get("intermine.template.lookupConstraint");
        constraint1 = (String) webProperties.get("intermine.template.constraint1");
        constraint2 = (String) webProperties.get("intermine.template.constraint2");
        reportTemplate1 = (String) webProperties.get("intermine.template.report.otherMines");
        reportTemplate2 = (String) webProperties.get("intermine.template.report.relatedData");

        valuesURL = WEBSERVICE_URL + TEMPLATE_PATH + valuesTemplate + identifierConstraint;
        mapURL = WEBSERVICE_URL + TEMPLATE_PATH + mapTemplate + identifierConstraint;

        final String localMineName = webProperties.getProperty("project.title");
        localMine = new Mine(localMineName);
        mines = readConfig(im, localMineName);
    }

    /**
     * @param im intermine api
     * @param properties the web properties
     * @return OrthologueLinkManager the link manager
     */
    public static synchronized LinkManager getInstance(InterMineAPI im, Properties properties) {
        if (linkManager == null || DEBUG) {
            linkManager = new LinkManager(im, properties);
            primeCache();
        }
        return linkManager;
    }

    /**
     * Return a list of Mines listed in config.  Used for intermine links on report pages.
     *
     * @return Collection of all friendly mines listed in config
     */
    public static Collection<Mine> getFriendlyMines() {
        return mines.values();
    }

    /**
     * REPORT PAGE
     *
     * Returns list of friendly mines that contain value of interest.  Used for
     * the links on the report page.
     *
     * @param constraintValue optional additional constraint, eg. organism
     * @param identifier identifier to query
     * @return the list of valid mines for the given list
     */
    public Map<Mine, String> getObjectInOtherMines(String constraintValue, String identifier) {
        Map<Mine, String> filteredMines = new HashMap<Mine, String>();
        for (Mine mine : mines.values()) {
//            if (!mine.hasValues()) {
//                LOG.warn("mine " + mine.getName() + " has no genes");
//                continue;
//            }
            String newIdentifier = runReportQuery(mine, constraintValue, identifier);
            if (!StringUtils.isEmpty(newIdentifier)) {
                filteredMines.put(mine, newIdentifier);
            }
        }
        return filteredMines;
    }

    private String runReportQuery(Mine mine, String constraintValue, String identifier) {
        final String webserviceURL = mine.getUrl() + WEBSERVICE_URL + TEMPLATE_PATH
            + reportTemplate1 + lookupConstraint + identifier + constraint2 + constraintValue;
        try {
            BufferedReader reader = runWebServiceQuery(webserviceURL);
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
                if (!StringUtils.isEmpty(symbol)) {
                    return symbol;
                }
                if (!StringUtils.isEmpty(newIdentifier)) {
                    return newIdentifier;
                }
            }
        } catch (MalformedURLException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return null;
        } catch (IOException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * used on REPORT page
     *
     * Runs queries and builds data structure for display on report pages.
     *
     * @param identifier identifier for object from report page
     * @param constraintValue optional additonal constraint, eg. organism
     * @return the list of valid mines for the given object
     */
    public Map<Mine, Map<String, Set<String>>> getRelatedData(String constraintValue,
            String identifier) {
        Map<Mine, Map<String, Set<String>>> filteredMines
            = new HashMap<Mine, Map<String, Set<String>>>();

        for (Mine mine : mines.values()) {
//            if (!mine.hasValues() || mine.getMineMap().isEmpty()) {
//                LOG.warn("no links to " + mine.getName() + ".");
//                continue;
//            }
            Map<String, Set<String>> relatedData = runRelatedDataQuery(mine, constraintValue,
                    identifier);
            if (!relatedData.isEmpty()) {
                filteredMines.put(mine, relatedData);
            }
        }
        return filteredMines;
    }

    private Map<String, Set<String>> runRelatedDataQuery(Mine mine, String constraintValue,
            String identifier) {
        Map<String, Set<String>> relatedDataMap = new HashMap<String, Set<String>>();
        final String webserviceURL = mine.getUrl() + WEBSERVICE_URL + TEMPLATE_PATH
            + reportTemplate2 + constraint1 + identifier + constraint2 + constraintValue;
        try {
            BufferedReader reader = runWebServiceQuery(webserviceURL);
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
                if (!StringUtils.isEmpty(symbol)) {
                    Util.addToSetMap(relatedDataMap, key, symbol);
                }
                if (!StringUtils.isEmpty(newIdentifier)) {
                    Util.addToSetMap(relatedDataMap, key, newIdentifier);
                }
            }
        } catch (MalformedURLException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return null;
        } catch (IOException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL, e);
            return null;
        }
        return relatedDataMap;
    }

    /**
     * used on LIST ANALYSIS page
     *
     * For a given mine and list of values, return filtered list of values contained in mine.  eg.
     * for a list of organisms, return the list of organism present in the given mine.
     *
     * Returns NULL if invalid mine or values not found.
     *
     * @param mineName name of mine to test
     * @param values list of values to check for in the mine
     * @return subset of values given that are present in the mine
     */
    public Map<Mine, Set<String>> getMine(String mineName, Collection<String> values) {
        if (StringUtils.isEmpty(mineName) || values == null || values.isEmpty()) {
            return null;
        }
        Mine mine = getMine(mineName);
        if (mine == null) {
            return null;
        }
        Map<Mine, Set<String>> filteredValues = new HashMap<Mine, Set<String>>();
        Map<String, Set<String>> mineMap = mine.getMineMap();
        if (mineMap == null || mineMap.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Set<String>> entry : mineMap.entrySet()) {
            String key = entry.getKey();
            Set<String> mineValues = entry.getValue();
            for (String value : values) {
                if (mineValues.contains(value)) {
                    Util.addToSetMap(filteredValues, mine, key);
                }
            }
        }
        if (filteredValues.isEmpty()) {
            return null;
        }
        return filteredValues;
    }

    private Mine getMine(String mineName) {
        for (Mine mine : mines.values()) {
            if (mine.getName().equals(mineName)) {
                return mine;
            }
        }
        return null;
    }

    /**
     * if an hour has passed, update data
     */
    public static synchronized void primeCache() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastCacheRefresh;
        if ((timeSinceLastRefresh > ONE_HOUR && !cached) || DEBUG) {
            lastCacheRefresh = System.currentTimeMillis();
            cached = true;
            updateMaps();
        }
    }

    /**
     * get release version number for each mine.  if release number is different from the one
     * we have locally, run queries to populate maps
     */
    private static void updateMaps() {
        for (Mine mine : mines.values()) {
            String currentReleaseVersion = mine.getReleaseVersion();
            String url = mine.getUrl() + WEBSERVICE_URL + RELEASE_VERSION_URL;
            BufferedReader reader = runWebServiceQuery(url);
            String newReleaseVersion = null;
            try {
                if (reader != null) {
                    newReleaseVersion = reader.readLine();
                } else {
                    String msg = "Unable to retrieve release version for " + mine.getName();
                    LOG.info(msg);
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            if (StringUtils.isEmpty(newReleaseVersion)
                    && StringUtils.isEmpty(currentReleaseVersion)) {
                // didn't get a release version this time or last time
                String msg = "Unable to retrieve release version for " + mine.getName();
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
                updateRemoteMines(mine);
            }
        }
    }

    private static BufferedReader runWebServiceQuery(String urlString) {
        try {
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader;
        } catch (MalformedURLException e) {
            LOG.info("Unable to access " + urlString);
            return null;
        } catch (IOException e) {
            LOG.info("Unable to access " + urlString);
            return null;
        }
    }

    private static void updateRemoteMines(Mine mine) {

        // query for which organisms are available;
        if (!setValues(mine)) {
            LOG.warn("No organisms found for " + mine.getName());
            return;
        }

        // query for which orthologues are available
        setMaps(mine);
    }

    // loop through properties and get mines' names, URLs and logos
    private static Map<String, Mine> readConfig(InterMineAPI im, String localMineName) {
        mines = new HashMap<String, Mine>();
        Properties props = PropertiesUtil.stripStart("intermines",
                PropertiesUtil.getPropertiesStartingWith("intermines", webProperties));

        Enumeration<?> propNames = props.propertyNames();

        while (propNames.hasMoreElements()) {
            String mineId =  (String) propNames.nextElement();
            mineId = mineId.substring(0, mineId.indexOf("."));
            Properties mineProps = PropertiesUtil.stripStart(mineId,
                    PropertiesUtil.getPropertiesStartingWith(mineId, props));

            String mineName = mineProps.getProperty("name");
            String url = mineProps.getProperty("url");
            String logo = mineProps.getProperty("logo");
            String defaultValue = mineProps.getProperty("defaultValue");
            String mapping = mineProps.getProperty("defaultMapping");

            if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(url)
                    || StringUtils.isEmpty(logo)) {
                String msg = "InterMine configured incorrectly in web.properties.  Cannot generate "
                    + " linkouts: " + mineId;
                LOG.error(msg);
                continue;
            }

            if (mineName.equals(localMineName)) {
                if (localMine.getUrl() == null) {
                    localMine.setUrl(url);
                    localMine.setLogo(logo);
                    setLocalValues(im);
                }
                // skip, this is the local intermine.
                continue;
            }

            Mine mine = mines.get(mineId);
            if (mine == null) {
                mine = new Mine(mineName);
                mine.setUrl(url);
                mine.setLogo(logo);
                mine.setDefaultValue(defaultValue);
                mine.setDefaultMapping(mapping);
                mines.put(mineId, mine);
                LOG.info("processing config for " + mineName);
            }
        }
        return mines;
    }

    private static boolean setValues(Mine mine) {
        Set<String> names = new HashSet<String>();
        String webserviceURL = null;
        try {
            webserviceURL = mine.getUrl() + valuesURL + WILDCARD;
            BufferedReader reader = runWebServiceQuery(webserviceURL);
            String line = null;
            while ((line = reader.readLine()) != null) {
                names.add(line);
            }
        } catch (MalformedURLException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
            return false;
        } catch (IOException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL + " " + e);
            return false;
        }

        mine.setMineValues(names);
        if (!names.isEmpty()) {
            LOG.info("processing values for " + mine.getName());
            return true;
        }
        LOG.info("no values found for " + mine.getName());
        return false;
    }

    private static void setMaps(Mine mine) {
        URL url;
        String webserviceURL = null;
        Map<String, Set<String>> mineMap = new HashMap<String, Set<String>>();
        try {
            webserviceURL = mine.getUrl() + mapURL + WILDCARD;
            url = new URL(webserviceURL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] bits = line.split("\\t");
                if (bits.length != 3) {
                    String msg = "Couldn't process orthologue links for " + mine.getName()
                        + ".  Expected three columns, found " + bits.length + " columns instead."
                        + webserviceURL;
                    LOG.info(msg);
                    return;
                }
                String key = bits[0];
//                String dataSet = bits[1];
                String value = bits[2];

                Util.addToSetMap(mineMap, key, value);
            }
        } catch (MalformedURLException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
        } catch (IOException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
        }
        // adds orthologues for this remote mine
        // merging with any matching orthologues in the local mine

        mine.setMineMap(mineMap);
        if (!mineMap.isEmpty()) {
            LOG.info("processing map for " + mine.getName());
        } else {
            LOG.info("no map found for " + mine.getName());
        }
    }

    // running templates run in setValues() and setMaps() for the local mine
    private static void setLocalValues(InterMineAPI im) {
        TemplateManager templateManager = im.getTemplateManager();
        ProfileManager profileManager = im.getProfileManager();

        // get values associated with this mine (eg. gene.organism)
        String templateName = valuesTemplate;
        TemplateQuery q = templateManager.getGlobalTemplate(templateName);
        if (q == null) {
            LOG.error(templateName + " template not found, unable to process intermine links");
            return;
        }
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        ExportResultsIterator it = executor.execute(q);
        Set<String> results = new HashSet<String>();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            results.add((String) row.get(0).getField());
        }
        localMine.setMineValues(results);

        // get map of values for this mine (eg. gene.organism --> gene.homologue.organism)
        templateName = mapTemplate;
        q = templateManager.getGlobalTemplate(templateName);
        if (q == null) {
            LOG.error(templateName + " template not found, unable to process intermine links");
            return;
        }
        executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        it = executor.execute(q);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            Util.addToSetMap(map, row.get(0).getField(), row.get(2).getField());
        }
        localMine.setMineMap(map);
    }
}

