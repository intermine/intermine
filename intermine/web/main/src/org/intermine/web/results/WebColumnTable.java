package org.intermine.web.results;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * A List that can understand ResultElement objects.
 * @author Kim Rutherford
 */
public interface WebColumnTable extends List
{
    /**
     * Return a List containing a ResultElement object for each element given in the given row.
     * @param index the row of the results to fetch
     * @return the results row
     */
    public List getResultElements(int index);
    
    /**
     * Returns the Column objects for this table.
     * @return the columns
     */
    public List getColumns();
}
