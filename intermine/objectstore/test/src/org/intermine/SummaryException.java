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

/**
 * An exception that stores a different message for getMessage() to that returned in
 * printStackTrace()
 *
 * @author Matthew Wakeling
 */
public class SummaryException extends Exception
{
    private String summary = "";
    
    public SummaryException() {
        super();
    }

    public SummaryException(String msg) {
        super(msg);
    }
    
    public SummaryException(String msg, String summary) {
        super(msg);
        this.summary = summary;
    }

    public SummaryException(Throwable t) {
        super(t);
    }

    public SummaryException(Throwable t, String summary) {
        super(t);
        this.summary = summary;
    }

    public SummaryException(String msg, Throwable t) {
        super(msg, t);
    }

    public SummaryException(String msg, Throwable t, String summary) {
        super(msg, t);
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
