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

import java.util.Map;

/**
 * This is a POJO to represent Cytoscape Web node data.
 * Easy to be extended.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeNetworkNodeData
{
    private Integer interMineId;
    private String sourceId; // TODO combine interMineId to only one id
    private String sourceLabel; // sometimes no values
    private String featueType; //e.g. miRNA/TF
    private String position;
    private String organism;
    private Map<String, String> extraInfo;

    /**
     * @return the interMineId
     */
    public Integer getInterMineId() {
        return interMineId;
    }

    /**
     * @param interMineId the interMineId to set
     */
    public void setInterMineId(Integer interMineId) {
        this.interMineId = interMineId;
    }

    /**
     * @return the sourceId
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId the sourceId to set
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the sourceLabel
     */
    public String getSourceLabel() {
        return sourceLabel;
    }

    /**
     * @param sourceLabel the sourceLabel to set
     */
    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    /**
     * @return the featueType
     */
    public String getFeatueType() {
        return featueType;
    }

    /**
     * @param featueType the featueType to set
     */
    public void setFeatueType(String featueType) {
        this.featueType = featueType;
    }

    /**
     * @return the position
     */
    public String getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(String position) {
        this.position = position;
    }

    /**
     * @return the organism
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
     * @return the extraInfo
     */
    public Map<String, String> getExtraInfo() {
        return extraInfo;
    }

    /**
     * @param extraInfo the extraInfo to set
     */
    public void setExtraInfo(Map<String, String> extraInfo) {
        this.extraInfo = extraInfo;
    }

    /**
     * @param obj a CytoscapeNetworkNodeData object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CytoscapeNetworkNodeData) {
            CytoscapeNetworkNodeData m = (CytoscapeNetworkNodeData) obj;
            return (interMineId.equals(m.getInterMineId()));
        }
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return interMineId.hashCode();
    }
}
