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

public class LoginTrack extends TrackAbstract
{
    private String user;

    public LoginTrack(String user, Timestamp timestamp) {
        this.user = user;
        this.timestamp = timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFormattedTrack() {
        return new Object[] {user, timestamp};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTableName() {
        return TrackerUtil.LOGIN_TRACKER_TABLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate() {
        if (user != null && !"".equals(user)) {
            return true;
        }
        return false;
    }
}
