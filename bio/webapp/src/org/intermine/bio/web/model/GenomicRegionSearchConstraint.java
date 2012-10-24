package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Set;

/**
 * A class to represent the constraints a user selected including a list of features and genomic
 * regions, etc.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSearchConstraint
{
    private String orgName = null;
    private Set<Class<?>> featureTypes = null;
    private List<GenomicRegion> genomicRegionList = null;
    private int extendedRegionSize = 0;

    // TODO add liftOver contraints: org, source, target

    /**
     * @return the orgName
     */
    public String getOrgName() {
        return orgName;
    }
    /**
     * @param orgName the orgName to set
     */
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    /**
     * @return the feature types to search for
     */
    public Set<Class<?>> getFeatureTypes() {
        return featureTypes;
    }
    /**
     * @param featureTypes the feature types to search for
     */
    public void setFeatureTypes(Set<Class<?>> featureTypes) {
        this.featureTypes = featureTypes;
    }

    /**
     * @return the genomicRegionList
     */
    public List<GenomicRegion> getGenomicRegionList() {
        return genomicRegionList;
    }
    /**
     * @param genomicRegionList the genomicRegionList to set
     */
    public void setGenomicRegionList(List<GenomicRegion> genomicRegionList) {
        this.genomicRegionList = genomicRegionList;
    }

    /**
     * @return the extendedRegionSize
     */
    public int getExtendedRegionSize() {
        return extendedRegionSize;
    }
    /**
     * @param extededRegionSize the extendedRegionSize to set
     */
    public void setExtededRegionSize(int extededRegionSize) {
        this.extendedRegionSize = extededRegionSize;
    }

    /**
     * @param obj a GenomicRegionSearchConstraint object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenomicRegionSearchConstraint) {
            GenomicRegionSearchConstraint c = (GenomicRegionSearchConstraint) obj;
            return (extendedRegionSize == c.getExtendedRegionSize()
                    && genomicRegionList.equals(c.getGenomicRegionList())
                    && featureTypes.equals(c.getFeatureTypes())
                    && orgName.equals(c.getOrgName()));
        }
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return extendedRegionSize + genomicRegionList.hashCode() + featureTypes.hashCode()
            + orgName.hashCode();
    }

}
