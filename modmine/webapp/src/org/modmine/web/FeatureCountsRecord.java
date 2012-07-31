package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A bean to store featureType and its duplicated counts and unique counts in an experiment
 *
 * @author Fengyuan Hu
 *
 */
public class FeatureCountsRecord
{
    private String featureType;
    private Long featureCounts;
    private Long uniqueFeatureCounts;


    /**
     * Constructor
     * @param featureType feature type
     * @param featureCounts duplicated counts
     * @param uniqueFeatureCounts unique counts
     */
    public FeatureCountsRecord(String featureType, Long featureCounts, Long uniqueFeatureCounts) {
        this.featureType = featureType;
        this.featureCounts = featureCounts;
        this.uniqueFeatureCounts = uniqueFeatureCounts;
    }

    /**
     *
     * @return the featureType
     */
    public String getFeatureType() {
        return featureType;
    }

    /**
     *
     * @param featureType feature type
     */
    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    /**
     *
     * @return the featureCounts
     */
    public Long getFeatureCounts() {
        return featureCounts;
    }

    /**
     *
     * @param featureCounts duplicated counts
     */
    public void setFeatureCounts(Long featureCounts) {
        this.featureCounts = featureCounts;
    }

    /**
     *
     * @return the uniqueFeatureCounts
     */
    public Long getUniqueFeatureCounts() {
        return uniqueFeatureCounts;
    }

    /**
     *
     * @param uniqueFeatureCounts unique counts
     */
    public void setUniqueFeatureCounts(Long uniqueFeatureCounts) {
        this.uniqueFeatureCounts = uniqueFeatureCounts;
    }
}

