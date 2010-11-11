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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
    protected TemplateTracker templateTracker;
    private static final Logger LOG = Logger.getLogger(TrackerDelegate.class);

    /**
     * Create the tracker manager managing the trackers specified in input
     * @param trackers the trackers
     */
    public TrackerDelegate(Map<String, Tracker> trackers) {
        this.trackers = trackers;
        if (!trackers.isEmpty()) {
            templateTracker = (TemplateTracker) trackers.get(TemplateTracker.getTrackerName());
        }
    }

    /**
     * Return the trackers saved in the TrackerManager
     * @return map containing names and trackers
     */
    public Map<String, Tracker> getTrackers() {
        return trackers;
    }

    public void setTemplateManager(TemplateManager templateManager) {
        if (templateTracker != null) {
            templateTracker.setTemplateManager(templateManager);
        }
    }
    /**
     * Store into the database the template execution by the user specified in input
     * @param templateName the template name
     * @param profile the user profile
     * @param sessionIdentifier the session id
     */
    public void trackTemplate(String templateName, Profile profile,
                             String sessionIdentifier) {
        if (templateTracker != null) {
            templateTracker.trackTemplate(templateName, profile, sessionIdentifier);
        }
    }

    /**
     * Return the template name associated to the public template with the highest rank
     * @return String the template name
     */
    public String getMostPopularTemplate() {
        if (templateTracker != null) {
            return templateTracker.getMostPopularTemplate();
        }
        return null;
    }

    /**
     * Return the list of public templates ordered by rank descendant.
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder() {
        if (templateTracker != null) {
            return templateTracker.getMostPopularTemplateOrder();
        }
        return null;
    }

    /**
     * Return the template list ordered by rank descendant for the user specified in input
     * @param profile the user profile
     * @param sessionIdentifier the session id
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder(Profile profile, String sessionIdentifier) {
        if (profile != null && templateTracker != null) {
            return templateTracker.getMostPopularTemplateOrder(profile.getUsername(),
                                                               sessionIdentifier);
        }
        return null;
    }

    /**
     * Return the rank associated to the templates
     * @return map with key the template name and value the rank associated
     */
    public Map<String, Integer> getAccessCounter() {
        if (templateTracker != null) {
            return templateTracker.getAccessCounter();
        }
        return null;
    }

    /**
     * Return the rank associated to the templates
     * @return map with key the template name and value the rank associated
     */
    public Map<String, Integer> getRank() {
        if (templateTracker != null) {
            return templateTracker.getRank();
        }
        return null;
    }

    /**
     * Update the template name value into the database
     * @param oldTemplateName the old name
     * @param newTemplateName the new name
     */
    public void updateTemplateName(String oldTemplateName, String newTemplateName) {
        if (templateTracker != null) {
            templateTracker.updateTemplateName(oldTemplateName, newTemplateName);
        }
    }
}
