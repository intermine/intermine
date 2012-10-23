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

import java.util.List;

import org.intermine.bio.web.model.GenomicRegion;

/**
 * A class to represent the constraints a user selected including a list of submissions, features
 * and spans.
 *
 * @author Fengyuan Hu
 */
public class SpanUploadConstraint
{
    private List<GenomicRegion> spanList = null;
    private List<String> subKeys = null;
    @SuppressWarnings("rawtypes")
    private List<Class> ftKeys = null;
    private String spanOrgName = null;

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
     * @return the subKeys
     */
    public List<String> getSubKeys() {
        return subKeys;
    }

    /**
     * @param subKeys the subKeys to set
     */
    public void setSubKeys(List<String> subKeys) {
        this.subKeys = subKeys;
    }

    /**
     * @return the ftKeys
     */
    @SuppressWarnings("rawtypes")
    public List<Class> getFtKeys() {
        return ftKeys;
    }

    /**
     * @param ftKeys the ftKeys to set
     */
    @SuppressWarnings("rawtypes")
    public void setFtKeys(List<Class> ftKeys) {
        this.ftKeys = ftKeys;
    }

    /**
     * @return the spanOrgName
     */
    public String getSpanOrgName() {
        return spanOrgName;
    }

    /**
     * @param spanOrgName the spanOrgName to set
     */
    public void setSpanOrgName(String spanOrgName) {
        this.spanOrgName = spanOrgName;
    }

    /**
     * @param obj a SpanUploadConstraint object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SpanUploadConstraint) {
            SpanUploadConstraint c = (SpanUploadConstraint) obj;
            return (spanList.equals(c.getSpanList())
                    && subKeys.equals(c.getSubKeys())
                    && ftKeys.equals(c.getFtKeys())
                    && spanOrgName.equals(c.getSpanOrgName()));
        }
        return false;
    }

    /**
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return spanList.hashCode() + subKeys.hashCode() + ftKeys.hashCode()
                + spanOrgName.hashCode();
    }
}
