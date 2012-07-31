package org.intermine.api.tracker.factory;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Queue;

import org.intermine.api.tracker.Tracker;
import org.intermine.api.tracker.track.Track;

/**
 * Factory to instantiate the trackers with the reflection
 * @author dbutano
 */
public final class TrackerFactory
{
    private TrackerFactory() {
    }

    /**
     * Return the tracker instantiated with the reflection
     * @param className name of the class to instantiate
     * @param con connection to the database
     * @return the tracker instantiated
     * @throws Exception
     */
    public static Tracker getTracker(String className, Connection con, Queue<Track> trackQueue)
        throws Exception {
        Class<?> cls = null;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find specified Tracker class '" + className, e);
        }
        Class[] params = {Connection.class, Queue.class};
        Object[] paramsObj = {con, trackQueue};
        Method m = cls.getDeclaredMethod("getInstance", params);
        return (Tracker) m.invoke(null, paramsObj);
    }
}
