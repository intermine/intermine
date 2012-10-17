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
 * Exception thrown when template has invalid name.
 * @author Daneila Butano
 */
public class BadTemplateException extends Exception
{

    /**
     * Constructor
     */
    public BadTemplateException() {
    }

    /**
     * Constructor with message.
     * @param msg message
     */
    public BadTemplateException(String msg) {
        super(msg);
    }

}
