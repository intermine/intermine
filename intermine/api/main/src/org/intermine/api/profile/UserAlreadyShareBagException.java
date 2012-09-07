package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class UserAlreadyShareBagException extends RuntimeException {

    public UserAlreadyShareBagException() {
    }

    public UserAlreadyShareBagException(String arg0) {
        super(arg0);
    }

    public UserAlreadyShareBagException(Throwable arg0) {
        super(arg0);
    }

    public UserAlreadyShareBagException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
