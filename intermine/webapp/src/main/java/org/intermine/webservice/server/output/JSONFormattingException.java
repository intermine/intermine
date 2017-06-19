package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/** @author Alex Kalderimis **/
public final class JSONFormattingException extends RuntimeException
{

    private static final long serialVersionUID = 1L;

     /**
     * @param message message
     */
    public JSONFormattingException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public JSONFormattingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause cause
     */
    public JSONFormattingException(Throwable cause) {
        super(cause);
    }

}
