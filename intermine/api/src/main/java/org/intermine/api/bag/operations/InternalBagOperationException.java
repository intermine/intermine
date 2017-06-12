package org.intermine.api.bag.operations;

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
 * Exceptions that are the fault of underlying infrastructure, and not the way the
 * classes themselves were used. ie. "our fault".
 * @author alex
 *
 */
public class InternalBagOperationException extends BagOperationException
{

    private static final long serialVersionUID = -5954984605945169071L;

    /**
     * Constructor
     */
    public InternalBagOperationException() {
    }

    /**
     * @param message error message
     */
    public InternalBagOperationException(String message) {
        super(message);
    }

    /**
     * @param cause exception
     */
    public InternalBagOperationException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message error message
     * @param cause exception
     */
    public InternalBagOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
