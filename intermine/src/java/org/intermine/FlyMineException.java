package org.intermine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An Exception that may be thrown by client facing parts
 * of FlyMine code.
 *
 * @author Richard Smith
 */
public class FlyMineException extends Exception
{
    /**
     * Constructs an FlyMineException
     */
    public FlyMineException() {
        super();
    }

    /**
     * Constructs an FlyMineException with the specified detail message.
     *
     * @param msg the detail message
     */
    public FlyMineException(String msg) {
        super(msg);
    }

    /**
     * Constructs an FlyMineException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public FlyMineException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an FlyMineException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public FlyMineException(String msg, Throwable t) {
        super(msg, t);
    }
}
