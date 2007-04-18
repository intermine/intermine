package org.intermine.path;

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
 * An Error thrown if there is a problem in the Path class.
 * @author Kim Rutherford
 */
public class PathError extends Error
{
    private final String pathString;

    /**
     * Create a new PathError with the given message
     * @param message the message
     * @param pathString the String representation of the Path that generated this Error - used
     * when the Path constructor is past a string that isn't a valid path
     */
    public PathError(String message, String pathString) {
        super(message);
        this.pathString = pathString;
    }

    /**
     * Return the path String that was passed to the constructor
     * @return the path string
     */
    public String getPathString() {
        return pathString;
    }
}
