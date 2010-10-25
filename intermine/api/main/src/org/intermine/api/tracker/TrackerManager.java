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

public class TrackerManager
{
    protected Map<String, Tracker> trackers;
    protected TemplateTracker templateTracker;
    private static final Logger LOG = Logger.getLogger(TrackerManager.class);

    public TrackerManager(Map<String, Tracker> trackers) {
        this.trackers = trackers;
        if (!trackers.isEmpty()) {
            templateTracker = (TemplateTracker) trackers.get(TemplateTracker.getTrackerName());
        }
    }

    public Map<String, Tracker> getTrackers() {
        return trackers;
    }

    public TemplateTracker getTemplateTracker() {
        return templateTracker;
    }

    public void trackTemplate(String templateName, Profile profile, String sessionIdentifier) {
        TemplateTrack templateTrack = new TemplateTrack(templateName, sessionIdentifier,
                                              System.currentTimeMillis());
        if (profile != null && profile.getUsername() != null) {
            templateTrack.setUserName(profile.getUsername());
        }
        if (templateTracker  != null) {
            templateTracker.storeTrack(templateTrack);
        } else {
            LOG.warn("Template not tracked. Check if the TemplateTracker has ben configured");
        }
    }

    public TemplateTrack getMostPopularTemplate() {
        if (templateTracker != null) {
            return templateTracker.getMostPopularTemplate();
        }
        return null;
    }

    public List<String> getMostPopularTemplateOrder() {
        if (templateTracker != null) {
            return templateTracker.getMostPopularTemplateOrder();
        }
        return null;
    }

    public List<String> getMostPopularTemplateOrder(Profile profile, String sessionIdentifier) {
        if (profile != null && templateTracker != null) {
            return templateTracker.getMostPopularTemplateOrder(profile.getUsername(),
                                                               sessionIdentifier);
        }
        return null;
    }

    public Map<String, Integer> getRank() {
        if (templateTracker != null) {
            return templateTracker.getRank();
        }
        return null;
    }
}
