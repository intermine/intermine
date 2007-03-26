package org.intermine.sql;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.intermine.util.PropertiesUtil;
import org.intermine.util.WeakReferenceHashMap;


/**
 * Creates Databases
 *
 * @author Andrew Varley
 */

public class DatabaseFactory
{

    protected static Map databases = new WeakReferenceHashMap();

    /**
     * Returns a connection to the named database
     *
     * @param instance the name of the database
     * @return a connection to that database
     * @throws SQLException if there is a problem with the underlying database
     * @throws ClassNotFoundException if the class that the instance uses cannot be found
     */
    public static Database getDatabase(String instance)
        throws SQLException, ClassNotFoundException {
        /*ClassLoader us = DatabaseFactory.class.getClassLoader();
        ClassLoader loader = us.getSystemClassLoader();
        if ((loader != null) && (! loader.equals(us))) {
            try {
                Class databaseFactory = loader.loadClass(DatabaseFactory.class.getName());
                Method method = databaseFactory.getDeclaredMethod("getDatabase",
                        new Class[] {String.class});
                return (Database) method.invoke(null, new Object[] {instance});
            } catch (IllegalAccessException e) {
                throw new ClassNotFoundException("Could not use parent classloader to create"
                        + " Database", e);
            } catch (IllegalArgumentException e) {
                throw new ClassNotFoundException("Could not use parent classloader to create"
                        + " Database", e);
            } catch (InvocationTargetException e) {
                throw new ClassNotFoundException("Could not use parent classloader to create"
                        + " Database", e);
            } catch (NullPointerException e) {
                throw new ClassNotFoundException("Could not use parent classloader to create"
                        + " Database", e);
            } catch (ExceptionInInitializerError e) {
                throw new ClassNotFoundException("Could not use parent classloader to create"
                        + " Database", e);
            } catch (NoSuchMethodException e) {
                throw new ClassNotFoundException("Could not use parent classloader to create"
                        + " Database", e);
            }
        } else {*/
            Database database;

            // Only one thread to configure or test for a DataSource
            synchronized (databases) {
                // If we have this DataSource already configured
                if (databases.containsKey(instance)) {
                    database = (Database) databases.get(instance);
                } else {
                    Properties props = PropertiesUtil.getPropertiesStartingWith(instance);
                    try {
                        database = new Database(PropertiesUtil.stripStart(instance, props));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialise " + instance, e);
                    }
                }
            }
            databases.put(instance, database);
            return database;
        /*}*/
    }
}
