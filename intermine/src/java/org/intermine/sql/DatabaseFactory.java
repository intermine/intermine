package org.flymine.sql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.sql.SQLException;
import org.flymine.util.PropertiesUtil;


/**
 * Creates Databases
 *
 * @author Andrew Varley
 */

public class DatabaseFactory
{

    protected static Map databases = new HashMap();

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
                database = (Database) databases.get(instance);
            } else {
                Properties props = PropertiesUtil.getPropertiesStartingWith(instance);
                database = new Database(PropertiesUtil.stripStart(instance, props));
            }
        }
        databases.put(instance, database);
        return database;

    }

}
