package org.intermine.util;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.tracker.TemplateTracker;
import org.intermine.tracker.TemplateTrack;
import org.intermine.web.logic.session.SessionMethods;

public class TrackerManager
{
    private static TrackerManager tm = null;
    private TemplateTracker tracker = null;

    private TrackerManager(InterMineAPI im) {
        if (!im.getTrackers().isEmpty()) {
            tracker = (TemplateTracker) im.getTrackers().get(TemplateTracker.getTrackerName());
        }
    }

    public static TrackerManager getInstance(InterMineAPI im) {
        if (tm == null) {
            tm = new TrackerManager(im);
        }
        return tm;
    }

    public void trackTemplate(String templateName, HttpSession session) {
        TemplateTrack tt = new TemplateTrack(templateName, session.getId(),
                                              System.currentTimeMillis());
        Profile profile = SessionMethods.getProfile(session);
        if (profile != null && profile.getUsername() != null) {
            tt.setUserName(profile.getUsername());
        }
        tracker.storeTrack(tt);
    }

    public TemplateTrack getMostPopularTemplate() {
        return tracker.getMostPopularTemplate();
    }

    public List<String> getMostPopularTemplateOrder() {
        return tracker.getMostPopularTemplateOrder();
    }

    public List<String> getMostPopularTemplateOrder(HttpSession session) {
        Profile profile = SessionMethods.getProfile(session);
        if (profile != null) {
            return tracker.getMostPopularTemplateOrder(profile.getUsername(), session.getId());
        }
        return null;
    }
    
    public Map<String, Integer> getRank() {
        return tracker.getRank();
    }
}
