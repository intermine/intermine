package org.intermine.api.mines;

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
 * An error caused when trying to configure something.
 * @author Alex Kalderimis
 *
 */
public final class ConfigurationException extends Exception
{

    /**
     * Construct an exception with a message.
     * @param message A description of what went wrong.
     */
    public ConfigurationException(String message) {
        super(message);
    }
}
