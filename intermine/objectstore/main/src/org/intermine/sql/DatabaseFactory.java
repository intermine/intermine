package org.intermine.sql;

/*
 * Copyright (C) 2002-2016 FlyMine
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

public final class DatabaseFactory
{
    private DatabaseFactory() {
    }

    protected static Map<String, Database> databases = new WeakReferenceHashMap<String, Database>();

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
        Database database;

        // Only one thread to configure or test for a DataSource
        synchronized (databases) {
            // If we have this DataSource already configured
            if (databases.containsKey(instance)) {
                database = databases.get(instance);
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
    }
}
