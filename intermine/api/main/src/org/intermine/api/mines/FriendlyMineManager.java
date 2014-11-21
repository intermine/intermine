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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.beans.PartnerLink;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;
import org.json.JSONObject;

/**
 * Class to manage friendly mines
 *
 * @author Julie Sullivan
 */
public class FriendlyMineManager {
    private static final boolean DEBUG = false;
    private static FriendlyMineManager linkManager = null;
    private static final Logger LOG = Logger.getLogger(FriendlyMineManager.class);
    private static Map<String, Mine> mines = new HashMap<String, Mine>();
    private static LocalMine localMine = null;
    private static Properties webProperties;
    private static InterMineAPI im;
    private static Map<MultiKey, Collection<PartnerLink>> intermineLinkCache
        = new CacheMap<MultiKey, Collection<PartnerLink>>();


    /**
     * @param interMineAPI
     *            intermine api
     * @param props
     *            the web properties
     */
    public FriendlyMineManager(InterMineAPI interMineAPI, Properties props) {
        webProperties = props;
        im = interMineAPI;
        localMine = new LocalMine(im, webProperties);
        mines.putAll(readConfig());
    }

    /**
     * Used in Ajax requests
     * 
     * @return InterMineAPI used
     */
    public InterMineAPI getInterMineAPI() {
        return im;
    }

    /**
     * @param imAPI
     *            intermine api
     * @param properties
     *            the web properties
     * @return OrthologueLinkManager the link manager
     */
    public static synchronized FriendlyMineManager getInstance(
            InterMineAPI imAPI, Properties properties) {
        if (linkManager == null || DEBUG) {
            linkManager = new FriendlyMineManager(imAPI, properties);
        }
        return linkManager;
    }

    /**
     * Return a list of Mines listed in config. Used for intermine links on
     * report pages.
     *
     * This collection does not include the local instance.
     *
     * @return Collection of all friendly mines listed in config
     */
    public Collection<Mine> getFriendlyMines() {
        Set<Mine> ret = new HashSet<Mine>(mines.values());
        ret.remove(localMine);
        return ret;
    }

    /**
     * @return the local mine
     */
    public Mine getLocalMine() {
        return localMine;
    }

    /**
     * @param key
     *            mine + identifier + organism
     * @return homologues for this key combo
     */
    public Collection<PartnerLink> getLinks(MultiKey key) {
        return intermineLinkCache.get(key);
    }

    /**
     * @param key
     *            mine + identifier + organism
     * @param results
     *            homologues for this key combo
     */
    public void cacheLinks(MultiKey key, Collection<PartnerLink> results) {
        intermineLinkCache.put(key, results);
    }

    /**
     * @param imAPI intermine API
     */
    private Map<String, ConfigurableMine> readConfig() {
        int timeout = Integer.parseInt(webProperties.getProperty("friendlymines.requests.timeout"), 10);
        int refreshInterval = Integer.parseInt(webProperties.getProperty("friendlymines.refresh.interval"), 10);
        MineRequester httpRequester = new HttpRequester(timeout);
        Map<String, ConfigurableMine> mines = new LinkedHashMap<String, ConfigurableMine>();
        mines.put(localMine.getID(), localMine);

        Properties props = PropertiesUtil.stripStart("intermines",
                PropertiesUtil.getPropertiesStartingWith("intermines",
                        webProperties));

        Enumeration<?> propNames = props.propertyNames();

        while (propNames.hasMoreElements()) {
            String mineId = (String) propNames.nextElement();
            mineId = mineId.substring(0, mineId.indexOf("."));
            Properties mineProps = PropertiesUtil.stripStart(mineId,
                    PropertiesUtil.getPropertiesStartingWith(mineId, props));

            ConfigurableMine mine = mines.get(mineId);
            if (mine == null) {
                mine = new RemoteMine(mineId, httpRequester, refreshInterval);
            }
            try {
                mine.configure(mineProps);
                mines.put(mineId, mine);
                mines.put(mine.getName(), mine);
            } catch (ConfigurationException e) {
                LOG.error("Bad configuration for " + mineId, e);
                continue;
            }
        }
        return mines;
    }

    /**
     * @param mineName
     *            name of mine
     * @return The mine properties object.
     */
    public Mine getMine(String mineName) {
        if (mineName == null) {
            throw new NullPointerException("mineName must not be null");
        }
        return mines.get(mineName);
    }

}
