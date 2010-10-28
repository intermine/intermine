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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;

/**
 * Class for tracking the accesses to the templates by the users.
 * @author dbutano
 */
public class TemplateTracker extends TrackerAbstract
{
    private static final Logger LOG = Logger.getLogger(TemplateTracker.class);
    private static TemplateTracker templateTracker = null;
    private static final String TRACKER_NAME = "TemplateTracker";

    /**
     * Build a template tracker
     * @param conn connection to the database
     */
    protected TemplateTracker(Connection conn) {
        this.connection = conn;
        trackTableName = "templatetrack";
        trackTableColumns =
            new String[] {"templatename", "username", "timestamp", "sessionidentifier"};
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }

    /**
     * Return an instance of the TemplateTracker
     * @param con connection to the database
     * @return TemplateTracker the template tracker
     */
    public static TemplateTracker getInstance(Connection con) {
        if (templateTracker == null) {
            templateTracker = new TemplateTracker(con);
            try {
                templateTracker.createTrackerTable();
            } catch (Exception e) {
                LOG.error("Error creating the table associated to the TemplateTracker" + e);
            }
        }
        return templateTracker;
    }

    /**
     * Store into the database the template execution by the user specified in input
     * @param templateName the template name
     * @param profile the user profile
     * @param sessionIdentifier the session id
     */
    public void trackTemplate(String templateName, Profile profile, String sessionIdentifier) {
        TemplateTrack templateTrack = new TemplateTrack(templateName, sessionIdentifier,
                                              System.currentTimeMillis());
        String userName = (profile != null && profile.getUsername() != null)
                          ? profile.getUsername()
                          : "";
        templateTrack.setUserName(userName);
        if (templateTracker  != null) {
            templateTracker.storeTrack(templateTrack);
        } else {
            LOG.warn("Template not tracked. Check if the TemplateTracker has been configured");
        }
    }

    /**
     * Return the template name associated to the public template with the highest rank
     * @return String the template name
     */
    public String getMostPopularTemplate() {
        ResultSet rs = null;
        Statement stm = null;
        try {
            stm = connection.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) accessnumbers"
                        + " FROM templatetrack tt, tag t"
                        + " WHERE tt.templatename=t.objectidentifier "
                        + " AND t.tagname LIKE '%public' AND t.type='template'"
                        + " GROUP BY tt.templatename"
                        + " ORDER BY accessnumbers DESC LIMIT 1";
            rs = stm.executeQuery(sql);
            rs.next();
            return rs.getString(1);
        } catch (SQLException sqle) {
            releaseResources(rs, stm);
        }
        return null;
    }

    /**
     * Return the list of public templates ordered by rank descendant.
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder() {
        ResultSet rs = null;
        Statement stm = null;
        List<String> mostPopularTemplateOrder = new ArrayList<String>();
        try {
            stm = connection.createStatement();
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
            releaseResources(rs, stm);
        }
        return null;
    }

    /**
     * Return the template list ordered by rank descendant for the user specified in input
     * @param userName the user name
     * @param sessionId the session id
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder(String userName, String sessionId) {
        ResultSet rs = null;
        Statement stm = null;
        List<String> mostPopularTemplateOrder = new ArrayList<String>();
        try {
            stm = connection.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) accessnumbers"
                        + " FROM templatetrack tt"
                        + " WHERE username = '" + userName + "'"
                        + " OR  sessionidentifier = '" + sessionId + "'"
                        + " GROUP BY tt.templatename"
                        + " ORDER BY accessnumbers DESC";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                mostPopularTemplateOrder.add(rs.getString(1));
            }
            return mostPopularTemplateOrder;
        } catch (SQLException sqle) {
            releaseResources(rs, stm);
        }
        return null;
    }

    /**
     * Return the rank associated to the templates
     * @return map with key the template name and value the rank associated
     */
    public Map<String, Integer> getRank() {
        ResultSet rs = null;
        Statement stm = null;
        Map<String, Integer> templateRank = new HashMap<String, Integer>();
        try {
            stm = connection.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) accessnumbers"
                        + " FROM templatetrack tt"
                        + " GROUP BY tt.templatename";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                templateRank.put(rs.getString(1), rs.getInt(2));
            }
            return templateRank;
        } catch (SQLException sqle) {
            releaseResources(rs, stm);
        }
        return null;
    }

    /**
     * Close the result set and statement objects in input
     * @param rs
     * @param stm
     */
    private void releaseResources(ResultSet rs, Statement stm) {
        if (rs != null) {
            try {
                rs.close();
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException e) {
                LOG.error("Problem closing  resources.", e);
            }
        }
    }

    /**
     * Update the template name value into the database
     * @param oldTemplateName the old name
     * @param newTemplateName the new name
     */
    public void updateTemplateName(String oldTemplateName, String newTemplateName) {
        Statement stm = null;
        try {
            stm = connection.createStatement();
            String sql = "UPDATE " + trackTableName
                        + " SET templatename = '" + newTemplateName + "'"
                        + " WHERE templatename = '" + oldTemplateName + "'";
            stm.executeUpdate(sql);
        } catch (SQLException sqe) {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    LOG.error("Problem closing  resources in updateTemplateName()", e);
                }
            }
        }
    }

    /**
     * Generate the sql statement to create the templatetrack table used by the template tracker
     * @return String sql statement
     */
    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName
            + "(templatename text, username text, timestamp bigint, sessionidentifier text)";
    }

    /**
     * Format the template track into an array of Objects to be saved in the database
     * The order is the same of the trackTableColumns
     * @param track the template track to format
     * @return Object[] an array of Objects
     */
    public Object[] getFormattedTrack(Track track) {
        if (track instanceof TemplateTrack) {
            TemplateTrack tti = (TemplateTrack) track;
            return new Object[] {tti.getTemplateName(), tti.getUserName(),
                                tti.getTimestamp(), tti.getSessionIdentifier()};
        } else {
            return null;
        }
    }

    /**
     * Return the tracker's name
     * @return String tracker's name
     */
    public static String getTrackerName() {
        return TRACKER_NAME;
    }

    /**
     * Return the tracker's name
     * @return String tracker's name
     */
    public String getName() {
        return TRACKER_NAME;
    }

}
