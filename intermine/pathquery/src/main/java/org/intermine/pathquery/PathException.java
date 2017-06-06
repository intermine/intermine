package org.intermine.pathquery;

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
 * An Exception thrown if there is a problem in the Path class.
 * @author Kim Rutherford
 */
public class PathException extends Exception
{
    private static final long serialVersionUID = 17668934838206563L;
    private final String pathString;

    /**
     * Create a new PathException with the given message.
     *
     * @param message the message
     * @param pathString the String representation of the Path that generated this exception - used
     * when the Path constructor is passed a string that isn't a valid path
     */
    public PathException(String message, String pathString) {
        super(message);
        this.pathString = pathString;
    }

    /**
     * Return the path String that was passed to the constructor.
     *
     * @return the path string
     */
    public String getPathString() {
        return pathString;
    }
}
