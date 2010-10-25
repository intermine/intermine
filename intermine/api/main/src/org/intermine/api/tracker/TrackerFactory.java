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
import java.lang.reflect.Method;
import java.sql.Connection;

public class TrackerFactory
{
    private TrackerFactory() {
    }

    public static Tracker getTracker(String className, Connection con) throws Exception {
        Class<?> cls = null;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find specified Tracker class '" + className, e);
        }
        Class[] params = {Connection.class};
        Object[] paramsObj = {con};
        Method m = cls.getDeclaredMethod("getInstance", params);
        return (Tracker) m.invoke(null, paramsObj);
    }
}
