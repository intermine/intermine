package org.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.AssertionFailedError;

/**
 * An exception that stores a different message for getMessage() to that returned in
 * printStackTrace()
 *
 * @author Matthew Wakeling
 */
public class SummaryAssertionFailedError extends AssertionFailedError
{
    private String summary = "";
    
    public SummaryAssertionFailedError() {
        super();
    }

    public SummaryAssertionFailedError(String msg) {
        super(msg);
    }
    
    public SummaryAssertionFailedError(String msg, String summary) {
        super(msg);
        this.summary = summary;
    }

    public String getMessage() {
        return summary;
    }

    public String toString() {
        String s = getClass().getName();
        String message = super.getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}

