package org.intermine.tracker;

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
