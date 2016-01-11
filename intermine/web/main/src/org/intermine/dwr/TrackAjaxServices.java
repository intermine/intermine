package org.intermine.dwr;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.tracker.util.ListTrackerEvent;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.context.InterMineContext;

/**
 * @author Daniela Butano
 *
 */
public class TrackAjaxServices
{
    protected static final Logger LOG = Logger.getLogger(TrackAjaxServices.class);

    private ObjectStore uos = null;
    private static final String LAST_2WEEKS = "LAST2WEEKS";
    private static final String LAST_MONTH = "LASTMONTH";
    private static final String LAST_90_DAYS = "LAST3MONTHES";
    private static final String LAST_YEAR = "LASTYEAR";
    private static final long ONEDAY = 1000L * 60 * 60 * 24;

    /**
     * Construct a ajax services object that does tracking stuff.
     */
    public TrackAjaxServices() {
        final InterMineAPI im = InterMineContext.getInterMineAPI();
        uos = im.getProfileManager().getProfileObjectStoreWriter().getObjectStore();
    }

    private List<Object[]> getTracksTrend(String tableName) {
        List<Object[]> tracksTrend = new ArrayList<Object[]>();
        String sql = "SELECT date_part('doy', timestamp) AS day, COUNT(timestamp) "
                   + "FROM " + tableName + " GROUP BY date_part('doy', timestamp)";
        Connection connection = null;
        Calendar calendar = Calendar.getInstance();
        try {
            connection = ((ObjectStoreInterMineImpl) uos).getConnection();
            Statement stm = connection.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            int dayOfYear;
            Object[] track;
            while (rs.next()) {
                track = new Object[2];
                dayOfYear = rs.getInt(1);
                calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
                track[0] = calendar.getTime();
                track[1] = rs.getInt(2);
                tracksTrend.add(track);
            }
            ((ObjectStoreInterMineImpl) uos).releaseConnection(connection);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return tracksTrend;
    }

    /**  @return the overall query tracks trend. **/
    public List<Object[]> getQueryTracksTrend() {
        return getTracksTrend("querytrack");
    }

    /** @return the overall template tracks trend. **/
    public List<Object[]> getTemplateTracksTrend() {
        return getTracksTrend("templatetrack");
    }

    /** @return the login tracks trend **/
    public List<Object[]> getLoginTracksTrend() {
        return getTracksTrend("logintrack");
    }

    /** @return the search tracks trend **/
    public List<Object[]> getSearchTracksTrend() {
        return getTracksTrend("searchtrack");
    }

    private List<Object[]> getListTracksTrend(String event) {
        List<Object[]> listTracksTrend = new ArrayList<Object[]>();

        String sql = "SELECT date_part('doy', timestamp) AS day, COUNT(timestamp)"
                   + " FROM listtrack WHERE event='" + event + "'"
                   + " GROUP BY date_part('doy', timestamp)";
        Connection connection = null;
        Calendar calendar = Calendar.getInstance();
        try {
            connection = ((ObjectStoreInterMineImpl) uos).getConnection();
            Statement stm = connection.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            int dayOfYear;
            Object[] track;
            while (rs.next()) {
                track = new Object[2];
                dayOfYear = rs.getInt(1);
                calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
                track[0] = calendar.getTime();
                track[1] = rs.getInt(2);
                listTracksTrend.add(track);
            }
            ((ObjectStoreInterMineImpl) uos).releaseConnection(connection);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return listTracksTrend;
    }

    /** @return the overall list execution trend **/
    public List<Object[]> getListExecutionTrend() {
        return getListTracksTrend(ListTrackerEvent.EXECUTION.toString());
    }

    /** @return the overall list creation trend **/
    public List<Object[]> getListCreationTrend() {
        return getListTracksTrend(ListTrackerEvent.CREATION.toString());
    }

    private List<Object[]> getTracksDataTable(String sqlQuery) {
        List<Object[]> trackTable = new ArrayList<Object[]>();
        Connection connection = null;
        try {
            connection = ((ObjectStoreInterMineImpl) uos).getConnection();
            Statement stm = connection.createStatement();
            ResultSet rs = stm.executeQuery(sqlQuery);
            Object[] track;
            while (rs.next()) {
                track = new Object[3];
                track[0] = rs.getString(1);
                track[1] = rs.getInt(2);
                track[2] = rs.getInt(3) + rs.getInt(4);
                trackTable.add(track);
            }
            ((ObjectStoreInterMineImpl) uos).releaseConnection(connection);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return trackTable;
    }

    /**
     * @param timeRange The time range to search within.
     * @return The query tracks in that time-range.
     */
    public List<Object[]> getQueryTracksDataTable(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "' ";
        String sql = "SELECT type, COUNT(type),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM querytrack WHERE username!=''"
                                      + " AND type=qt.type AND" + timeStampConstraint
                                      + " GROUP BY username) AS querysubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM querytrack"
                                      + " WHERE username='' AND type=qt.type AND"
                                      + timeStampConstraint
                                      + " GROUP BY sessionidentifier) AS querysubselect2)"
            + " FROM querytrack AS qt WHERE qt.timestamp > '" + startTimeRange.toString() + "'"
            + " GROUP BY qt.type ORDER BY COUNT(type) DESC LIMIT 20";
        return getTracksDataTable(sql);
    }

    /**
     * @param timeRange The time range to search within.
     * @return The template tracks in that time-range.
     */
    public List<Object[]> getTemplateTracksPercentage(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "'";
        String sql = "SELECT templatename, COUNT(templatename),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM templatetrack WHERE username!='' "
                                   + "AND templatename=tt.templatename AND" + timeStampConstraint
                                   + "GROUP BY username) AS templatesubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM templatetrack"
                                    + " WHERE username='' AND templatename=tt.templatename AND"
                                    + timeStampConstraint + " GROUP BY sessionidentifier) "
                                    + "AS templatesubselect2)"
            + " FROM templatetrack AS tt WHERE tt.timestamp > '" + startTimeRange.toString() + "'"
            + " GROUP BY tt.templatename ORDER BY COUNT(templatename) DESC LIMIT 20";
        return getTracksDataTable(sql);
    }


    private Date calculateDate(String timeRange) {
        long currentDateMs = System.currentTimeMillis();
        long startTimeMs;
        if (StringUtils.isBlank(timeRange)) {
            startTimeMs = -1;
        } else if (timeRange.equals(LAST_2WEEKS)) {
            startTimeMs = (currentDateMs - 14 * ONEDAY);
        } else if (timeRange.equals(LAST_MONTH)) {
            startTimeMs = (currentDateMs - 30 * ONEDAY);
        } else if (timeRange.equals(LAST_90_DAYS)) {
            startTimeMs = (currentDateMs - 30 * 3 * ONEDAY);
        } else if (timeRange.equals(LAST_YEAR)) {
            startTimeMs = (currentDateMs - 365 * ONEDAY);
        } else {
            throw new RuntimeException("Unknown time range: " + timeRange);
        }
        Date startDate = new Date(startTimeMs);
        return startDate;
    }

    /**
     * @param timeRange The time range to search within.
     * @return The list search tracks in that time-range.
     */
    public List<Object[]> getSearchTracksDataTable(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "'";
        String sql = "SELECT keyword, COUNT(keyword),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM searchtrack WHERE username!=''"
                                      + " AND keyword=st.keyword AND" + timeStampConstraint
                                      + "GROUP BY username) AS searchsubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM searchtrack"
                                      + " WHERE username='' AND keyword=st.keyword AND"
                                      + timeStampConstraint
                                      + "GROUP BY sessionidentifier) AS searchsubselect2)"
            + " FROM searchtrack AS st WHERE st.timestamp > '" + startTimeRange.toString() + "'"
            + " GROUP BY st.keyword ORDER BY COUNT(keyword) DESC LIMIT 50";
        return getTracksDataTable(sql);
    }

    /**
     * @param timeRange The time range to search within.
     * @return The list execution tracks in that time-range.
     */
    public List<Object[]> getListExecutionTracksDataTable(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "'";
        String sql = "SELECT type, COUNT(type),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM listtrack WHERE username!=''"
                                      + " AND event='EXECUTION' AND type=lt.type AND"
                                      + timeStampConstraint
                                      + "GROUP BY username) AS listsubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM listtrack"
                                      + " WHERE username='' AND event='EXECUTION'"
                                      + " AND type=lt.type AND"
                                      + timeStampConstraint
                                      + "GROUP BY sessionidentifier) AS listsubselect2)"
            + " FROM listtrack AS lt WHERE lt.timestamp > '" + startTimeRange.toString() + "'"
            + " AND lt.event='EXECUTION' GROUP BY lt.type ORDER BY COUNT(type) DESC LIMIT 20";
        return getTracksDataTable(sql);
    }

    /**
     * @param timeRange The time range to search within.
     * @return The list creation tracks in that time-range.
     */
    public List<Object[]> getListCreationTracksDataTable(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "'";
        String sql = "SELECT type, COUNT(type),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM listtrack WHERE username!=''"
                                      + " AND event='CREATION' AND type=lt.type AND"
                                      + timeStampConstraint
                                      + "GROUP BY username) AS listsubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM listtrack"
                                      + " WHERE username='' AND event='CREATION'"
                                      + " AND type=lt.type AND"
                                      + timeStampConstraint
                                      + " GROUP BY sessionidentifier) AS listsubselect2)"
            + " FROM listtrack AS lt WHERE lt.timestamp > '" + startTimeRange.toString() + "'"
            + " AND lt.event='CREATION' GROUP BY lt.type ORDER BY COUNT(type) DESC LIMIT 20";
        return getTracksDataTable(sql);
    }
}
