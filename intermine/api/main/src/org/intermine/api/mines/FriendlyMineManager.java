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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.config.Constants;
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
            FriendlyMineQueryRunner.updateData(mines);
        }
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

            if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(url)
                    || StringUtils.isEmpty(logo)) {
                String msg = "InterMine configured incorrectly in web.properties.  Cannot generate "
                    + " linkouts: " + mineId;
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
            setLocalValues(im);
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

    // running templates run in setValues() and setMaps() for the local mine
    private void setLocalValues(InterMineAPI im) {
        TemplateManager templateManager = im.getTemplateManager();
        ProfileManager profileManager = im.getProfileManager();
        processLocalValues(profileManager, templateManager);
        processLocalMap(profileManager, templateManager);
    }

    // get values associated with this mine (eg. gene.organism)
    private void processLocalValues(ProfileManager profileManager,
            TemplateManager templateManager) {
        String templateName = Constants.VALUES_TEMPLATE;
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
    }

    private void processLocalMap(ProfileManager profileManager,
            TemplateManager templateManager) {
        String templateName = Constants.MAP_TEMPLATE;
        TemplateQuery q = templateManager.getGlobalTemplate(templateName);
        if (q == null) {
            LOG.error(templateName + " template not found, unable to process intermine links");
            return;
        }
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        ExportResultsIterator it = executor.execute(q);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            Util.addToSetMap(map, row.get(0).getField(), row.get(2).getField());
        }
        localMine.setMineMap(map);
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


