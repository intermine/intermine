package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * This ActionForm represents the form elements on the spanUploadOptions.jsp
 *
 * @author Fengyuan Hu
 *
 */
public class SpanUploadForm extends ActionForm
{

    private static final long serialVersionUID = 1L;

    private String orgName;
    private String[] featureTypes;
    private Integer[] submissions; // DCCid

    /**
     *
     * @return orgName
     */
    public String getOrgName() {
        return orgName;
    }

    /**
     *
     * @param orgName organism primaryIdentifier
     */
    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    /**
     *
     * @return featureTypes
     */
    public String[] getFeatureTypes() {
        return featureTypes;
    }

    /**
     *
     * @param featureTypes A string array of feature types
     */
    public void setFeatureTypes(String[] featureTypes) {
        this.featureTypes = featureTypes;
    }

    /**
     *
     * @return submissions
     */
    public Integer[] getSubmissions() {
        return submissions;
    }

    /**
     *
     * @param submissions A string array of submission DCCid
     */
    public void setSubmissions(Integer[] submissions) {
        this.submissions = submissions;
    }

    /**
     *
     */
    public SpanUploadForm() {
        reset();
    }

    /**
     * Reset form bean.
     *
     * @param mapping  the action mapping associated with this form bean
     * @param request  the current http servlet request
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }

    /**
     *
     */
    public void reset() {

        orgName = "";
        featureTypes = null;
        submissions = null;
    }

}

