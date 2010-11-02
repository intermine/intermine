package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
/**
 * Class to represent the track for the templates objects.
 * The track contains the template name, the access time, the session identifier
 * and the user name (if the user is logged in)
 * @author dbutano
 */
public class TemplateTrack implements Track
{
    private String userName;
    private String templateName;
    boolean isTemplatePublic;
    private long timestamp;
    private String sessionIdentifier;

    /**
     * Create a template track
     * @param templateName template name
     * @param isTemplatePublic true if the template has the tag public
     * @param username the user name
     * @param sessionIdentifier session id
     * @param timestamp access time
     */
    public TemplateTrack(String templateName, boolean isTemplatePublic, String username,
                        String sessionIdentifier, long timestamp) {
        this.templateName = templateName;
        this.isTemplatePublic = isTemplatePublic;
        this.userName = username;
        this.sessionIdentifier = sessionIdentifier;
        this.timestamp = timestamp;
    }

    /**
     * Create a template track
     * @param templateName template name
     * @param isTemplatePublic true if the template has the tag public
     * @param username the user name
     * @param sessionIdentifier session id
     */
    public TemplateTrack(String templateName, boolean isTemplatePublic, String username,
                         String sessionIdentifier) {
        this.templateName = templateName;
        this.isTemplatePublic = isTemplatePublic;
        this.userName = username;
        this.sessionIdentifier = sessionIdentifier;
    }

    /**
     * Return the user name
     * @return String user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the user name
     * @param userName the user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Return the template name
     * @return String template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Set the template name
     * @param templateName the template name
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
     * Return true if the template has the public tag
     * @return true if the template has the public tag
     */
    public boolean isTemplatePublic() {
        return isTemplatePublic;
    }

    /**
     * Set if the template has a public tag or not
     * @param isTemplatePublic the flag
     */
    public void setTemplatePublic(boolean isTemplatePublic) {
        this.isTemplatePublic = isTemplatePublic;
    }

    /**
     * Return the time of access
     * @return long the time of access
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the time of access to the template
     * @param timestamp the time of access
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Return the session id
     * @return String session id
     */
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    /**
     * Set the session id
     * @param sessionIdentifier the session id
     */
    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    /**
     * Validate the template track before saving into the database
     * @return true if the template track is valid
     */
    public boolean validate() {
        if (templateName != null && !"".equals(templateName) && sessionIdentifier != null) {
            return true;
        }
        return false;
    }

}
