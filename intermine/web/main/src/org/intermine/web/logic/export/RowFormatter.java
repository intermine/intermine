package org.intermine.web.logic.export;

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


/**
 * Interface denoting that object is able format list of objects
 * to string. Implementations format string as comma separated
 * values string and so.
 * @author Jakub Kulaviak
 **/
public interface RowFormatter
{

    /**
     * Format row to string.
     * @param row The objects to format (covariant).
     * @return a string with everything nicely formatted.
     */
    String format(List<? extends Object> row);
}
