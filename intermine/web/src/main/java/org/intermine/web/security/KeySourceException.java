package org.intermine.web.security;

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
 * Errors encountered when using a PublicKeySource
 * @author Alex Kalderimis
 *
 */
public class KeySourceException extends Exception
{

    private static final long serialVersionUID = 1861648517209072150L;

    /**
     * An error caused by another error
     * @param cause The reason we could not continue.
     **/
    public KeySourceException(Throwable cause) {
        super(cause);
    }
}
