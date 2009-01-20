package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Helper methods for PathQuery.
 * @author Richard Smith
 *
 */
public class PathQueryHelper
{

    
    /**
     * If the sort order of the query is empty iterate through the view and add the first view
     * path that is valid for a sort order.  If the sort order isn't empty or there are no valid
     * view elements, does nothing.
     * @param query the query to set default view of, may be altered by this method
     */
    public static void setDefaultSortOrder(PathQuery query) {
        // if the sort order isn't empty, do nothing
        if (!query.getSortOrder().isEmpty()) {
            return;
        }
        
        // add the first valid view element to the sort order list
        for (Path path : query.getView()) {
            if (query.getSortOrder().isEmpty() && query.isValidOrderPath(path.toString())) {
                query.addOrderBy(path.toString());
                return;
            }
        }
    }
}
