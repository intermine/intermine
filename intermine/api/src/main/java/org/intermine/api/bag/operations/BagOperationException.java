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
 * @author Alex
 */
public class BagOperationException extends Exception
{

    private static final long serialVersionUID = -7187065324138215891L;

    /**
     * Constructor
     */
    public BagOperationException() {
    }

    /**
     * @param message error message
     */
    public BagOperationException(String message) {
        super(message);
    }

    /**
     * @param cause error
     */
    public BagOperationException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message error message
     * @param cause error
     */
    public BagOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
