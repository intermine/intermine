package org.intermine.dwr;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.directwebremoting.WebContextFactory;
import org.intermine.api.InterMineAPI;
import org.intermine.api.tracker.util.ListTrackerEvent;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.logic.session.SessionMethods;


public class TrackAjaxServices {
    protected static final Logger LOG = Logger.getLogger(TrackAjaxServices.class);
    private ObjectStore uos = null;
    public static String LAST_2WEEKS = "LAST2WEEKS";
    public static String LAST_MONTH = "LASTMONTH";
    public static String LAST_3MONTHES = "LAST3MONTHES";
    public static String LAST_YEAR = "LASTYEAR";

    public TrackAjaxServices() {
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        uos = im.getProfileManager().getProfileObjectStoreWriter().getObjectStore();
    }

    private List getTracksTrend(String tableName) {
        List tracksTrend = new ArrayList<Object[]>();
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

    public List getQueryTracksTrend() {
        return getTracksTrend("querytrack");
    }

    public List getTemplateTracksTrend() {
        return getTracksTrend("templatetrack");
    }

    public List getLoginTracksTrend() {
        return getTracksTrend("logintrack");
    }

    public List getSearchTracksTrend() {
        return getTracksTrend("searchtrack");
    }

    private List getListTracksTrend(String event) {
        List listTracksTrend = new ArrayList<Object[]>();

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

    public List getListExecutionTrend() {
        return getListTracksTrend(ListTrackerEvent.EXECUTION.toString());
    }

    public List getListCreationTrend() {
        return getListTracksTrend(ListTrackerEvent.CREATION.toString());
    }

    public List getTracksDataTable(String sqlQuery) {
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

    public List getQueryTracksDataTable(String timeRange) {
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

    public List getTemplateTracksPercentage(String timeRange) {
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
        final long ONEDAY = 1000 * 60 * 60 * 24;
        long currentDateMs = System.currentTimeMillis();
        long startTimeMs = -1;
        if (timeRange.equals(LAST_2WEEKS)) {
            startTimeMs = (currentDateMs - 14 * ONEDAY);
        } else if (timeRange.equals(LAST_MONTH)) {
            startTimeMs = (currentDateMs - 30 * ONEDAY);
        } else if (timeRange.equals(LAST_3MONTHES)) {
            startTimeMs = (currentDateMs - 30 * 3 * ONEDAY);
        } else if (timeRange.equals(LAST_YEAR)) {
            startTimeMs = (currentDateMs - 365 * ONEDAY);
        }
        Date startDate = new Date(startTimeMs);
        return startDate;
    }

    public List getSearchTracksDataTable(String timeRange) {
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

    public List getListExecutionTracksDataTable(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "'";
        String sql = "SELECT type, COUNT(type),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM listtrack WHERE username!=''"
                                      + " AND event='EXECUTION' AND type=lt.type AND"
                                      + timeStampConstraint
                                      + "GROUP BY username) AS listsubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM listtrack"
                                      + " WHERE username='' AND event='EXECUTION' AND type=lt.type AND"
                                      + timeStampConstraint
                                      + "GROUP BY sessionidentifier) AS listsubselect2)"
            + " FROM listtrack AS lt WHERE lt.timestamp > '" + startTimeRange.toString() + "'"
            + " AND lt.event='EXECUTION' GROUP BY lt.type ORDER BY COUNT(type) DESC LIMIT 20";
        return getTracksDataTable(sql);
    }

    public List getListCreationTracksDataTable(String timeRange) {
        Date startTimeRange = calculateDate(timeRange);
        String timeStampConstraint = " timestamp > '" + startTimeRange.toString() + "'";
        String sql = "SELECT type, COUNT(type),"
            + " (SELECT COUNT(*) FROM (SELECT username FROM listtrack WHERE username!=''"
                                      + " AND event='CREATION' AND type=lt.type AND"
                                      + timeStampConstraint
                                      + "GROUP BY username) AS listsubselect),"
            + " (SELECT COUNT(*) FROM (SELECT sessionidentifier FROM listtrack"
                                      + " WHERE username='' AND event='CREATION' AND type=lt.type AND"
                                      + timeStampConstraint
                                      + " GROUP BY sessionidentifier) AS listsubselect2)"
            + " FROM listtrack AS lt WHERE lt.timestamp > '" + startTimeRange.toString() + "'"
            + " AND lt.event='CREATION' GROUP BY lt.type ORDER BY COUNT(type) DESC LIMIT 20";
        return getTracksDataTable(sql);
    }
}
