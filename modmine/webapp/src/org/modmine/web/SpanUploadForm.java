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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

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
    private String[] experiments;
    private String[] featureTypes;
    private String text;
    private FormFile formFile;
    private String whichInput;
    private String submissions; // comma separated DCCid
    private String source; // from facted search or modMine

    private String isInterBaseCoordinate; // by default, intermine use base coordinate

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
    public String getSubmissions() {
        return submissions;
    }

    /**
     *
     * @param submissions A string array of submission DCCid
     */
    public void setSubmissions(String submissions) {
        this.submissions = submissions;
    }

    /**
     * @return experiments
     */
    public String[] getExperiments() {
        return experiments;
    }

    /**
    *
    * @param experiments exp names
    */
    public void setExperiments(String[] experiments) {
        this.experiments = experiments;
    }

    /**
     * Set the query string
     * @param text the query string
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the text string
     * @return the text string
     */
    public String getText() {
        return text;
    }

    /**
     * Set the FormFile.
     * @param formFile the FormFile
     */
    public void setFormFile(FormFile formFile) {
        this.formFile = formFile;
    }

    /**
     * Get the FormFile.
     * @return the FormFile.
     */
    public FormFile getFormFile() {
        return formFile;
    }

    /**
     * Set the method the user used to upload her span.
     * @param whichInput Which method the user used: paste or file
     */
    public void setWhichInput(String whichInput) {
        this.whichInput = whichInput;
    }

    /**
     * Get the method the user used to upload the span.  Will be either 'paste' or 'file'.  Paste if
     * they typed in the entries via the form.  File if they uploaded a file from their computer.
     * @return which method the user used to upload her span: paste or file
     */
    public String getWhichInput() {
        return whichInput;
    }

    /**
     * @return the isInterBaseCoordinate
     */
    public String getIsInterBaseCoordinate() {
        return isInterBaseCoordinate;
    }

    /**
     * @param isInterBaseCoordinate the isInterBaseCoordinate to set
     */
    public void setIsInterBaseCoordinate(String isInterBaseCoordinate) {
        this.isInterBaseCoordinate = isInterBaseCoordinate;
    }

    /**
     * @return source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Class Constructor
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
     * {@inheritDoc}
     */
    public void reset() {
        orgName = "";
        featureTypes = null;
        experiments = null;
        text = "";
        formFile = null;
        whichInput = "";
        submissions = "";
        source = "";
        isInterBaseCoordinate = "isNotInterBaseCoordinate";
    }
}
