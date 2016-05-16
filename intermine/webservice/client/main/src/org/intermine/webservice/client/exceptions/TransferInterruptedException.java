package org.intermine.webservice.client.exceptions;

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
 * TransferInterruptedException is the error thrown when
 * a service returns an incomplete result set.
 *
 * @author Alexis Kalderimis
 */
public class TransferInterruptedException extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public TransferInterruptedException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public TransferInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause cause
     */
    public TransferInterruptedException(Throwable cause) {
        super(cause);
    }

    /**
     * Construct a new TransferInterrupted Exception.
     */
    public TransferInterruptedException() {
        super();
    }
}
