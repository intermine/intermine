package org.intermine.install.database;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An exception for when the credentials given for a database connection
 * are incorrect.
 */
public class PasswordException extends DatabaseConnectionException
{
    private static final long serialVersionUID = -8095480967064818807L;

    /**
     * The name of the database causing this error.
     * @serial
     */
    private String database;
    
    /**
     * Constructs a new instance with the given database name.
     * 
     * @param database The database name.
     */
    public PasswordException(String database) {
        this(database, null);
    }

    /**
     * Constructs a new instance with the given database name and an
     * error message.
     * 
     * @param database The database name.
     * @param message The error message.
     */
    public PasswordException(String database, String message) {
        super(message);
        this.database = database;
    }

    /**
     * Get the database name.
     * @return The database name.
     */
    public String getDatabase() {
        return database;
    }

}
