package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Timestamp;

import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.api.tracker.util.ListTrackerEvent;
import org.intermine.api.tracker.util.TrackerUtil;

/**
 * Class to represent the track for the lists objects.
 * The track contains the type, the count of the items, the way used to build the list.
 * @author dbutano
 */
public class ListTrack extends TrackAbstract
{
    private String type;
    private int count;
    private ListBuildMode buildMode;
    private ListTrackerEvent event;

    /**
     * @param type the class type of items contained into the list
     * @param count number of items contained into the list
     * @param buildMode the way the list is built
     * @param event type of event to track (creation or list execution )
     * @param username user who created list
     * @param sessionIdentifier session
     * @param timestamp time stamp
     */
    public ListTrack(String type, int count, ListBuildMode buildMode, ListTrackerEvent event,
                     String username, String sessionIdentifier, Timestamp timestamp) {
        this.type = type;
        this.count = count;
        this.buildMode = buildMode;
        this.event = event;
        this.userName = username;
        this.sessionIdentifier = sessionIdentifier;
        this.timestamp = timestamp;
    }

    /**
     *
     * @param type the class type of items contained into the list
     * @param count number of items contained into the list
     * @param buildMode the way the list is built
     * @param event type of event to track (creation or list execution )
     */
    public ListTrack(String type, int count, ListBuildMode buildMode, ListTrackerEvent event) {
        this.type = type;
        this.count = count;
        this.buildMode = buildMode;
        this.event = event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate() {
        if (type != null && !"".equals(type)) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFormattedTrack() {
        if (buildMode == null) {
            return new Object[] {type, count, "", event, userName, sessionIdentifier, timestamp};
        }
        return new Object[] {type, count, buildMode, event, userName, sessionIdentifier, timestamp};
    }

    /**
     * Return the class type of items contained into the list
     * @return String the class type
     */
    public String getType() {
        return type;
    }

    /**
     * Return the number of items contained into the list
     * @return int the number of items
     */
    public int getCount() {
        return count;
    }

    /**
     * Return the way the list is built
     * @return ListBuildMode
     */
    public ListBuildMode getBuildMode() {
        return buildMode;
    }

    /**
     * Return the type of event to track (creation or list execution )
     * @return ListTrackerEvent the event
     */
    public ListTrackerEvent getEvent() {
        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTableName() {
        return TrackerUtil.LIST_TRACKER_TABLE;
    }
}
