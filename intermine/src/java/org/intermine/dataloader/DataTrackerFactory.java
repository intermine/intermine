package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.util.PropertiesUtil;

/**
 * Produce DataTrackers
 *
 * @author Matthew Wakeling
 */
public class DataTrackerFactory
{
    /**
     * Return a DataTracker configured using the properties file
     *
     * @param alias identifier for properties defining parameters
     * @return instance of a DataTracker
     */
    public static DataTracker getDataTracker(String alias) {
        if (alias == null) {
            throw new NullPointerException("DataTracker alias cannot be null");
        }
        if (alias.equals("")) {
            throw new IllegalArgumentException("DataTracker alias cannot be empty");
        }
        Properties props = PropertiesUtil.getPropertiesStartingWith(alias);
        if (props.size() == 0) {
            throw new IllegalArgumentException("No DataTracker properties were found for " + alias);
        }
        props = PropertiesUtil.stripStart(alias, props);
        String dbName = props.getProperty("db");
        if (dbName == null) {
            throw new IllegalArgumentException("No db for DataTracker " + alias);
        }

        String maxSizeString = props.getProperty("maxSize");
        if (maxSizeString == null) {
            throw new IllegalArgumentException("No maxSize for DataTracker " + alias);
        }

        String commitSizeString = props.getProperty("commitSize");
        if (commitSizeString == null) {
            throw new IllegalArgumentException("No commitSize for DataTracker " + alias);
        }

        try {
            int maxSize = Integer.parseInt(maxSizeString);
            int commitSize = Integer.parseInt(commitSizeString);
            Database db = DatabaseFactory.getDatabase(dbName);
            if (db == null) {
                throw new NullPointerException("DB " + dbName + " for DataTracker not found");
            }
            return new DataTracker(db, maxSize, commitSize);
        } catch (Exception e) {
            IllegalArgumentException e2 = new IllegalArgumentException("Problem instantiating"
                    + " DataTracker " + alias + " with db = " + dbName + ", maxSize = "
                    + maxSizeString + ", commitSize = " + commitSizeString);
            e2.initCause(e);
            throw e2;
        }
    }
}

