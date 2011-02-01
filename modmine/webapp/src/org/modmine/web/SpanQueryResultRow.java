package org.modmine.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A bean to represent a row of the results fields from a query in SpanOverlapQueryRunner
 * @author fhu
 */
public class SpanQueryResultRow
{

    private String featurePID;
    @SuppressWarnings("rawtypes")
    private Class featureClass;
    private String chr;
    private Integer start;
    private Integer end;
    private String subDCCid;
    private String subTitle;

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
    @SuppressWarnings("rawtypes")
    public Class getFeatureClass() {
        return featureClass;
    }

    /**
     * @param featureClass feature class
     */
    public void setFeatureClass(@SuppressWarnings("rawtypes") Class featureClass) {
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

}
