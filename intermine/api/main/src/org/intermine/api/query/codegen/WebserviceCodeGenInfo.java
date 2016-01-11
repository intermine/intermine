package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.Profile;
import org.intermine.api.util.AnonProfile;
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
    private Profile user;
    private String resultTablesLib = null;
    private String baseUrl = null;
    private String lineBreak;
    private Properties properties = new Properties();

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
    public WebserviceCodeGenInfo(PathQuery query, String serviceBaseURL, String projectTitle,
            String perlWSModuleVer, boolean isPubliclyAccessible, Profile user) {
        this.query = query;
        this.serviceBaseURL = serviceBaseURL;
        this.projectTitle = projectTitle;
        this.perlWSModuleVer = perlWSModuleVer;
        this.isPublic = isPubliclyAccessible;
        this.user = user;
        this.lineBreak = System.getProperty("line.separator");
    }

    /**
     * @param pq a PathQuery to copy
     * @param serviceBaseURL the base url of web service
     * @param projectTitle the Title of a local InterMine project
     * @param perlWSModuleVer the perl web service module version on CPAN
     * @param pathQueryIsPublic whether this query can be accessed by the public
     * @param profile the profile of the user who was logged in when this info was generated
     * @param lineBreak which linebreak to use
     */
    public WebserviceCodeGenInfo(PathQuery pq, String serviceBaseURL,
            String projectTitle, String perlWSModuleVer,
            boolean pathQueryIsPublic, Profile profile, String lineBreak) {
        this.query = pq;
        this.serviceBaseURL = serviceBaseURL;
        this.projectTitle = projectTitle;
        this.perlWSModuleVer = perlWSModuleVer;
        this.isPublic = pathQueryIsPublic;
        this.user = profile;
        this.lineBreak = lineBreak;
    }

    /**
     * @param props properties
     */
    public void readWebProperties(Properties props) {
        this.properties.putAll(props);
        if (properties != null) {
            resultTablesLib = (String) properties.get("ws.imtables.provider");
            baseUrl = properties.get("webapp.baseurl") + "/" + properties.get("webapp.path") + "/";
        }
    }

    /**
     * Get a configured property for which an accessor does not exist.
     *
     * @param key The key for this property.
     * @param defaultValue The value to return if this property is not configured.
     * @return The value of the property.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     *
     * @return results tables
     */
    public String getResultsTablesLib() {
        return resultTablesLib;
    }

    /**
     *
     * @return base URL
     */
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
        return user.getUsername();
    }

    /**
     * @return a line break
     */
    public String getLineBreak() {
        return lineBreak;
    }

    /**
     * A token for the user. The permanent token is preferred, but a temporary one
     * is generated if that is not available.
     * @return A token for the the user
     */
    public String getUserToken() {
        if (user.getApiKey() != null) {
            return user.getApiKey();
        }
        return user.getDayToken();
    }

    /**
     * @return True if this user has a permanent profile.
     */
    public boolean isLoggedIn() {
        return user.getUserId() != null && !(
                StringUtils.isBlank(user.getUsername())
                    || AnonProfile.USERNAME.equals(user.getUsername()));
    }
}
