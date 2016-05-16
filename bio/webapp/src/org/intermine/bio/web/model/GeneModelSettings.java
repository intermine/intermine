package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 *
 * @author Fenyuan
 */
public class GeneModelSettings
{
    String organism;
    boolean hasGenes = true;
    boolean hasTranscripts = true;
    boolean hasThreePrimeUTRs = true;
    boolean hasFivePrimeUTRs = true;
    boolean hasExons = true;
    boolean hasIntrons = true;
    boolean hasCDSs = true;

    /**
     * @param organism organism
     */
    public GeneModelSettings(String organism) {
        this.organism = organism;
    }

    /**
     * @return the organism
     */
    public String getOrganism() {
        return organism;
    }

    // weird looking getters for use in JSP
    /**
     * @return true if has genes
     */
    public boolean getHasGenes() {
        return hasGenes;
    }

    /**
     * @return true if has transcripts
     */
    public boolean getHasTranscripts() {
        return hasTranscripts;
    }

    /**
     * @return true if has three ' UTRs
     */
    public boolean getHasThreePrimeUTRs() {
        return hasThreePrimeUTRs;
    }

    /**
     * @return true if has five ' UTRs
     */
    public boolean getHasFivePrimeUTRs() {
        return hasFivePrimeUTRs;
    }

    /**
     * @return true if has exon
     */
    public boolean getHasExons() {
        return hasExons;
    }

    /**
     * @return true if has intron
     */
    public boolean getHasIntrons() {
        return hasIntrons;
    }

    /**
     * @return true if has CDS
     */
    public boolean getHasCDSs() {
        return hasCDSs;
    }

}
