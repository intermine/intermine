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
     * Test whether this Mine has genes
     *
     * @return true if the Mine has genes
     */
    public boolean hasGenes() {
        return !organismNames.isEmpty();
    }

    /**
     * Return map of map, gene.organism --> gene.homologue.organism --> datasets
     * @return the orthologues
     */
    public Map<String, Map<String, HomologueMapping>> getOrthologues() {
        return geneToOrthologues;
    }

    /**
     * Set map of map, gene.organism --> gene.homologue.organism --> datasets.  Also add matching
     * orthologues in the localMine.
     *
     * @param orthologues the orthologues to set
     * @param localMine the mine where the user is
     */
    public void setOrthologues(Map<String, Map<String, HomologueMapping>> orthologues,
            Mine localMine) {
        this.geneToOrthologues = orthologues;
        // merge the orthologues from the local mine with this one
        if (localMine != null) {
            mergeLocalOrthologues(localMine);
        }
    }

    /**
     * Add the orthologues from the local mine to this mine, only for organisms that are present
     * in this (remote) mine.
     *
     * @param localMine the local mine - where the user is
     */
    private void mergeLocalOrthologues(Mine localMine) {

        // gene --> gene.orthologue --> datasets
        Map<String, Map<String, HomologueMapping>> localOrthologues = localMine.getOrthologues();

        if (localOrthologues.isEmpty()) {
            // no local orthologues, nothing to merge
            return;
        }

        // list of organisms for which remote mine has genes.
        Set<String> remoteGeneOrganisms = getOrganisms();

        // does the local mine have orthologues for gene.organism in remote mine?
        for (Map.Entry<String, Map<String, HomologueMapping>> entry : localOrthologues.entrySet()) {

            // C. elegans
            String localGeneOrganism = entry.getKey();
            // C. elegans
            if (remoteGeneOrganisms.contains(localGeneOrganism)) {
                swap(localGeneOrganism, entry.getValue());
            }
        }
    }

    /**
     * Swaps around the map to be used in the remote mine.  This is just done for easier lookups
     * by the webapp.
     *
     *  Original map looks like this:
     *
     *      [A] --> [B]
     *
     *  A = gene in remote mine [geneOrganism]
     *  B = gene in local mine  [homologueOrganism]
     *
     * The webapp code will have a bag of type [B].  We need to swap this around to what the webapp
     * expects.  The goal is to convert the genes to type [A].
     *
     *      [B] --> [A]
     *
     */
    private void swap(String geneOrganism, Map<String, HomologueMapping> mapping) {
        for (Map.Entry<String, HomologueMapping> entry : mapping.entrySet()) {
            String homologueOrganism = entry.getKey();
            HomologueMapping mapCopy = new HomologueMapping(entry.getValue());
            merge(homologueOrganism, newMap(geneOrganism, mapCopy));
        }
    }

    /**
     * Merge orthologues from local mine to list of orthologues available for this mine.
     *
     * @param geneOrganism gene.organism
     * @param localOrthologues mapping from gene.homologue.organism --> datasets
     */
    private void merge(String geneOrganism, Map<String, HomologueMapping> localOrthologues) {

        // what this mine has already for this organism
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
     *
     * @param organisms list of organisms in list
     * @return mapping
     */
    public Map<String, HomologueMapping> getRelevantHomologues(Collection<String> organisms) {

        // [gene -->] orthologue --> datasets
        Map<String, HomologueMapping> homologuesForList = new HashMap<String, HomologueMapping>();

        // get all gene --> homologue entries
        for (Map.Entry<String, Map<String, HomologueMapping>> entry
                : geneToOrthologues.entrySet()) {

            // gene
            String geneOrganismName = entry.getKey();
            // homologue --> datasets
            Map<String, HomologueMapping> orthologuesToDatasets = entry.getValue();

            // gene.organism is in user's bag
            if (organisms.contains(geneOrganismName)) {

                // for every gene.homologue, add to list and add datasets
                for (Map.Entry<String, HomologueMapping> orthologueEntry
                        : orthologuesToDatasets.entrySet()) {

                    // gene.orthologue.organism.name
                    String orthologueOrganismName = orthologueEntry.getKey();
                    // gene.orthologue.organism --> dataSets
                    HomologueMapping homologueMapping = orthologueEntry.getValue();

                    //  create list for display on JSP
                    addHomologues(homologuesForList, orthologueOrganismName, homologueMapping);

                }
            }
        }
        return homologuesForList;
    }

    /* add homologues to map used on JSP to display to user*/
    // TODO this method can be merged with merge() and OLM.addToMap
    private static void addHomologues(Map<String, HomologueMapping> homologuesForList,
            String orthologueOrganismName, HomologueMapping homologueMapping) {

        // this mapping is used by the JSP to print out the homologues to the user
        HomologueMapping mappingForThisList = homologuesForList.get(orthologueOrganismName);

        if (mappingForThisList == null) {
            mappingForThisList = new HomologueMapping(orthologueOrganismName);
            homologuesForList.put(orthologueOrganismName, mappingForThisList);
        }

        // add datasets for display
        mappingForThisList.addRemoteDataSets(homologueMapping.getRemoteDataSets());
        mappingForThisList.addLocalDataSets(homologueMapping.getLocalDataSets());
    }

    private Map<String, HomologueMapping> newMap(String geneOrganism,
            HomologueMapping newMapping) {
        newMapping.setOrganism(geneOrganism);
        Map<String, HomologueMapping> newMap = new HashMap();
        newMap.put(geneOrganism, newMapping);
        return newMap;
    }
}
