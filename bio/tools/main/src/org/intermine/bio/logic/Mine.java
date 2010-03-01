package org.intermine.bio.logic;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an instance of an InterMine
 * @author julie
 */
public class Mine
{
    private String name = null;
    private String url = null;
    private String logo = null;
    private String defaultOrganismName = null, defaultMapping = null;
    private String releaseVersion = null;
    private Set<String> organismNames = new HashSet();

    // gene.organismName --> gene.orthologue.organismName --> local/remote
    // --> gene.orthologue.datasets
    private Map<String, Map<String, Set[]>> orthologues = new HashMap();

    /**
     * Constructor
     * @param name name of mine, eg FlyMine
     */
    public Mine(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the url
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
     * @return the default organism
     */
    public String getDefaultOrganismName() {
        return defaultOrganismName;
    }

    /**
     * @param defaultOrganismName the defaultOrganismName to set
     */
    public void setDefaultOrganismName(String defaultOrganismName) {
        this.defaultOrganismName = defaultOrganismName;
    }

    /**
     * remote/local
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
     * List of organisms for all genes available to query for this mine.  shortnames are used,
     * eg. D. rerio
     * @param organismNames the organism shortNames
     */
    public void setOrganisms(Set<String> organismNames) {
        this.organismNames = organismNames;
    }

    /**
     * shortname format is used, eg. D. melanogaster
     * @return the list of all organisms available to query for this mine
     */
    public Set<String> getOrganisms() {
        return organismNames;
    }

    /**
     * gene organismNames --> homologue organismNames --> dataset
     * @return the map of orthologues
     */
    public Map<String, Map<String, Set[]>> getOrthologues() {
        return orthologues;
    }

    /**
     * map contains gene -> homologue mapping.  the homologue mapping is a map from homologue's
     * taxonId to dataset
     * @param orthologues the map of orthologues
     */
    public void setOrthologues(Map<String, Map<String, Set[]>> orthologues) {
        this.orthologues = orthologues;
    }

    /**
     * test if intermine instance has  gene.orthologues for organism.
     * @param shortName name to test
     * @return TRUE if this mine has orthologues for this organism
     */
    public boolean validOrganism(String shortName) {
        return orthologues.containsKey(shortName);
    }

}
