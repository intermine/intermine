package org.modmine.web.model;

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
 * A bean to represent a row of the results fields from a query in SpanOverlapQueryRunner.
 *
 * @author Fengyuan Hu
 */
public class SpanQueryResultRow
{

    private Integer featureId;
    private String featurePID;
    private String featureClass;
    private String chr;
    private Integer start;
    private Integer end;
    private String subDCCid; // a String as modENCODE_2675
    private String subTitle;

    /**
     * @return the featureId
     */
    public Integer getFeatureId() {
        return featureId;
    }

    /**
     * @param featureId the featureId to set
     */
    public void setFeatureId(Integer featureId) {
        this.featureId = featureId;
    }

    /**
     * @return feature primary id
     */
    public String getFeaturePID() {
        return featurePID;
    }

    /**
     * @param featurePID feature primary id
     */
    public void setFeaturePID(String featurePID) {
        this.featurePID = featurePID;
    }

    /**
     * @return feature class
     */
    public String getFeatureClass() {
        return featureClass;
    }

    /**
     * @param featureClass feature class
     */
    public void setFeatureClass(String featureClass) {
        this.featureClass = featureClass;
    }

    /**
     * @return chromosome
     */
    public String getChr() {
        return chr;
    }

    /**
     * @param chr chromosome
     */
    public void setChr(String chr) {
        this.chr = chr;
    }

    /**
     * @return feature start position
     */
    public Integer getStart() {
        return start;
    }

    /**
     *
     * @param start feature start position
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * @return feature end position
     */
    public Integer getEnd() {
        return end;
    }

    /**
     * @param end feature end position
     */
    public void setEnd(Integer end) {
        this.end = end;
    }

    /**
     * @return submission DCCid
     */
    public String getSubDCCid() {
        return subDCCid;
    }

    /**
     * @param subDCCid submission DCCid
     */
    public void setSubDCCid(String subDCCid) {
        this.subDCCid = subDCCid;
    }

    /**
     * @return submission title
     */
    public String getSubTitle() {
        return subTitle;
    }

    /**
     * @param subTitle submission title
     */
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    /**
     * @return chr:start..end
     */
    public String locationToString() {
        return chr + ":" + start + ".." + end;
    }
}
