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
    private Set<String> organismNames = new HashSet<String>();

    // gene.organism --> gene.orthologue.organism --> gene.orthologue.datasets
    private Map<String, Map<String, HomologueMapping>> geneToOrthologues
        = new HashMap<String, Map<String, HomologueMapping>>();

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
     * Return map of map, gene.organism --> gene.homologue.organism --> datasets
     * @return the orthologues
     */
    public Map<String, Map<String, HomologueMapping>> getOrthologues() {
        return geneToOrthologues;
    }

    /**
     * Set map of map, gene.organism --> gene.homologue.organism --> datasets
     * @param orthologues the orthologues to set
     */
    public void setOrthologues(Map<String, Map<String, HomologueMapping>> orthologues) {
        this.geneToOrthologues = orthologues;
    }

    /**
     * Merge orthologues from local mine to list of orthologues available for this mine.
     *
     * @param geneOrganism gene.organism
     * @param localOrthologues mapping from gene.homologue.organism --> datasets
     */
    public void addLocalOrthologues(String geneOrganism,
            Map<String, HomologueMapping> localOrthologues) {

        // what this mine has already
        Map<String, HomologueMapping> orthologues = geneToOrthologues.get(geneOrganism);

        // -- ADD ALL --
        if (orthologues == null) {
            // mine didn't have any orthologues for this gene.organism, add all
            geneToOrthologues.put(geneOrganism, localOrthologues);
            return;
        }

        // -- MERGE --
        // this mine already has orthologues for this organism, so merge results
        for (Map.Entry<String, HomologueMapping> entry : localOrthologues.entrySet()) {

            // homologue data for local mine
            String localHomologueOrganism = entry.getKey();
            HomologueMapping localOrthologueMapping = entry.getValue();

            // merge local mine homologues with current mine's homologues
            HomologueMapping mergedOrthologues = orthologues.get(localHomologueOrganism);
            if (mergedOrthologues == null) {
                mergedOrthologues = new HomologueMapping(localHomologueOrganism);
                orthologues.put(localHomologueOrganism, mergedOrthologues);
            }
            mergedOrthologues.addLocalDataSets(localOrthologueMapping.getLocalDataSets());
        }
    }

    /**
     * Test mine has gene.orthologues for organism.
     * @param shortName name to test
     * @return TRUE if this mine has orthologues for this organism
     */
    public boolean validOrganism(String shortName) {
        return geneToOrthologues.containsKey(shortName);
    }
}
