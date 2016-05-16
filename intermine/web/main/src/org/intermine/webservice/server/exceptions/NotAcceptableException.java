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

/** @author Alex Kalderimis **/
public class NotAcceptableException extends ServiceException
{

    private static final long serialVersionUID = 6348869247603849879L;

    /** Construct a NotAcceptableException **/
    public NotAcceptableException() {
        super("Cannot serve any format that is acceptable to you", 406);
    }
}
