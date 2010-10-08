package org.intermine.util;

import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.tracker.TemplateTracker;
import org.intermine.tracker.TemplateTrackerInput;
import org.intermine.tracker.Tracker;
import org.intermine.web.logic.session.SessionMethods;

public class TrackerManager {

    public static void trackTemplate(String templateName, InterMineAPI im, HttpSession session) {
        TemplateTrackerInput tti = new TemplateTrackerInput();
        tti.setTemplateName(templateName);
        tti.setSessionIdentifier(session.getId());
        tti.setTimestamp(System.currentTimeMillis());
        Profile profile = SessionMethods.getProfile(session);
        if (profile != null && profile.getUsername() != null) {
            tti.setUserName(profile.getUsername());
        }
        if (!im.getTrackers().isEmpty()) {
            Tracker templateTracker = im.getTrackers().get(TemplateTracker.getTrackerName());
            if (templateTracker != null) {
                templateTracker.storeTrack(tti);
            }
        }
    }
}
