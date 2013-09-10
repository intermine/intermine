package org.modmine.web;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.profile.InterMineBag;

/**
 * Form to hold selections from submissionDisplayer to find overlapping features.
 * @author
 *
 */
public class FeaturesOverlapsForm extends ActionForm
{
    private static final Logger LOG = Logger.getLogger(FeaturesOverlapsForm.class);


    private static final long serialVersionUID = 1L;
    private String direction;
    private String distance;
    private String overlapFeatureType;
    private String flankingFeatureType;
    private String overlapFindType;

    private InterMineBag featuresList;


    /**
     * @return the overlapFeatureType
     */
    public String getOverlapFeatureType() {
        return overlapFeatureType;
    }

    /**
     * @param overlapFeatureType the overlapFeatureType to set
     */
    public void setOverlapFeatureType(String overlapFeatureType) {
        this.overlapFeatureType = overlapFeatureType;
    }


    /**
     * @return the overlapFindType
     */
    public String getOverlapFindType() {
        return overlapFindType;
    }

    /**
     * @param overlapFindType the overlapFindType to set
     */
    public void setOverlapFindType(String overlapFindType) {
        this.overlapFindType = overlapFindType;
    }


    /**
     * @return the flankingFeatureType
     */
    public String getFlankingFeatureType() {
        return flankingFeatureType;
    }

    /**
     * @param flankingFeatureType the flankingFeatureType to set
     */
    public void setFlankingFeatureType(String flankingFeatureType) {
        this.flankingFeatureType = flankingFeatureType;
    }


    /**
     * @return the distance
     */
    public String getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(String distance) {
        this.distance = distance;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * Get the bag type
     * @return the bag type string
     */
    public String getDirection() {
        return direction;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        direction = null;
        distance = null;
    }

    public InterMineBag getFeaturesList() {
        // TODO Auto-generated method stub
        return featuresList;
    }
}
