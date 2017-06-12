package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.beans.PartnerLink;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;

/**
 * Class to manage friendly mines
 *
 * @author Julie Sullivan
 * @author Alex Kalderimis
 */
public class FriendlyMineManager
{
    private static final String REQUESTER_CONFIG = "friendlymines.requester.config";
    private static final String DEFAULT_REQR = "org.intermine.api.mines.HttpRequester";
    private static final String REQUESTER_IMPL = "friendlymines.requester.impl";

    @SuppressWarnings("unused") private static final boolean DEBUG = false;
    private static final Logger LOG = Logger.getLogger(FriendlyMineManager.class);
    private static final Map<MultiKey, Collection<PartnerLink>> LINK_CACHE
        = new CacheMap<MultiKey, Collection<PartnerLink>>();
    private static final Map<InterMineAPI, FriendlyMineManager> INSTANCE_MAP
        = new CacheMap<InterMineAPI, FriendlyMineManager>();

    private final Map<String, Mine> mines = new HashMap<String, Mine>();
    private final LocalMine localMine;
    private final Properties webProperties;
    private final InterMineAPI im;
    private final MineRequester requester;

    /**
     * @param interMineAPI
     *            intermine api
     * @param props
     *            the web properties
     * @param reqr
     *            the requester used by remote mine implementations.
     */
    FriendlyMineManager(InterMineAPI interMineAPI, Properties props, MineRequester reqr) {
        webProperties = props;
        im = interMineAPI;
        localMine = new LocalMine(im, webProperties);
        requester = reqr;
        mines.putAll(readConfig());
    }

    /**
     * Get an instance of FriendlyMineManager.
     *
     * This method caches the instances, keyed against the InterMineAPI they were constructed
     * with. Subsequent invocations will return the same instance, providing that it hasn't already
     * been reaped.
     *
     * @param api InterMine api
     * @param properties the web properties
     * @return an instance of FriendlyMineManager
     */
    public static synchronized
    FriendlyMineManager getInstance(InterMineAPI api, Properties properties) {
        if (!INSTANCE_MAP.containsKey(api)) {
            MineRequester r = TypeUtil.createNew(
                    properties.getProperty(REQUESTER_IMPL, DEFAULT_REQR));
            INSTANCE_MAP.put(api, new FriendlyMineManager(api, properties, r));
        }
        return INSTANCE_MAP.get(api);
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
        return LINK_CACHE.get(key);
    }

    /**
     * @param key
     *            mine + identifier + organism
     * @param results
     *            homologues for this key combo
     */
    public void cacheLinks(MultiKey key, Collection<PartnerLink> results) {
        LINK_CACHE.put(key, results);
    }

    /**
     * @param imAPI intermine API
     */
    private Map<String, ConfigurableMine> readConfig() {
        int refreshInterval = getIntProperty("friendlymines.refresh.interval", 3600);
        Properties requesterConfig =
                PropertiesUtil.stripStart(REQUESTER_CONFIG, webProperties);
        requester.configure(requesterConfig);

        Map<String, ConfigurableMine> newMines = new LinkedHashMap<String, ConfigurableMine>();
        newMines.put(localMine.getID(), localMine);

        Properties props = PropertiesUtil.stripStart("intermines",
                PropertiesUtil.getPropertiesStartingWith("intermines",
                        webProperties));

        Enumeration<?> propNames = props.propertyNames();

        while (propNames.hasMoreElements()) {
            String mineId = (String) propNames.nextElement();
            mineId = mineId.substring(0, mineId.indexOf("."));
            Properties mineProps = PropertiesUtil.stripStart(mineId,
                    PropertiesUtil.getPropertiesStartingWith(mineId, props));

            ConfigurableMine mine = newMines.get(mineId);
            if (mine == null) {
                mine = new RemoteMine(mineId, requester, refreshInterval);
            }
            try {
                mine.configure(mineProps);
                newMines.put(mineId, mine);
                newMines.put(mine.getName(), mine);
            } catch (ConfigurationException e) {
                LOG.error("Bad configuration for " + mineId, e);
                continue;
            }
        }
        return newMines;
    }

    private int getIntProperty(String propName, int defaultValue) {
        if (webProperties.containsKey(propName)) {
            return Integer.parseInt(getProperty(propName), 10);
        } else {
            return defaultValue;
        }
    }

    private String getProperty(String propName) {
        return webProperties.getProperty(propName);
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
