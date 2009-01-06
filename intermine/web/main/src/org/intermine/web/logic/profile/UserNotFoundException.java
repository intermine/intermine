package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Exception thrown when user is not found.  
 * @author Jakub Kulaviak <jakub@flymine.org>
 */
public class UserNotFoundException extends RuntimeException
{
    /**
     * Constructor with message.
     * @param msg message
     */
    public UserNotFoundException(String msg) {
        super(msg);
    }
    
    /**
     * Constructor with message and nested exception.
     * @param msg message
     * @param t nested exception
     */
    public UserNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }
}
