package org.intermine.api.query.codegen;

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
 * Exception representing the failure to encode a feature in the target
 * language.
 * @author Alex Kalderimis
 *
 */
public class UnhandledFeatureException extends Exception
{

    private static final long serialVersionUID = 1701853337768806064L;

    private static final String DEFAULT_MESSAGE = "This feature is not supported in this language";

    /**
     * Create an exception with a default message.
     */
    public UnhandledFeatureException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Create an exception with a given message.
     * @param message What to tell the user.
     */
    public UnhandledFeatureException(String message) {
        super(message);
    }

    /**
     * Create an exception when there is something to blame.
     * @param cause What caused this unfortunate situation.
     */
    public UnhandledFeatureException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    /**
     * Create an exception when there is a message and a cause
     * @param message What to tell the user.
     * @param cause What caused this unfortunate situation.
     */
    public UnhandledFeatureException(String message, Throwable cause) {
        super(message, cause);
    }

}
