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
import java.util.Map;

import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;

/**
 * Intermediate class which decouples the tracker components from the code that uses them.
 * @author dbutano
 *
 */
public class TrackerDelegate
{
    protected Map<String, Tracker> trackers;

    /**
     * Create the tracker manager managing the trackers specified in input
     * @param trackers the trackers
     */
    public TrackerDelegate(Map<String, Tracker> trackers) {
        this.trackers = trackers;
    }

    /**
     * Return the trackers saved in the TrackerManager
     * @return map containing names and trackers
     */
    public Map<String, Tracker> getTrackers() {
        return trackers;
    }

    /**
     * Return the tracker template
     * @return map containing names and trackers
     */
    public TemplateTracker getTemplateTracker() {
        if (!trackers.isEmpty()) {
            return (TemplateTracker ) trackers.get(TemplateTracker.TRACKER_NAME);
        }
        return null;
    }

    /**
     * Store into the database the template execution by the user specified in input
     * @param templateName the template name
     * @param profile the user profile
     * @param sessionIdentifier the session id
     */
    public void trackTemplate(String templateName, Profile profile,
                             String sessionIdentifier) {
        TemplateTracker tt = getTemplateTracker();
        if (tt != null) {
            tt.trackTemplate(templateName, profile, sessionIdentifier);
        }
    }

    /**
     * Return the rank associated to the templates
     * @return map with key the template name and value the rank associated
     */
    public Map<String, Integer> getAccessCounter() {
        TemplateTracker tt = getTemplateTracker();
        if (tt != null) {
            return tt.getAccessCounter();
        }
        return null;
    }

    /**
     * Return the rank associated to the templates
     * @param templateManager the template manager
     * @return map with key the template name and value the rank associated
     */
    public Map<String, Integer> getRank(TemplateManager templateManager) {
        TemplateTracker tt = getTemplateTracker();
        if (tt != null) {
            return tt.getRank(templateManager);
        }
        return null;
    }

    /**
     * Update the template name value into the database
     * @param oldTemplateName the old name
     * @param newTemplateName the new name
     */
    public void updateTemplateName(String oldTemplateName, String newTemplateName) {
        TemplateTracker tt = getTemplateTracker();
        if (tt != null) {
            tt.updateTemplateName(oldTemplateName, newTemplateName);
        }
    }
}
