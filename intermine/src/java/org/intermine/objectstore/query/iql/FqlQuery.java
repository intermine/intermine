package org.flymine.objectstore.query.fql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.query.Query;

/**
 * OQL representation of an object-based Query
 *
 * @author Andrew Varley
 */
public class FqlQuery
{
    private String query;
    private String packageName;

    /**
     * Construct an FQL query from a String.
     * NOTE: The query string is not validated on construction
     *
     * @param query the string-based query
     * @param packageName the package name to qualify unqualified classnames with
     * @throws NullPointerException if query is null
     */
    public FqlQuery(String query, String packageName) {
        if (query == null) {
            throw new NullPointerException("query should not be null");
        }
        if ("".equals(query)) {
            throw new IllegalArgumentException("query should not be empty");
        }
        if ("".equals(packageName)) {
            throw new IllegalArgumentException("packageName should not be empty");
        }
        this.query = query;
        this.packageName = packageName;
    }


    /**
     * Convert to a FlyMine query
     *
     * @return the FlyMine Query object
     */
    public Query toQuery() {
        return FqlQueryParser.parse(this);
    }

    /**
     * Get the query String
     * NOTE: this will be unvalidated
     *
     * @return the query String
     */
    public String getQueryString() {
        return query;
    }

    /**
     * Get the package name
     *
     * @return the package name
     */
    public String getPackageName() {
        return packageName;
    }

}
