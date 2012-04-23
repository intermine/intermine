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

import java.util.HashSet;
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
     * get first default value.  used in querybuilder to select default extra value
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
}
