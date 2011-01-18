package org.intermine.api.query;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;

/**
 * Common superclass of query executors that holds cache of pathToQueryNode maps per query. This
 * cache is not just for performance, if a query hits the results cache a different pathToQueryNode
 * map needs to be used because QueryNodes are not equals() to one another even if they represent
 * the same class/field.
 *
 * @author Richard Smith
 *
 */
public abstract class QueryExecutor
{
    /**
     * A cache of pathToQueryNode maps that is shared between subclasses of QueryExecutor. The
     * maps are needed to link paths in path queries to objects in the underlying ObjectStore
     * results.
     */
    protected static Map<Query, Map<String, QuerySelectable>> queryToPathToQueryNode =
        Collections.synchronizedMap(new WeakHashMap<Query, Map<String, QuerySelectable>>());

}
