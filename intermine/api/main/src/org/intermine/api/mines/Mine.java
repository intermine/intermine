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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an instance of an InterMine.  Contains generic data structures to populate with
 * queryable values.
 *
 * @author Julie Sullivan
 */
public class Mine
{
//    private static final Logger LOG = Logger.getLogger(Mine.class);
    protected String name = null;
    protected String url = null;
    protected String logo = null;
    protected String bgcolor, frontcolor;
    protected Set<String> defaultValues = new HashSet<String>();
    protected String releaseVersion = null;
    // holds a set of values available to query for this mine
    private Set<String> mineValues;
    // holds a map of values available to query for this mine, eg. dept --> employee,gene --> ortho
    private Map<String, Set<String>> mineMap;

    /**
     * Constructor
     *
     * @param name name of mine, eg FlyMine
     */
    public Mine(String name) {
        this.name = name;
    }

    /**
     * @return the name of the mine
     */
    public String getName() {
        return name;
    }

    /**
     * @return the url to the mine
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the logo
     */
    public String getLogo() {
        return logo;
    }

    /**
     * @param logo the logo to set
     */
    public void setLogo(String logo) {
        this.logo = logo;
    }

    /**
     * @return bgcolor
     */
    public String getBgcolor() {
        return bgcolor;
    }

    /**
     * @param bgcolor background color
     */
    public void setBgcolor(String bgcolor) {
        this.bgcolor = bgcolor;
    }

    /**
     * @return frontcolor
     */
    public String getFrontcolor() {
        return frontcolor;
    }

    /**
     * @param frontcolor front color
     */
    public void setFrontcolor(String frontcolor) {
        this.frontcolor = frontcolor;
    }

    /**
     * @return the mineValues
     */
    public Set<String> getMineValues() {
        return mineValues;
    }

    /**
     * @param mineValues the mineValues to set
     */
    public void setMineValues(Set<String> mineValues) {
        this.mineValues = mineValues;
    }

    /**
     * @return true if this mine has queryable values
     */
    public boolean hasValues() {
        return mineValues != null && !mineValues.isEmpty();
    }

    /**
     * @return the mineMap
     */
    public Map<String, Set<String>> getMineMap() {
        return mineMap;
    }

    /**
     * @param mineMap the mineMap to set
     */
    public void setMineMap(Map<String, Set<String>> mineMap) {
        this.mineMap = mineMap;
    }

    /**
     * @return the releaseVersion
     */
    public String getReleaseVersion() {
        return releaseVersion;
    }
    /**
     * @param releaseVersion the releaseVersion to set
     */
    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    /**
     * @return the defaultValue
     */
    public Set<String> getDefaultValues() {
        return defaultValues;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        if (defaultValues.isEmpty()) {
            return null;
        }
        Object[] values = defaultValues.toArray();
        return values[0].toString();
    }


    /**
     * @param defaultValue the defaultValues to set, comma delim
     */
    public void setDefaultValues(String defaultValue) {
        String[] bits = defaultValue.split(",");
        for (String bit : bits) {
            defaultValues.add(bit);
        }
    }

    /**
     * Search through the map held by this mine for matching values.  eg. look in the map for
     * entries with values equal to the organism name provided.
     *
     * For a Dmel list, finds in this mine:
     *
     *   D. rerio --> Dmel
     *
     * @param remoteKeys keys from other mine
     * @param values values to query for
     * @return list of keys for values provided
     */
    public Set<String> getMatchingMapKeys(Set<String> remoteKeys, List<String> values) {
        if (mineMap != null && !mineMap.isEmpty()) {
            Set<String> results = new HashSet<String>();
            for (Map.Entry<String, Set<String>> entry : mineMap.entrySet()) {
                String key = entry.getKey();
                Set<String> currentMineValues = entry.getValue();
                for (String otherMineValue : values) {
                    if (currentMineValues.contains(otherMineValue)) {
                        if (remoteKeys == null || remoteKeys.contains(key)) {
                            results.add(key);
                        }
                    }
                }
            }
            return results;
        }
        return Collections.emptySet();
    }

    /**
     * finds Dmel (organism in list) --> D. rerio (organism for remote mine)
     *
     * @param remoteKeys keys for remote mine
     * @param values values to test for
     * @return list of values (organisms)
     */
    public Set<String> getMatchingMapValues(Set<String> remoteKeys, List<String> values) {
        if (mineMap != null && !mineMap.isEmpty()) {
            Set<String> results = new HashSet<String>();
            for (String value : values) {
                Set<String> localValues = mineMap.get(value);
                if (localValues != null) {
                    for (String key : remoteKeys) {
                        if (localValues.contains(key)) {
                            results.add(key);
                        }
                    }
                }
            }
            return results;
        }
        return Collections.emptySet();
    }

}
