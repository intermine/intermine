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
 * The NotConnectedException is thrown when some method requiring opened connection was
 * called on closed HttpConnection.
 * @author Jakub Kulaviak
 **/
public class NotConnectedException extends ServiceException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param message message
     */
    public NotConnectedException(String message) {
        super(message);
    }

    /**
     * Default constructor.
     */
    public NotConnectedException() {
        super("Called method requiring opened connection but connect() wasn't called before.");
    }
}
