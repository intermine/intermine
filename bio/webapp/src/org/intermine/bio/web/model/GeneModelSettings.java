package org.intermine.bio.web.model;

public class GeneModelSettings {
    String organism;
    boolean hasGenes = true;
    boolean hasTranscripts = true;
    boolean hasThreePrimeUTRs = true;
    boolean hasFivePrimeUTRs = true;
    boolean hasExons = true;
    boolean hasIntrons = true;
    boolean hasCDSs = true;
    
    public GeneModelSettings(String organism) {
        this.organism = organism;
    }

    public String getOrganism() {
        return organism;
    }

    // weird looking getters for use in JSP
    public boolean getHasGenes() {
        return hasGenes;
    }

    public boolean getHasTranscripts() {
        return hasTranscripts;
    }

    public boolean getHasThreePrimeUTRs() {
        return hasThreePrimeUTRs;
    }

    public boolean getHasFivePrimeUTRs() {
        return hasFivePrimeUTRs;
    }

    public boolean getHasExons() {
        return hasExons;
    }

    public boolean getHasIntrons() {
        return hasIntrons;
    }

    public boolean getHasCDSs() {
        return hasCDSs;
    }
    
}
