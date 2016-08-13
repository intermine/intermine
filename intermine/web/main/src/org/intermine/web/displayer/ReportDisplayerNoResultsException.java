package org.intermine.web.displayer;

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
 * An exception for conveying the message that there are no results.
 * @author Radek (?)
 *
 */
@SuppressWarnings("serial")
class ReportDisplayerNoResultsException extends Exception
{

    private final String mistake;

    /**
     * Construct an exception with the default mistake.
     */
    public ReportDisplayerNoResultsException() {
        super();
        mistake = "The displayer has no results to show";
    }

    /**
     * Construct an exception with a custom mistake message.
     * @param err The mistake.
     */
    public ReportDisplayerNoResultsException(String err) {
        super(err);
        mistake = err;
    }

    /** @return the error **/
    public String getError() {
        return mistake;
    }

}
