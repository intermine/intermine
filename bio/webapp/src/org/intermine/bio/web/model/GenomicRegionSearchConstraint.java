package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * A class to represent the constraints a user selected including a list of features and spans, etc.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSearchConstraint
{
    private String orgName = null;
    @SuppressWarnings("rawtypes")
    private List<Class> ftList = null;
    private List<GenomicRegion> spanList = null;
    private int extendedRegionSize = 0;

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
     * @return the ftList
     */
    @SuppressWarnings("rawtypes")
    public List<Class> getFtList() {
        return ftList;
    }
    /**
     * @param ftList the ftList to set
     */
    public void setFtList(@SuppressWarnings("rawtypes") List<Class> ftList) {
        this.ftList = ftList;
    }

    /**
     * @return the spanList
     */
    public List<GenomicRegion> getSpanList() {
        return spanList;
    }
    /**
     * @param spanList the spanList to set
     */
    public void setSpanList(List<GenomicRegion> spanList) {
        this.spanList = spanList;
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
                    && spanList.equals(c.getSpanList())
                    && ftList.equals(c.getFtList())
                    && orgName.equals(c.getOrgName()));
        }
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return extendedRegionSize + spanList.hashCode() + ftList.hashCode() + orgName.hashCode();
    }

}
