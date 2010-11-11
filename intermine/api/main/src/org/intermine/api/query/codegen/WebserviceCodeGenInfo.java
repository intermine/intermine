package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.pathquery.PathQuery;

/**
 * Class to represent the information used to generate web service source code.
 *
 * @author Fengyuan Hu
 *
 */
public class WebserviceCodeGenInfo
{
    private PathQuery query;
    private String serviceBaseURL;
    private String projectTitle;

    /**
     * Constructor.
     *
     * @param query a PathQuery to copy
     * @param serviceBaseURL the base url of web service
     * @param projectTitle the Title of a local InterMine project
     *
     */
    public WebserviceCodeGenInfo(PathQuery query, String serviceBaseURL, String projectTitle) {
        this.query = query;
        this.serviceBaseURL = serviceBaseURL;
        this.projectTitle = projectTitle;
    }

    /**
     * Default Constructor.
     */
    public WebserviceCodeGenInfo() {
        this.query = null;
        this.serviceBaseURL = null;
        this.projectTitle = null;
    }

    /**
     * @return the query
     */
    public PathQuery getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(PathQuery query) {
        this.query = query;
    }

    /**
     * @return the serviceBaseURL
     */
    public String getServiceBaseURL() {
        return serviceBaseURL;
    }

    /**
     * @param serviceBaseURL the serviceBaseURL to set
     */
    public void setServiceBaseURL(String serviceBaseURL) {
        this.serviceBaseURL = serviceBaseURL;
    }

    /**
     * @return the projectTitle
     */
    public String getProjectTitle() {
        return projectTitle;
    }

    /**
     * @param projectTitle the projectTitle to set
     */
    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }
}
