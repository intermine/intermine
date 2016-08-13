package org.intermine.webservice.server.exceptions;

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
 * Exception representation for denying service based on rate limits.
 *
 * @author Alex Kalderimis
 *
 */
public class RateLimitException extends ServiceForbiddenException
{

    private static final String MSG = "Rate limit (%d per hour) exceeded for IP address %s";
    /**
     * Generated serial version UID, for Serializable
     */
    private static final long serialVersionUID = 388573502722302782L;

    /**
     * Constructor
     * @param remoteAddr The IP-Address we are associating this rate limit with.
     * @param limitPerHour The maximum number of requests that can be made in any 1 hour period.
     */
    public RateLimitException(String remoteAddr, int limitPerHour) {
        super(String.format(MSG, limitPerHour, remoteAddr));
    }
}
