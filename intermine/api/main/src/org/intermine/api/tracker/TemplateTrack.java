package org.intermine.api.tracker;

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
 * Class to represent the track for the templates objects.
 * The track contains the template name, the access time, the session identifier
 * and the user name (if the user is logged in)
 * @author dbutano
 */
public class TemplateTrack implements Track
{
    private String userName;
    private String templateName;
    private long timestamp;
    private String sessionIdentifier;

    /**
     * Create a template track
     * @param templateName template name
     * @param username the user name
     * @param sessionIdentifier session id
     * @param timestamp access time
     */
    public TemplateTrack(String templateName, String username,
                        String sessionIdentifier, long timestamp) {
        this.templateName = templateName;
        this.userName = username;
        this.sessionIdentifier = sessionIdentifier;
        this.timestamp = timestamp;
    }

    /**
     * Create a template track
     * @param templateName template name
     * @param username the user name
     * @param sessionIdentifier session id
     */
    public TemplateTrack(String templateName, String username,
                         String sessionIdentifier) {
        this.templateName = templateName;
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
     * Return the template name
     * @return String template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Return the time of access
     * @return long the time of access
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Return the session id
     * @return String session id
     */
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    /**
     * Validate the template track before saving into the database.
     * A track is valid if has a template name and a session identifier
     * @return true if the template track is valid
     */
    public boolean validate() {
        if (templateName != null && !"".equals(templateName)
            && sessionIdentifier != null && !"".equals(sessionIdentifier)) {
            return true;
        }
        return false;
    }

}
