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

import org.intermine.util.StringUtil;

/**
 * Represents a homologue mapping instance
 * Map<String, Set<String>[]>
 * gene.orthologue.organism --> Set<String> of datasets
 * @author julie
 */
public class HomologueMapping
{

    private String organism;
    private Set<String> localDataSets = new HashSet<String>();
    private Set<String> remoteDataSets = new HashSet<String>();

    /**
     * @param organism the gene.orthologue.organism
     */
    public HomologueMapping(String organism) {
        this.organism = organism;
    }

    /**
     * Copy Constructor
     *
     * @param mapping the object to clone
     */
    public HomologueMapping(HomologueMapping mapping) {
        this(mapping.getOrganism());
        this.localDataSets = mapping.getLocalDataSets();
        this.remoteDataSets = mapping.getRemoteDataSets();
    }

    /**
     * @return the gene.homologue.organism
     */
    public String getOrganism() {
        return organism;
    }

    /**
     * @param organism the organism to set
     */
    public void setOrganism(String organism) {
        this.organism = organism;
    }

    /**
     * @param dataSet the dataSet from the local intermine to add
     */
    public void addLocalDataSet(String dataSet) {
        localDataSets.add(dataSet);
    }

    /**
     * @param dataSets the dataSets to add
     */
    public void addLocalDataSets(Set<String> dataSets) {
        localDataSets.addAll(dataSets);
    }

    /**
     * @return the list of datasets containing homologues present in local mine
     */
    public Set<String> getLocalDataSets() {
        return localDataSets;
    }

    /**
     * @return the list of datasets as a string
     */
    public String getLocalDataSetsString() {
        return StringUtil.prettyList(localDataSets, true);
    }

    /**
     * @param dataSet the dataSet from the remote intermine to add
     */
    public void addRemoteDataSet(String dataSet) {
        remoteDataSets.add(dataSet);
    }

    /**
     * Used when merging.
     * @param dataSets the dataSet from the remote intermine to add
     */
    public void addRemoteDataSets(Set<String> dataSets) {
        remoteDataSets.addAll(dataSets);
    }

    /**
     * List of datasets that have this gene+homologue pair
     * @return the list of datasets containing homologues present in remote mine
     */
    public Set<String> getRemoteDataSets() {
        return remoteDataSets;
    }

    /**
     * For display on JSP page
     * @return the list of datasets as a string
     */
    public String getRemoteDataSetsString() {
        return StringUtil.prettyList(remoteDataSets, true);
    }
}
