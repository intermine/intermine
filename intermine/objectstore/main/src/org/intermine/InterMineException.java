package org.intermine;

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
 * An Exception that may be thrown by client facing parts
 * of InterMine code.
 *
 * @author Richard Smith
 */
public class InterMineException extends Exception
{
    /**
     * Constructs an InterMineException
     */
    public InterMineException() {
        super();
    }

    /**
     * Constructs an InterMineException with the specified detail message.
     *
     * @param msg the detail message
     */
    public InterMineException(String msg) {
        super(msg);
    }

    /**
     * Constructs an InterMineException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public InterMineException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an InterMineException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public InterMineException(String msg, Throwable t) {
        super(msg, t);
    }
}
