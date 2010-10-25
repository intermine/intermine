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
public class TemplateTrack implements TrackerInput
{
    private String userName;
    private String templateName;
    private long timestamp;
    private String sessionIdentifier;
    private int accessCounter;

    public TemplateTrack(String templateName, String sessionIdentifier, long timestamp) {
        this.templateName = templateName;
        this.sessionIdentifier = sessionIdentifier;
        this.timestamp = timestamp;
    }

    public TemplateTrack(String templateName, String userName, int accessCounter) {
        this.templateName = templateName;
        this.userName = userName;
        this.accessCounter = accessCounter;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    public boolean validate() {
        if (templateName != null && !"".equals(templateName) && sessionIdentifier != null) {
            return true;
        }
        return false;
    }

}
