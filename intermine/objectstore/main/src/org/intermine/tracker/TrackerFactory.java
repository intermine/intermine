package org.intermine.tracker;

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
