package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.Model;

/**
 * Helper methods for SaveQueryAction.
 *
 * @author Kim Rutherford
 */
public class SaveQueryHelper
{
    private static final String QUERY_NAME_PREFIX = "query_";

    /**
     * Return a query name that isn't currently in use.
     *
     * @param savedQueries the Map of current saved queries
     * @return the new query name
     */
    public static String findNewQueryName(Map savedQueries) {
        for (int i = 1;; i++) {
            String testName = QUERY_NAME_PREFIX + i;
            if (savedQueries == null || savedQueries.get(testName) == null) {
                return testName;
            }
        }
    }

    /**
     * Clone a query Map
     * @param query the Map
     * @param model the metadata for the query
     * @return a new query
     */
    public static Map clone(Map query, Model model) {
        Map newQuery = new TreeMap();
        for (Iterator i = query.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            newQuery.put(entry.getKey(), clone((RightNode) entry.getValue(), query, model));
        }
        return newQuery;
    }

    /**
     * Clone a RightNode
     * @param node the Node
     * @param query the current query Map
     * @param model the metadata for the query
     * @return a new RightNode
     */
    public static RightNode clone(RightNode node, Map query, Model model) {
        RightNode newNode;
        RightNode parent = (RightNode) query.get(node.getPrefix());
        if (parent == null) {
            newNode = new RightNode(node.getType());
        } else {
            newNode = new RightNode(parent, node.getFieldName(), model);
            newNode.setType(node.getType());
        }
        newNode.setConstraints(clone(node.getConstraints()));
        return newNode;
    }

    /**
     * Clone a List of Constraints
     * @param constraints the Constraints
     * @return a new List of Constraints
     */
    public static List clone(List constraints) {
        List newConstraints = new ArrayList();
        for (Iterator i = constraints.iterator(); i.hasNext();) {
            Constraint constraint = (Constraint) i.next();
            newConstraints.add(new Constraint(constraint.getOp(), constraint.getValue()));
        }
        return newConstraints;
    }
}
