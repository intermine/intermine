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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;

/**
 * Class for tracking the accesses to the templates by the users.
 * @author dbutano
 */
public class TemplateTracker extends TrackerAbstract
{
    private static final Logger LOG = Logger.getLogger(TemplateTracker.class);
    private static TemplateTracker templateTracker = null;
    private static final String TRACKER_NAME = "TemplateTracker";
    private static TemplatesExecutionMap templatesExecutionCache;

    /**
     * Build a template tracker
     * @param conn connection to the database
     */
    protected TemplateTracker(Connection conn) {
        this.connection = conn;
        trackTableName = "templatetrack";
        trackTableColumns =
            new String[] {"templatename", "username", "timestamp", "sessionidentifier"};
        templatesExecutionCache = new TemplatesExecutionMap();
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
            templateTracker.loadTemplatesExecutionCache();
        }
        return templateTracker;
    }

    private void loadTemplatesExecutionCache() {
        Statement stm = null;
        ResultSet rs = null;
        try {
            stm = connection.createStatement();
            String sql = "SELECT tt.templatename, tt.username, tt.sessionidentifier, t.tagname "
                         + "FROM templatetrack tt LEFT JOIN tag t "
                         + "ON tt.templatename = t.objectidentifier "
                         + "AND t.type='" + TagTypes.TEMPLATE + "' "
                         + "AND t.tagname='" + TagNames.IM_PUBLIC+ "'";
            rs = stm.executeQuery(sql);
            TemplateTrack tt;
            while (rs.next()) {
                tt =  new TemplateTrack(rs.getString(1),
                                        (rs.getString(4) != null && rs.getString(4).equals(TagNames.IM_PUBLIC)) ? true : false,
                                        rs.getString(2),
                                        rs.getString(3));
                templatesExecutionCache.addExecution(tt);
            }
        } catch (SQLException sqle) {
            LOG.error("Error during loading template tracks into the cache", sqle);
        } finally {
            releaseResources(rs, stm);
        }
    }

    /**
     * Store into the database the template execution by the user and by session id
     * @param templateName the template name
     * @param isPublic true if the template has the tag public
     * @param profile the user profile
     * @param sessionIdentifier the session id
     */
    public void trackTemplate(String templateName, boolean isPublic, Profile profile,
                              String sessionIdentifier) {
        String userName = (profile != null && profile.getUsername() != null)
        ? profile.getUsername()
        : "";
        TemplateTrack templateTrack = new TemplateTrack(templateName, isPublic, userName, sessionIdentifier,
                                              System.currentTimeMillis());
        if (templateTracker  != null) {
            templateTracker.storeTrack(templateTrack);
            templatesExecutionCache.addExecution(templateTrack);
        } else {
            LOG.warn("Template not tracked. Check if the TemplateTracker has been configured");
        }
    }

    /**
     * Return the template name associated to the public template with the highest rank
     * @return String the template name
     */
    public String getMostPopularTemplate() {
        List<String> templateListOrdered = getMostPopularTemplateOrder();
        if (!templateListOrdered.isEmpty()) {
            return templateListOrdered.get(0);
        }
        return null;
    }

    /**
     * Return the list of public templates ordered by rank descendant.
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder() {
        return getMostPopularTemplateOrder(null, null);
    }

    /**
     * Return the template list ordered by rank descendant for the user/sessionid specified in the input
     * @param userName the user name
     * @param sessionId the session id
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder(String userName, String sessionId) {
        List<String> mostPopularTemplateOrder = new ArrayList<String>();
        Map<String, Double> templateLnRank = getLnMap(userName, sessionId, false);
        //create an entry list ordered
        List<Entry<String, Double>> listOrdered =
            new LinkedList<Entry<String, Double>>(templateLnRank.entrySet());
        Collections.sort(listOrdered, new Comparator<Entry<String, Double>>() {
            public int compare (Entry<String, Double> e1, Entry<String, Double> e2) {
                return -e1.getValue().compareTo(e2.getValue());
            }
        });
        for (Entry<String, Double> entry : listOrdered) {
            mostPopularTemplateOrder.add(entry.getKey());
        }
        return mostPopularTemplateOrder;
    }

    /**
     * Return the number of executions for each template
     * @return map with key the template name and executions number
     */
    public Map<String, Integer> getAccessCounter() {
        ResultSet rs = null;
        Statement stm = null;
        Map<String, Integer> templateRank = new HashMap<String, Integer>();
        try {
            stm = connection.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) accessnumbers "
                        + "FROM templatetrack tt, tag t "
                        + "WHERE tt.templatename=t.objectidentifier "
                        + "AND t.tagname LIKE '%public' AND t.type='template' "
                        + "GROUP BY tt.templatename";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                templateRank.put(rs.getString(1), rs.getInt(2));
            }
            return templateRank;
        } catch (SQLException sqle) {
            LOG.error("Error in getAccessCounter method: ", sqle);
        } finally {
            releaseResources(rs, stm);
        }
        return null;
    }

    /**
     * Return the rank for each template. The rank represents a relationship between the templates
     * execution; a template with rank 1 has been executed more than a template with rank 2. The
     * rank is calculated by summing the ln of the template execution launched by the user (if
     * logged in) or (if the user is not logged in) during the to the same http session
     * @return map with key the template name and executions number
     */
    public Map<String, Integer> getRank(String userName) {
        Map<String, Integer> templateRank = new HashMap<String, Integer>();
        Map<String, Double> templateMergedRank = getLnMap(userName, null, true);
        //prepare templateRank
        List<Entry<String, Double>> listOrdered =
            new LinkedList<Entry<String, Double>>(templateMergedRank.entrySet());
        Collections.sort(listOrdered, new Comparator<Entry<String, Double>>() {
            public int compare (Entry<String, Double> e1, Entry<String, Double> e2) {
                return -e1.getValue().compareTo(e2.getValue());
            }
        });

        int rankDisplayed = 0;
        Double prevValue = 0.0;
        for (Entry<String, Double> entry : listOrdered) {
            if (entry.getValue().doubleValue() != prevValue.doubleValue()) {
                rankDisplayed++;
                prevValue = entry.getValue();
            }
            templateRank.put(entry.getKey(), rankDisplayed);
        }
        templateRank.put("minRank", ++rankDisplayed);
        return templateRank;
    }

    public Map<String, Double> getLnMap(String userName, String sessionId, boolean isSuperUser) {
        Map<String, Double> templateUserLn = new HashMap<String, Double>();
        Map<String, Double> templateAnonymousLn = new HashMap<String, Double>();
        templateUserLn = templatesExecutionCache.getLnUserMap(userName, isSuperUser);
        templateAnonymousLn = templatesExecutionCache.getLnAnonymousMap(sessionId);

        //merge the templateAnonymousRank and the templateUserRank (summing the ln for each
        //tempalte) into a templateMergedRank
        Map<String, Double> templateMergedLn = mergeMap(templateUserLn, templateAnonymousLn);
        return templateMergedLn;
    }

    private Map<String, Double> mergeMap (Map<String, Double> map1, Map<String, Double> map2) {
        Map<String, Double> mergedMap = new HashMap<String, Double>();
        Map<String, Double> tempMap = new HashMap<String, Double>(map2);
        double r1 = 0;
        double r2 = 0;
        for (String templateName : map1.keySet()) {
            r1 = map1.get(templateName);
            if (tempMap.containsKey(templateName)) {
                r2 = tempMap.get(templateName);
                tempMap.remove(templateName);
            }
            mergedMap.put(templateName, r1 + r2);
            r1 = 0;
            r2 = 0;
        }
        for (String templateName : tempMap.keySet()) {
            mergedMap.put(templateName, tempMap.get(templateName));
        }
        return mergedMap;
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
            LOG.error("Problem during updating templatename in updateTemplateName() ,method", sqe);
        } finally {
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
