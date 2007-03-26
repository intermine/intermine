package org.intermine.model;

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
 * A root interface for all objects that can be stored in a InterMine database.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public interface InterMineObject
{
    /**
     * Getter for the ID field - ensures that every object in the database has an ID.
     *
     * @return an Integer
     */
    public java.lang.Integer getId();
    /**
     * Setter for the ID field.
     *
     * @param id an Integer
     */
    public void setId(java.lang.Integer id);
}
