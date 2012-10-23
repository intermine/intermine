package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

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
    private String perlWSModuleVer;
    private boolean isPublic;
    private String userName;
    private String resultTablesLib = null;
    private String baseUrl = null;

	/**
     * Constructor.
     *
     * @param query a PathQuery to copy
     * @param serviceBaseURL the base url of web service
     * @param projectTitle the Title of a local InterMine project
     * @param perlWSModuleVer the perl web service module version on CPAN
     * @param isPubliclyAccessible whether this query can be accessed by the public
     * @param user the name of the user who was logged in when this info was generated
     *
     */
    public WebserviceCodeGenInfo(PathQuery query, String serviceBaseURL,
            String projectTitle, String perlWSModuleVer, boolean isPubliclyAccessible, String user) {
        this.query = query;
        this.serviceBaseURL = serviceBaseURL;
        this.projectTitle = projectTitle;
        this.perlWSModuleVer = perlWSModuleVer;
        this.isPublic = isPubliclyAccessible;
        this.userName = user;
    }

    /**
     * Default Constructor.
     */
    public WebserviceCodeGenInfo() {
        this.query = null;
        this.serviceBaseURL = null;
        this.projectTitle = null;
        this.perlWSModuleVer = null;
        this.isPublic = true;
        this.userName = null;
    }

    public void readWebProperties(Properties properties) {
        if (properties != null) {
            resultTablesLib = (String) properties.get("ws.imtables.provider");
            baseUrl = properties.get("webapp.baseurl") + "/" + properties.get("webapp.path") + "/";
        }
    }

    public String getResultsTablesLib() {
        return resultTablesLib;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the filename that should be associated with this query.
     * @return a file name
     */
    public String getFileName() {
    	if (query instanceof TemplateQuery) {
    		return "template_query";
    	}
    	return "query";
    }

    /**
     * @return the query
     */
    public PathQuery getQuery() {
        return query;
    }

    /**
     * @return the serviceBaseURL
     */
    public String getServiceBaseURL() {
        return serviceBaseURL;
    }

    /**
     * @return the projectTitle
     */
    public String getProjectTitle() {
        return projectTitle;
    }

    /**
     * @return the perlWSModuleVer
     */
    public String getPerlWSModuleVer() {
        return perlWSModuleVer;
    }

    /**
     * Returns whether this query is publicly accessible. If so,
     * then the webservice will not need to implement a log-in.
     * @return Whether the query is public.
     */
    public boolean isPublic() {
		return isPublic;
	}

	/**
	 * The name of the user logged in when this info was generated
	 * @return The name of the user
	 */
	public String getUserName() {
		return userName;
	}
}
