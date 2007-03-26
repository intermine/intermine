package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An interface for all classes that implement the getSQLString method.
 *
 * @author Matthew Wakeling
 */
public interface SQLStringable
{
    /**
     * Returns a String representation of this object, suitable for forming part of an SQL query.
     *
     * @return the String representation
     */
    public String getSQLString();
}
