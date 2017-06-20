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
 * The error that indicates we cannot decode something.
 * @author Alex Kalderimis
 *
 */
public class DecodingException extends Exception
{

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * @param cause The reason we could not decode the thing.
     */
    public DecodingException(Throwable cause) {
        super(cause);
    }
}
