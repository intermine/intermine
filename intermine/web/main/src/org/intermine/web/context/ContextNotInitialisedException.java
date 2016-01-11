package org.intermine.web.context;

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
 * An error thrown when the InterMineContext object has not been initialised.
 * @author Richard Smith
 * @author Alex Kalderimis
 *
 */
public final class ContextNotInitialisedException extends RuntimeException
{

    /**
     * @param message The message.
     */
    public ContextNotInitialisedException(String message) {
        super(message);
    }

}
