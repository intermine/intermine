package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Interface to define the types of things that can have from elements added to them.
 * @author Alex Kalderimis
 *
 */
public interface HasFromList
{

    /**
     * Adds a FromElement to the FROM clause of this Query
     *
     * @param cls the FromElement to be added
     */
    void addFrom(FromElement cls);

}
