package org.intermine.sql;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;

/**
 * Exception thrown on failing to object a database connection. This a specific subclass of
 * SQLException in order to distinguish connection errors from other SQL failures.
 * @author rns
 *
 */
public class DatabaseConnectionException extends SQLException
{

    /**
     * Constructs an DatabaseConnectionException
     */
    public DatabaseConnectionException() {
        super();
    }

    /**
     * Constructs an ObjectStoreException with the specified detail message.
     *
     * @param msg the detail message
     */
    public DatabaseConnectionException(String msg) {
        super(msg);
    }

    /**
     * Constructs an ObjectStoreException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public DatabaseConnectionException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an ObjectStoreException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public DatabaseConnectionException(String msg, Throwable t) {
        super(msg, t);
    }
}

