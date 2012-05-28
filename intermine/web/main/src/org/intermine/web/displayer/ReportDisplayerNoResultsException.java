package org.intermine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

@SuppressWarnings("serial")
class ReportDisplayerNoResultsException extends Exception
{

    String mistake;

    public ReportDisplayerNoResultsException() {
        super();
        mistake = "The displayer has no results to show";
    }

    public ReportDisplayerNoResultsException(String err) {
        // a super error...
        super(err);
        mistake = err;
    }

    public String getError() {
        return mistake;
    }

}