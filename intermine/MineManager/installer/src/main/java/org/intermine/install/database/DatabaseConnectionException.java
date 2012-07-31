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
 * An exception for errors when connecting to an Intermine database.
 */
public class DatabaseConnectionException extends Exception
{
    private static final long serialVersionUID = -3000273506016346732L;

    /**
     * Constructs a new instance with no message or root cause.
     */
    public DatabaseConnectionException() {
    }

    /**
     * Constructs a new instance with the given message message and no root cause.
     * 
     * @param message The error message.
     */
    public DatabaseConnectionException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance with no message and the given root cause.
     * 
     * @param cause The original error that caused this.
     */
    public DatabaseConnectionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new instance with the given message and root cause.
     * 
     * @param message The error message.
     * @param cause The original error that caused this.
     */
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
