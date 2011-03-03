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
    protected String name = null;
    protected String url = null;
    protected String logo = null;
    protected String defaultValue = null, defaultMapping = null;
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
     * Which mapping to use, either remote or local
     *
     * @return the defaultMapping
     */
    public String getDefaultMapping() {
        return defaultMapping;
    }

    /**
     * remote/local
     * @param defaultMapping the defaultMapping to set
     */
    public void setDefaultMapping(String defaultMapping) {
        this.defaultMapping = defaultMapping;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

}
