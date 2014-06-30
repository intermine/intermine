package org.intermine.web.context;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class ContextNotInitialisedException extends RuntimeException {

    public ContextNotInitialisedException() {
    }

    public ContextNotInitialisedException(String arg0) {
        super(arg0);
    }

    public ContextNotInitialisedException(Throwable arg0) {
        super(arg0);
    }

    public ContextNotInitialisedException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
