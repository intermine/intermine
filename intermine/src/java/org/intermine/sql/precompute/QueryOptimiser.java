package org.flymine.sql.precompute;

import org.flymine.sql.query.Query;
import org.flymine.sql.query.AbstractTable;
import org.flymine.sql.query.AbstractConstraint;
import org.flymine.util.MappingUtil;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map;
import java.util.TreeMap;
import java.sql.Connection;
import java.sql.SQLException;
//import org.flymine.sql.query.

/**
 * A static class providing the code to optimise a query, given a database (presumably with a table
 * describing the available precomputed tables).
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class QueryOptimiser
{
    /**
     * Runs the optimiser through the query represented in the String, given the database. If
     * anything goes wrong, then the original String is returned.
     *
     * @param query the query to optimise
     * @param database the database to use to find precomputed tables
     * @return a String representing the optimised query
     * @throws SQLException if a database error occurs
     */
    public static String optimise(String query, Connection database) throws SQLException {
        try {
            return optimise(new Query(query), database).getSQLString();
        } catch (RuntimeException e) {
            // Query was not acceptable.
        }
        return query;
    }

    /**
     * Runs the optimiser through the query, given the database.
     *
     * @param query the Query to optimise
     * @param database the database to use to find precomputed tables
     * @return the optimised Query
     * @throws SQLException if a database error occurs
     */
    public static Query optimise(Query query, Connection database) throws SQLException {
        BestQueryExplainer bestQuery = new BestQueryExplainer();
        try {
            Set precomputedTables = null; //PrecomputedTableManager.getPrecomputedTables(database);
            recursiveOptimise(precomputedTables, query, bestQuery);
        } catch (BestQueryException e) {
            // Ignore - bestQuery decided to cut short the search
        }
        return bestQuery.getBestQuery();
    }

    /**
     * Recursively optimises the query, given the set of precomputed tables, and updates the
     * BestQuery object with each Query found.
     * When this method returns, either bestQuery will hold the fastest Query, or the bestQuery
     * object will have decided to cut short proceedings. Either way, use the Query in bestQuery
     * for best results.
     *
     * @param precomputedTables a Set of PrecomputedTable objects to use
     * @param query a query to optimise
     * @param bestQuery a BestQuery object to update with each optimised Query object
     * @throws BestQueryException if the BestQuery decides to cut short the search
     * @throws SQLException if a database error occurs
     */
    protected static void recursiveOptimise(Set precomputedTables, Query query,
            BestQuery bestQuery) throws BestQueryException, SQLException {
        // This line creates a Map from PrecomputedTable objects to Sets of optimised Query objects.
        SortedMap map = mergeMultiple(precomputedTables, query);
        // Now we want to iterate through every optimised Query in the Map of Sets.
        Iterator mapIter = map.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) mapIter.next();
            PrecomputedTable p = (PrecomputedTable) mapEntry.getKey();
            Set queries = (Set) mapEntry.getValue();
            // We should prepare a Set of PrecomputedTable objects to reoptimise the Queries in this
            // Set.
            Set newPrecomputedTables = map.headMap(p).keySet();
            // Now we want to iterator through every optimised Query in this Set.
            Iterator queryIter = queries.iterator();
            while (queryIter.hasNext()) {
                Query optimisedQuery = (Query) queryIter.next();
                // Now we want to call recursiveOptimise on each one.
                recursiveOptimise(newPrecomputedTables, optimisedQuery, bestQuery);
                // Also, we need to update BestQuery with this optimised Query.
                bestQuery.add(optimisedQuery);
            }
        }
    }

    /**
     * Iteratively calls merge on query with all the PrecomputedTables in a Set, returning the
     * results in a Map from the PrecomputedTable to the Set that merge returns.
     *
     * @param precomputedTables a Set of PrecomputedTable objects to iterate through
     * @param query the Query to pass in to merge
     * @return a Map from all PrecomputedTable objects that produced a non-empty Set, to the Set
     * that was produced by merge
     */
    protected static SortedMap mergeMultiple(Set precomputedTables, Query query) {
        SortedMap result = new TreeMap();
        Iterator precompIter = precomputedTables.iterator();
        while (precompIter.hasNext()) {
            PrecomputedTable p = (PrecomputedTable) precompIter.next();
            Set mergeResult = merge(p, query);
            if (!mergeResult.isEmpty()) {
                result.put(p, mergeResult);
            }
        }
        return result;
    }

    /**
     * Finds all the possible uses of a given precomputed table in the query, and returns them as
     * a Set of new Queries.
     * If there is no scope for the PrecomputedTable to replace any part of the Query, then this
     * method will return an empty Set.
     * If there are two independent opportunities to insert the PrecomputedTable, then this method
     * will return three Query objects in the Set - one with the first opportunity used, one with
     * the second opportunity used, and one with both.
     * A PrecomputedTable is deemed to "fit", if it <ol>
     * <li>Contains no other tables than those present in the Query</li>
     * <li>Contains no constraints that restrict the output more than the constraints of the Query.
     * Constraints that equal a constraint in the Query can be missed out of the resulting
     * Query</li>
     * <li>Contains all the items in the SELECT list that are present in the Query's SELECT from the
     * tables that are to be replaced</li>
     * </ol>
     * This type of precomputed table can be fitted multiple times.
     * Note that a subquery could be replaced completely by a precomputed table, or can be optimised
     * in-place by another precomputed table (in which case merge should call itself with the
     * subquery).
     * Alternatively, If the PrecomputedTable contains a GROUP BY, then all fields in the Query's
     * SELECT list and all primary keys, that are not to be replaced by the PrecomputedTable must
     * be in the Query's GROUP BY clause, and the fields in the GROUP BY for the tables that are
     * being replaced must match completely. Also, all the fields in the SELECT list of the query
     * must be present in the PrecomputedTable. This type of PrecomputedTable can only be fitted
     * once.
     *
     * @param precomputedTable the PrecomputedTable to use in the new Queries
     * @param query the Query object to try to fit the PrecomputedTable into
     * @return a Set of Query objects, one for each combination of the PrecomputedTable in the
     * query
     */
    protected static Set merge(PrecomputedTable precomputedTable, Query query) {
        Query precompQuery = precomputedTable.getQuery();
        if (!precompQuery.getGroupBy().isEmpty()) {
            return mergeGroupBy(precomputedTable, query);
        }
        return null;
    }

    /**
     * Tries to match a PrecomputedTable with a GROUP BY clause to this query.
     * @see merge for a description of how this works. In fact, we aren't implementing this
     * properly. We are imposing the restriction that there can be no more tables than in the
     * PrecomputedTable, therefore the first restriction mentioned above is followed automatically.
     *
     * @param precomputedTable the PrecomputedTable to use in the new Query
     * @param query the Query object to try to fit the PrecomputedTable into
     * @return a Set containing maybe a new Query object with the PrecomputedTable inserted
     */
    protected static Set mergeGroupBy(PrecomputedTable precomputedTable, Query query) {
        Query precompQuery = precomputedTable.getQuery();
        Set retval = new HashSet();
        if (precompQuery.getGroupBy().size() != query.getGroupBy().size()) {
            // GROUP BY clauses are unequal in size.
            return retval;
        }
        if (precompQuery.getFrom().size() != query.getFrom().size()) {
            // FROM lists are unequal in size.
            return retval;
        }

        // Find the possible mappings from tables in the
        // PrecomputedTable query to tables in the Query
        Set mappings = MappingUtil.findCombinations(precompQuery.getFrom(),
                                                    query.getFrom(),
                                                    new AbstractTableComparator());

        // Iterate through the mappings and compare combinations
        Iterator mappingsIter = mappings.iterator();
        while (mappingsIter.hasNext()) {
            Map mapping = (Map) mappingsIter.next();

            // Remap the aliases
            remapAliases(mapping);

            // For each constraint in the precomputed table, there
            // must be one in the query that is equal, or more
            // restrictive

            if (!compareConstraints(precompQuery.getWhere(), query.getWhere())) {
                continue;
            }
        }


        Iterator precompTableIter = precompQuery.getFrom().iterator();
        while (precompTableIter.hasNext()) {
            AbstractTable precompTable = (AbstractTable) precompTableIter.next();
        }
        return null;
    }


    /**
     * Compares 2 sets of AbstractConstraints
     *
     * @param set1 the first set
     * @param set2 the second set
     * @return true if every element of set1 is equal or less restrictive
     * than some element in set2
     */
    protected static boolean compareConstraints(Set set1, Set set2) {
        Iterator set1Iter = set1.iterator();
        while (set1Iter.hasNext()) {
            AbstractConstraint constraint1 = (AbstractConstraint) set1Iter.next();
            boolean match = false;
            Iterator set2Iter = set2.iterator();
            while (set2Iter.hasNext()) {
                AbstractConstraint constraint2 = (AbstractConstraint) set2Iter.next();
                int compareResult = constraint1.compare(constraint2);
                if (AbstractConstraint.checkComparisonImplies(compareResult)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    /**
     * Alters all the aliases of the tables being mapped to, to equal the alias of the table mapping
     * to them. After this operation (where the AbstractTables of the PrecomputedTable are mapped
     * to the AbstractTables of the Query), the Constraint objects of the Query and the
     * PrecomputedTable can be directly compared with their standard compare methods.
     *
     * @param map the Map from AbstractTable objects with the source alias, to other AbstractTable
     * objects with the destination alias
     */
    protected static void remapAliases(Map map) {
        Iterator mapIter = map.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) mapIter.next();
            AbstractTable firstTable = (AbstractTable) mapEntry.getKey();
            AbstractTable secondTable = (AbstractTable) mapEntry.getValue();
            secondTable.setAlias(firstTable.getAlias());
        }
    }


    /**
     * Compares two AbstractTables using their equalsIgnoreAlias() method
     */
    protected static class AbstractTableComparator implements Comparator
    {
        /**
         * Constructor
         */
        public AbstractTableComparator() {
        }
        /**
         * Compare two AbstractTables using equalsIgnoreAlias()
         *
         * @param a the first AbstractTable
         * @param b the second AbstractTable
         * @return zero if the two AbstractTables are equal
         */
        public int compare(Object a, Object b) {
            return (((AbstractTable) a).equalsIgnoreAlias((AbstractTable) b) ? 0 : -1);
        }
    }
}
