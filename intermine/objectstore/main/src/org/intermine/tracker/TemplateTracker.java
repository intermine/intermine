package org.intermine.tracker;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import org.apache.log4j.Logger;
import org.intermine.util.ShutdownHook;

public class TemplateTracker extends TrackerAbstract
{
    private static final Logger LOG = Logger.getLogger(TemplateTracker.class);
    private static TemplateTracker templateTracker = null;
    private static final String TRACKER_NAME = "TemplateTracker";

    protected TemplateTracker(Connection conn) {
        this.connection = conn;
        trackTableName = "templatetrack";
        trackTableColumns =
            new String[] {"templatename", "username", "timestamp", "sessionidentifier"};
        ShutdownHook.registerObject(new WeakReference<Object>(this));
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }

    public static TemplateTracker getInstance(Connection con) {
        if (templateTracker == null) {
            templateTracker = new TemplateTracker(con);
            try {
                templateTracker.createTrackerTable();
            } catch (Exception e) {
                //TODO
                //LOG.warn("Error setting up execute log in database table" + e);
            }
        }
        return templateTracker;
    }

    public Object getTrack() {
        return null;
    }

    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName
            + "(templatename text, username text, timestamp bigint, sessionidentifier text)";
    }

    public Object[] getFormattedTrack(TrackerInput track) {
        if (track instanceof TemplateTrackerInput) {
            TemplateTrackerInput tti = (TemplateTrackerInput) track;
            return new Object[] {tti.getTemplateName(), tti.getUserName(),
                                tti.getTimestamp(), tti.getSessionIdentifier()};
        } else {
            return null;
        }
    }

    public static String getTrackerName() {
        return TRACKER_NAME;
    }

    public String getName() {
        return TRACKER_NAME;
    }

}
