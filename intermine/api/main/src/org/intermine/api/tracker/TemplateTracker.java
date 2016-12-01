package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.tracker.track.TemplateTrack;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.api.template.TemplateManager;

/**
 * Class for tracking the templates execution by the users. When a user executes a template,
 * a new track is saved into the database and saved into the memory too.
 * @author dbutano
 */
public class TemplateTracker extends AbstractTracker
{
    private static final Logger LOG = Logger.getLogger(TemplateTracker.class);
    private static TemplateTracker templateTracker = null;
    private static TemplatesExecutionMap templatesExecutionCache;

    /**
     *
     * @param trackQueue queue
     */
    protected TemplateTracker(Queue<Track> trackQueue) {
        super(trackQueue, TrackerUtil.TEMPLATE_TRACKER_TABLE);
        templatesExecutionCache = new TemplatesExecutionMap();
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }

    /**
     * Return an instance of the TemplateTracker
     * @param con connection to the database
     * @param trackQueue queue
     * @return TemplateTracker the template tracker
     */
    public static TemplateTracker getInstance(Connection con, Queue<Track> trackQueue) {
        if (templateTracker == null) {
            templateTracker = new TemplateTracker(trackQueue);
            try {
                templateTracker.createTrackerTable(con);
            } catch (Exception e) {
                LOG.error("Error creating the table associated to the TemplateTracker" + e);
            }
            TemplateTracker.loadTemplatesExecutionCache(con);
        } else {
            templateTracker.setTrackQueue(trackQueue);
        }
        return templateTracker;
    }

    /**
     * Load the tracks retrieved from the database into TemplateExecutionMap object.
     */
    private static void loadTemplatesExecutionCache(Connection con) {
        PreparedStatement stm = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT tt.templatename, tt.username, tt.sessionidentifier"
                         + " FROM templatetrack tt"
                         + " LEFT JOIN tag t ON tt.templatename = t.objectidentifier"
                         + " WHERE t.type = ? AND t.tagname = ?";
            stm = con.prepareStatement(sql);
            stm.setString(1, TagTypes.TEMPLATE);
            stm.setString(2, TagNames.IM_PUBLIC);
            rs = stm.executeQuery();
            TemplateTrack tt;
            while (rs.next()) {
                tt = new TemplateTrack(rs.getString(1), rs.getString(2), rs.getString(3));
                templatesExecutionCache.addExecution(tt);
            }
        } catch (SQLException sqle) {
            LOG.error("Error during loading template tracks into the cache", sqle);
        }
    }

    /**
     * Store into the database the template execution by the user or if the user is not logged
     * during a specific http session. Update the cache.
     * @param templateName the template name
     * @param profile the user profile
     * @param sessionIdentifier the session id
     */
    protected void trackTemplate(String templateName, Profile profile,
                              String sessionIdentifier) {
        String userName = (profile.getUsername() != null)
                          ? profile.getUsername()
                          : "";
        TemplateTrack templateTrack = new TemplateTrack(templateName, userName, sessionIdentifier,
                new Timestamp(System.currentTimeMillis()));
        if (templateTracker  != null) {
            templateTracker.storeTrack(templateTrack);
            templatesExecutionCache.addExecution(templateTrack);
        } else {
            LOG.warn("Template not tracked. Check if the TemplateTracker has been configured");
        }
    }

    /**
     * Return the number of executions for each public template
     * @param con db connection
     * @return map with key the template name and executions number
     */
    protected Map<String, Integer> getAccessCounter(Connection con) {
        ResultSet rs = null;
        Statement stm = null;
        Map<String, Integer> templateRank = new HashMap<String, Integer>();
        try {
            stm = con.createStatement();
            String sql = "SELECT tt.templatename, COUNT(tt.templatename) as accessnumbers "
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
     * Return the rank for each public template.It represents a relationship between the templates
     * executions; a template with rank 1 has been executed more than a template with rank 2. The
     * rank is calculated by summing the logarithm of the templates executions launched by the
     * single users, if the user is logged in, or otherwise, by summing the logarithm of the
     * templates executions during the same http session. The function is called only by the
     * super user
     * @param templateManager the template manager used to retrieve the global templates
     * @return map with key the template name and rank
     */
    protected Map<String, Integer> getRank(TemplateManager templateManager) {
        Map<String, Integer> templateRank = new HashMap<String, Integer>();
        Map<String, Double> templateMergedRank = templatesExecutionCache.getLogarithmMap(null,
                                                                              templateManager);

        //order the templateMergedRank by value descending
        List<Entry<String, Double>> listOrdered =
            new LinkedList<Entry<String, Double>>(templateMergedRank.entrySet());
        Collections.sort(listOrdered, new Comparator<Entry<String, Double>>() {
            @Override
            public int compare (Entry<String, Double> e1, Entry<String, Double> e2) {
                return -e1.getValue().compareTo(e2.getValue());
            }
        });

        //assign rank 1 to the template with higher value
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

    /**
     * Return a map containing the logarithm of accesses for each template. The map is obtained
     * merging the map of logarithm of accesses for single users and the map of logarithm of
     * accesses for the single sessions
     * @param userName the user name
     * @param sessionId the http session identifier
     * @param templateManager the template manager used to retrieve the global templates
     * @return map of logarithm of accesses
     */
    public Map<String, Double> getLogarithmMap(String userName, String sessionId,
                                               TemplateManager templateManager) {
        Map<String, Double> logarithmMap;
        if (userName == null && sessionId == null) {
            logarithmMap = templatesExecutionCache.getLogarithmMap(null, templateManager);
        } else {
            Map<String, Double> executionsByUser = new HashMap<String, Double>();
            Map<String, Double> executionsBySessionId = new HashMap<String, Double>();
            executionsByUser = templatesExecutionCache.getLogarithmMap(userName, templateManager);
            executionsBySessionId = templatesExecutionCache.getLogarithmMap(sessionId,
                                                                            templateManager);
            logarithmMap = mergeMap(executionsByUser, executionsBySessionId);
        }
        return logarithmMap;
    }

    /**
     * Return a map obtained merging the two maps specified in input. If the two maps have an entry
     * with the same key (template name), the result will be an entry with the same key and value
     * the sum of the two values.
     * @param map1 the map to merge
     * @param map2 the map to merge
     * @return a map containing the map1 and map2 merged
     */
    private static Map<String, Double> mergeMap (Map<String, Double> map1,
            Map<String, Double> map2) {
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
     * Update the template name value into the database
     * @param oldTemplateName the old name
     * @param newTemplateName the new name
     * @param con the connection
     */
    protected void updateTemplateName(String oldTemplateName, String newTemplateName,
                                      Connection con) {
        Statement stm = null;
        try {
            stm = con.createStatement();
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
     * {@inheritDoc}
     */
    @Override
    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName
            + "(templatename text, username text, sessionidentifier text, timestamp timestamp)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return TrackerUtil.TEMPLATE_TRACKER;
    }
}
