package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
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

    public ListTrack(String type, int count, ListBuildMode buildMode, ListTrackerEvent event,
                    long timestamp) {
        this.type = type;
        this.count = count;
        this.buildMode = buildMode;
        this.event = event;
        this.timestamp = timestamp;
    }

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
            return new Object[] {type, count, "", event, timestamp};
        }
        return new Object[] {type, count, buildMode, event, timestamp};
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
     * Return the time of execution of the list
     * @return long the time of execution
     */
    public long getTimestamp() {
        return timestamp;
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
