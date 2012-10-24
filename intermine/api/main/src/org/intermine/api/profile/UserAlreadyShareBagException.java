package org.intermine.api.profile;

import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;

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
    
    /**
     * Constructor with the actual objects we are trying to link.
     * @param bag The SavedBag we are trying to share
     * @param up The UserProfile we are trying to share this with.
     */
    public UserAlreadyShareBagException(SavedBag bag, UserProfile up) {
    	super(String.format("This bag (%s:%d) is already shared with this user (%s:%d)",
    			bag.getName(), bag.getId(), up.getUsername(), up.getId()));
    }
}
