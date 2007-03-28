package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.InterMineObject;

/**
 * A class to represent the mapping between an object before type conversion and the converted
 * object.  This is returned when the key of the Map returned by getIssues() is TYPE_CONVERTED.
 * ie. getIssues() returns ["TYPE_CONVERTED" -> ["some message" -> ["input string" -> List of 
 * ConvertedObjectPair objects]]].
 * @author Kim Rutherford
 */
public class ConvertedObjectPair 
{
    private final InterMineObject oldObject;
    private final InterMineObject newObject;

    /**
     * Create a new ConvertedObjectPair.
     * @param oldObject the original object
     * @param newObject the object found by the TypeConverter class
     */
    public ConvertedObjectPair(InterMineObject oldObject, InterMineObject newObject) {
        this.oldObject = oldObject;
        this.newObject = newObject;
        
    }

    /**
     * Get the newObject that was passed to the constructor.
     * @return the newObject that was passed to the constructor.
     */
    public InterMineObject getNewObject() {
        return newObject;
    }

    /**
     * Get the objObject that was passed to the constructor.
     * @return the objObject that was passed to the constructor.
     */
    public InterMineObject getOldObject() {
        return oldObject;
    }
}