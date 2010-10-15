package org.intermine.tracker;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

    public TemplateTrack getMostPopularTemplate() {
        ResultSet rs = null;
        try {
            Statement stm = connection.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) accessnumbers"
                        + " FROM templatetrack tt, tag t"
                        + " WHERE tt.templatename=t.objectidentifier "
                        + " AND t.tagname LIKE '%public' AND t.type='template'"
                        + " GROUP BY tt.templatename"
                        + " ORDER BY accessnumbers DESC LIMIT 1";
            rs = stm.executeQuery(sql);
            rs.next();
            return new TemplateTrack(rs.getString(1), "", rs.getInt(2));
        } catch (SQLException sqle) {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("Problem closing  resultset in getMostVisitedTemplateByUser", e);
                }
            }
        }
        return null;
    }

    public List<String> getMostPopularTemplateOrder() {
        ResultSet rs = null;
        List<String> mostPopularTemplateOrder = new ArrayList<String>();
        try {
            Statement stm = connection.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) accessnumbers"
                        + " FROM templatetrack tt, tag t"
                        + " WHERE tt.templatename=t.objectidentifier "
                        + " AND t.tagname LIKE '%public' AND t.type='template'"
                        + " GROUP BY tt.templatename"
                        + " ORDER BY accessnumbers DESC";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                mostPopularTemplateOrder.add(rs.getString(1));
            }
            return mostPopularTemplateOrder;
        } catch (SQLException sqle) {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("Problem closing  resultset in getMostVisitedTemplateByUser", e);
                }
            }
        }
        return null;
    }

    public TemplateTrack getMostPopularTemplate(String userName, String sessionId) {
        ResultSet rs = null;
        try {
            Statement stm = connection.createStatement();
            StringBuffer sql = new StringBuffer("SELECT templatename, "
                                                + " COUNT(templatename) as accessnumbers,"
                                                + " username as user "
                                                + " FROM templatetrack GROUP BY templatename"
                                                + " ORDER BY accessnumbers DESC LIMIT 1 "
                                                + " WHERE sessionidentifier = " + sessionId);
            if (userName != null && !"".equals(userName)) {
                sql.append(" OR user=" + userName);
            }
            rs = stm.executeQuery(sql.toString());
            String templateName = rs.getString(0);
            int accessCounter = rs.getInt(1);
            rs.close();
            return new TemplateTrack(templateName, userName, accessCounter);
        } catch (SQLException sqle) {
            LOG.error("Problem executing query mostVisitedTemplateByUser", sqle);
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("Problem closing  resultset in getMostVisitedTemplateByUser", e);
                }
            }
        }
        return null;
    }

    public Object getTrack() {
        return null;
    }

    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName
            + "(templatename text, username text, timestamp bigint, sessionidentifier text)";
    }

    public Object[] getFormattedTrack(TrackerInput track) {
        if (track instanceof TemplateTrack) {
            TemplateTrack tti = (TemplateTrack) track;
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
