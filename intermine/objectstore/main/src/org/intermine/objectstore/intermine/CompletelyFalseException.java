package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreException;

/**
 * Exception thrown if a Constraint is deemed to be completely false.
 *
 * @author Matthew Wakeling
 */
public class CompletelyFalseException extends ObjectStoreException
{
    /**
     * Constructor.
     */
    public CompletelyFalseException() {
        super();
    }

    /**
     * Constructor.
     *
     * @param msg the detail message
     */
    public CompletelyFalseException(String msg) {
        super(msg);
    }

    /**
     * Constructor.
     *
     * @param t the nested Throwable
     */
    public CompletelyFalseException(Throwable t) {
        super(t);
    }

    /**
     * Constructor.
     *
     * @param msg the detail message
     * @param t the nested Throwable
     */
    public CompletelyFalseException(String msg, Throwable t) {
        super(msg, t);
    }
}
