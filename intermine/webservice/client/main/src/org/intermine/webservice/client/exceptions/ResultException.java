package org.intermine.webservice.client.exceptions;

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
 * ResultException is the exception thrown when a request fails.
 *
 * @author Alexis Kalderimis
 */
public class ResultException extends RuntimeException
{

    private final String errorReason;

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public ResultException(String message) {
        super(message);
        errorReason = null;
    }

    /**
     * @param message message
     * @param cause cause
     */
    public ResultException(String message, Throwable cause) {
        super(message, cause);
        errorReason = null;
    }

    /**
     * @param cause cause
     */
    public ResultException(Throwable cause) {
        super(cause);
        errorReason = null;
    }

    /**
    * Construct an informative exception.
    * Result sets may provide two separate strings - the message,
    * a fairly dull statement of the general status, and a more
    * informative message about the reason for this particular
    * error.
    *
    * @param message The general statement
    * @param reason  The specific reason
    */
    public ResultException(String message, String reason) {
        super(message);
        errorReason = reason;
    }

     /**
      * {@inheritDoc}
      */
    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null) {
            message = errorReason;
        } else {
            if (errorReason != null) {
                message += "-" + errorReason;
            }
        }
        return message;
    }

    /**
    * Get the reason for this error.
    * The reason is an explanation why this service request failed,
    * hopefully giving the user enough information to be able to
    * fix the original request.
    *
    * @return an explanation for the failure of the results.
    */
    public String getReason() {
        return errorReason;
    }

}
