package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.webservice.server.exceptions.ServiceException;

public class JSONCountFormatter extends JSONFormatter {

    @Override
    public String formatResult(List<String> resultRow) {
        if (resultRow.size() != 1) {
            throw new ServiceException("Something is wrong - I got this"
                    + "result row: " + resultRow.toString()
                    + ", but I was expecting a count");
        } else {
            declarePrinted();
            return "\"count\":" + resultRow.get(0);
        }
    }

}
