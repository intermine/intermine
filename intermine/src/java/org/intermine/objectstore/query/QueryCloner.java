package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;

import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.fql.FqlQueryParser;

/**
 * This is a static class that provides a method to clone a Query object.
 *
 * @author Matthew Wakeling
 */
public class QueryCloner
{
    /**
     * Clones a query object.
     *
     * @param query a Query to clone
     * @return a Query object not connected to the original, but identical
     */
    public static Query cloneQuery(Query query) {
        Query newQuery = new Query();
        /*Map fromElementMap = new HashMap();
        Iterator fromIter = query.getFrom().iterator();
        while (fromIter.hasNext()) {
            FromElement origFrom = (FromElement) fromIter.next();
            FromElement newFrom = null;
            if (origFrom instanceof QueryClass) {
                newFrom = new QueryClass(((QueryClass) origFrom).getType());
            } else if (origFrom instanceof Query) {
                newFrom = cloneQuery((Query) origFrom);
            } else {
                throw new IllegalArgumentException("Unknown type of FromElement " + origFrom);
            }
            newQuery.addFrom(newFrom, (String) query.getAliases().get(origFrom));
            fromElementMap.put(origFrom, newFrom);
        }*/
        //return newQuery;
        return FqlQueryParser.parse(new FqlQuery(query));
    }
}
