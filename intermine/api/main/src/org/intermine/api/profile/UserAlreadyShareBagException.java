package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Exception thrown when a user already shares the bag.
 * @author Daneila Butano
 */

public class UserAlreadyShareBagException extends RuntimeException
{

    /**
     * Constructor
     */
    public UserAlreadyShareBagException() {
    }

    /**
     * Constructor with message.
     * @param msg message
     */
    public UserAlreadyShareBagException(String msg) {
        super(msg);
    }

    /**
     * Constructor with throwable
     * @param t another throwable
     */
    public UserAlreadyShareBagException(Throwable t) {
        super(t);
    }

    /**
     * Constructor with message and throwable
     * @param msg message
     * @param t another throwable
     */
    public UserAlreadyShareBagException(String msg, Throwable t) {
        super(msg, t);
    }
}
