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
import java.util.Set;

/**
 * This is a Java Bean to represent Cytoscape Web edge data.
 * Easy to be extended.
 *
 * @author Fengyuan Hu
 *
 */
public class CytoscapeNetworkEdgeData
{
    private String id; // as in a form of "<sourceId>-<targetId>"
    private String sourceId; // Use InterMine Id
    private String sourceLabel; // sometimes no values
    private String targetId; // Use InterMine Id
    private String targetLabel; // sometimes no values
    private String interactionType; // edge id and label
    private String direction; // both or one
    /** key - datasource name, value - interaction short name **/
    private Map<String, Set<String>> dataSources;
    private Map<String, String> extraInfo; // such as color, etc.
    // TODO private Map<String, Object> extraInfo, combine dataSources and extraInfo

    /**
     * @return interactionString a record in format of "source<tab>interactionType<tab>target"
     */
    public String generateInteractionString() {
        return sourceId + "\\t" + interactionType + "\\t" + targetId;
    }

    /**
     * @return interactionString a record in format of "target<tab>interactionType<tab>source"
     */
    public String generateReverseInteractionString() {
        return targetId + "\\t" + interactionType + "\\t" + sourceId;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the soureceId
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @param soureceId the soureceId to set
     */
    public void setSourceId(String soureceId) {
        this.sourceId = soureceId;
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
     * @return the targetId
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * @param targetId the targetId to set
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * @return the targetLabel
     */
    public String getTargetLabel() {
        return targetLabel;
    }

    /**
     * @param targetLabel the targetLabel to set
     */
    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    /**
     * @return the interactionType
     */
    public String getInteractionType() {
        return interactionType;
    }

    /**
     * @param interactionType the interactionType to set
     */
    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    /**
     * @return the direction
     */
    public String getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * @return the dataSources as Map
     */
    public Map<String, Set<String>> getDataSources() {
        return dataSources;
    }

    /**
     * @param dataSources the dataSources to set
     */
    public void setDataSources(Map<String, Set<String>> dataSources) {
        this.dataSources = dataSources;
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
     * @param obj a CytoscapeNetworkEdgeData object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CytoscapeNetworkEdgeData) {
            CytoscapeNetworkEdgeData e = (CytoscapeNetworkEdgeData) obj;
            return (id.equals(e.getId()));
        }
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return (sourceId + "-" + targetId).hashCode();
    }
}
