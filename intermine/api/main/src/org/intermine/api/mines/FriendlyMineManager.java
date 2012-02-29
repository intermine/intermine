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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;
import org.intermine.webservice.client.core.ServiceFactory;
import org.json.JSONObject;

/**
 * Class to manage friendly mines
 *
 * @author Julie Sullivan
 */
public class FriendlyMineManager
{
    private static final boolean DEBUG = false;
    private static boolean cached = false;
    private static FriendlyMineManager linkManager = null;
    private static long lastCacheRefresh = 0;
    private static final long ONE_HOUR = 3600000;
    private static final Logger LOG = Logger.getLogger(FriendlyMineManager.class);
    private static Map<String, Mine> mines = null;
    private static Mine localMine = null;
    private static Properties webProperties;
    private static InterMineAPI im;
    private static Map<MultiKey, Collection<JSONObject>> intermineLinkCache
        = new CacheMap<MultiKey, Collection<JSONObject>>();

/**
 * @param interMineAPI intermine api
 * @param props the web properties
 */
    public FriendlyMineManager(InterMineAPI interMineAPI, Properties props) {
        webProperties = props;
        im = interMineAPI;
        final String localMineName = webProperties.getProperty("project.title");
        localMine = new Mine(localMineName);
        mines = readConfig(im, localMineName);
    }

    /**
     * Used in Ajax requests
     * @return InterMineAPI used
     */
    public InterMineAPI getInterMineAPI() {
        return im;
    }

    /**
     * @param im intermine api
     * @param properties the web properties
     * @return OrthologueLinkManager the link manager
     */
    public static synchronized FriendlyMineManager getInstance(InterMineAPI im,
            Properties properties) {
        if (linkManager == null || DEBUG) {
            linkManager = new FriendlyMineManager(im, properties);
            primeCache();
        }
        return linkManager;
    }

    /**
     * Return a list of Mines listed in config.  Used for intermine links on report pages.
     *
     * @return Collection of all friendly mines listed in config
     */
    public Collection<Mine> getFriendlyMines() {
        return mines.values();
    }

    /**
     * @return the local mine
     */
    public Mine getLocalMine() {
        return localMine;
    }

    /**
     * if an hour has passed, update data
     */
    public static synchronized void primeCache() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastCacheRefresh;
        if (timeSinceLastRefresh > ONE_HOUR || !cached || DEBUG) {
            lastCacheRefresh = System.currentTimeMillis();
            cached = true;
            FriendlyMineQueryRunner.updateReleaseVersion(mines);
            // FIXME there is a delay when using the client, See #2829
//            for (Mine mine : mines.values()) {
//                String webserviceURL = mine.getUrl() + "/service";
//                ServiceFactory services = new ServiceFactory(webserviceURL);
//                String currentReleaseVersion = mine.getReleaseVersion();
//                String newReleaseVersion = null;
//                try {
//                    newReleaseVersion = services.getQueryService().getRelease();
//                } catch (Exception e) {
//                    final String msg = "Unable to retrieve release version for " + mine.getName();
//                    LOG.warn(msg);
//                    continue;
//                }
//
//                if (StringUtils.isEmpty(newReleaseVersion)
//                        && StringUtils.isEmpty(currentReleaseVersion)) {
//                    // didn't get a release version this time or last time
//                    final String msg = "Unable to retrieve release version for " + mine.getName();
//                    LOG.warn(msg);
//                    continue;
//                }
//
//                // if release version is different
//                if (StringUtils.isEmpty(newReleaseVersion)
//                        || StringUtils.isEmpty(currentReleaseVersion)
//                        || !newReleaseVersion.equals(currentReleaseVersion)
//                        || DEBUG) {
//
//                    // update release version
//                    mine.setReleaseVersion(newReleaseVersion);
//
//                    intermineLinkCache = new HashMap<MultiKey, Collection<JSONObject>>();
//                }
//            }
        }
    }

    /**
     * @param key mine + identifier + organism
     * @return homologues for this key combo
     */
    public Collection<JSONObject> getLink(MultiKey key) {
        return intermineLinkCache.get(key);
    }

    /**
     * @param key mine + identifier + organism
     * @param results homologues for this key combo
     */
    public void addLink(MultiKey key, Collection<JSONObject> results) {
        intermineLinkCache.put(key, results);
    }

    private Map<String, Mine> readConfig(InterMineAPI im, String localMineName) {
        mines = new LinkedHashMap<String, Mine>();
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
            String defaultValues = mineProps.getProperty("defaultValues");
            String bgcolor = mineProps.getProperty("bgcolor");
            String frontcolor = mineProps.getProperty("frontcolor");

            if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(url)) {
                final String msg = "InterMine configured incorrectly in web.properties.  "
                        + "Cannot generate friendly mine linkouts: " + mineId;
                LOG.error(msg);
                continue;
            }

            if (mineName.equals(localMineName)) {
                if (localMine.getUrl() == null) {
                    parseLocalConfig(url, logo, defaultValues, bgcolor, frontcolor);
                }
            } else {
                Mine mine = mines.get(mineId);
                if (mine == null) {
                    parseRemoteConfig(mineName, mineId, defaultValues, url, logo, bgcolor,
                            frontcolor);
                }
            }
        }
        return mines;
    }

    private void parseLocalConfig(String url, String logo, String defaultValues,
            String bgcolor, String frontcolor) {
        if (localMine.getUrl() == null) {
            localMine.setUrl(url);
            localMine.setLogo(logo);
            localMine.setBgcolor(bgcolor);
            localMine.setFrontcolor(frontcolor);
            localMine.setDefaultValues(defaultValues);
        }
    }

    private void parseRemoteConfig(String mineName, String mineId, String defaultValues,
            String url, String logo, String bgcolor, String frontcolor) {
        Mine mine = new Mine(mineName);
        mine.setUrl(url);
        mine.setLogo(logo);
        mine.setBgcolor(bgcolor);
        mine.setFrontcolor(frontcolor);
        mine.setDefaultValues(defaultValues);
        mines.put(mineId, mine);
    }

    /**
     * @param mineName name of mine
     * @return mine
     */
    public Mine getMine(String mineName) {
        for (Mine mine : mines.values()) {
            if (mine.getName().equals(mineName)) {
                return mine;
            }
        }
        return null;
    }
}


