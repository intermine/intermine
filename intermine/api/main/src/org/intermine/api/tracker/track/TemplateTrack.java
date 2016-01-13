package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Timestamp;

import org.intermine.api.tracker.util.TrackerUtil;
/**
 * Class to represent the track for the templates objects.
 * The track contains the template name, the access time, the session identifier
 * and the user name (if the user is logged in)
 * @author dbutano
 */
public class TemplateTrack extends TrackAbstract
{
    private String templateName;

    /**
     * Create a template track
     * @param templateName template name
     * @param username the user name
     * @param sessionIdentifier session id
     * @param timestamp access time
     */
    public TemplateTrack(String templateName, String username,
                        String sessionIdentifier, Timestamp timestamp) {
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
     * Return the template name
     * @return String template name
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate() {
        if (templateName != null && !"".equals(templateName)
            && sessionIdentifier != null && !"".equals(sessionIdentifier)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFormattedTrack() {
        return new Object[] {templateName, userName, sessionIdentifier, timestamp};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTableName() {
        return TrackerUtil.TEMPLATE_TRACKER_TABLE;
    }

}
