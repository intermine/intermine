package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Timestamp;

import org.intermine.api.tracker.util.TrackerUtil;



public class KeySearchTrack extends TrackAbstract {
    private String keyword;

    public KeySearchTrack(String keyword, String username,
            String sessionIdentifier, Timestamp timestamp) {
        this.keyword = keyword;
        this.userName = username;
        this.sessionIdentifier = sessionIdentifier;
        this.timestamp = timestamp;
    }

    @Override
    public Object[] getFormattedTrack() {
        return new Object[] {keyword, userName, sessionIdentifier, timestamp};
    }

    @Override
    public String getTableName() {
        return TrackerUtil.SEARCH_TRACKER_TABLE;
    }

    @Override
    public boolean validate() {
        if (keyword != null && !"".equals(keyword)) {
            return true;
        }
        return false;
    }
}
