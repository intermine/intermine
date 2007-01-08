package org.intermine.path;

/* 
 * Copyright (C) 2002-2005 FlyMine
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
    /**
     * Create a new PathError with the given message
     * @param message the message
     */
    public PathError(String message) {
        super(message);
    }
}
