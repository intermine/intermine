package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;


/**
 * Abstract class for formatting result data.
 * @author Jakub Kulaviak
 **/
public abstract class Formatter
{

    /**
     * Returns formatted header.
     * @param attributes attributes contained in header
     * @return formatted header
     */
    public abstract String formatHeader(Map<String, Object> attributes);

    /**
     * Returns formatted result item.
     * @param resultRow result row
     * @return formatted result row
     */
    public abstract String formatResult(List<String> resultRow);

    /**
     * Returns formatted footer.
     * @param errorMessage The error message, if sth went wrong.
     * @param errorCode The error code, if sth went wrong.
     * @return formatted footer
     */
    public abstract String formatFooter(String errorMessage, int errorCode);

}
