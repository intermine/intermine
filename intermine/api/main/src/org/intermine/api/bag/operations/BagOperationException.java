package org.intermine.api.bag.operations;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class BagOperationException extends Exception {

    private static final long serialVersionUID = -7187065324138215891L;

    public BagOperationException() {
    }

    public BagOperationException(String message) {
        super(message);
    }

    public BagOperationException(Throwable cause) {
        super(cause);
    }

    public BagOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
