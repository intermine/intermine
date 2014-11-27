package org.intermine.api.query;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 *
 * @author Alex
 *
 */
public class NotPresentException extends QueryStoreException
{

    /**
     *
     * @param message error message
     */
    public NotPresentException(String message) {
        super(message);
    }

}
