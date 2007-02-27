package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.intermine.sql.Database;
import org.intermine.sql.query.Query;
import org.intermine.sql.query.AbstractTable;
import org.intermine.sql.query.AbstractConstraint;
import org.intermine.sql.query.AbstractValue;
import org.intermine.sql.query.Constant;
import org.intermine.sql.query.Constraint;
import org.intermine.sql.query.ConstraintSet;
import org.intermine.sql.query.Field;
import org.intermine.sql.query.Function;
import org.intermine.sql.query.InListConstraint;
import org.intermine.sql.query.NotConstraint;
import org.intermine.sql.query.SelectValue;
import org.intermine.sql.query.SubQueryConstraint;
import org.intermine.sql.query.Table;
import org.intermine.util.ConsistentSet;
import org.intermine.util.IdentityMap;
import org.intermine.util.MappingUtil;
import org.intermine.util.StringUtil;

/**
 * A static class providing the code to optimise a query, given a database (presumably with a table
 * describing the available precomputed tables).
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class QueryOptimiser
{
    private static final Logger LOG = Logger.getLogger(QueryOptimiser.class);
    private static final int REPORT_INTERVAL = 10000;

    private static final String ALIAS_PREFIX = "P";
    private static int callCount = 0;

    /**
     * Runs the optimiser through the query represented in the String, given the database. If
     * anything goes wrong, then the original String is returned.
     *
     * @param query the query to optimise
     * @param database the database to use to find precomputed tables
     * @return a String representing the optimised query
     * @throws SQLException if a database error occurs
     */
    public static String optimise(String query, Database database) throws SQLException {
        return optimise(query, database, QueryOptimiserContext.DEFAULT);
    }

    /**
     * Runs the optimiser through the query represented in the String, given the database. If
     * anything goes wrong, then the original String is returned.
     *
     * @param query the query to optimise
     * @param database the database to use to find precomputed tables
     * @param context a QueryOptimiserContext, to alter settings
     * @return a String representing the optimised query
     * @throws SQLException if a database error occurs
     */
    public static String optimise(String query, Database database, QueryOptimiserContext context)
        throws SQLException {
        return optimise(query, null, database, null, context).getBestQueryString();
    }

    /**
     * Runs the optimiser through the query, given the database.
     *
     * @param query the Query to optimise
     * @param database the database to use to find precomputed tables
     * @return the optimised Query
     * @throws SQLException if a database error occurs
     */
    protected static Query optimise(Query query, Database database) throws SQLException {
        return optimise(query, database, QueryOptimiserContext.DEFAULT);
    }

    /**
     * Runs the optimiser through the query, given the database.
     *
     * @param query the Query to optimise
     * @param database the database to use to find precomputed tables
     * @param context a QueryOptimiserContext, to alter settings
     * @return the optimised Query
     * @throws SQLException if a database error occurs
     */
    protected static Query optimise(Query query, Database database, QueryOptimiserContext context)
        throws SQLException {
        return optimise(query.toString(), query, database, null, context).getBestQuery();
    }

    /**
     * Runs the optimiser through the query represented in the String and Query, given the
     * Connection and an object to lookup a PrecomputedTable
     *
     * @param query the query String to optimise
     * @param originalQuery the Query object to optimise - or optionally null
     * @param precompLookup a Database or Connection to lookup a PrecomputedTableManager
     * @param explainConnection the database connection to use, or null if precompLookup is a
     * Database
     * @param context a QueryOptimiserContext, to alter settings
     * @return a BestQuery object
     * @throws SQLException if a database error occurs
     */
    public static BestQuery optimise(String query, Query originalQuery, Object precompLookup,
            Connection explainConnection, QueryOptimiserContext context) throws SQLException {
        Database database = null;
        PrecomputedTableManager ptm = null;
        if (precompLookup instanceof Database) {
            database = (Database) precompLookup;
            ptm = PrecomputedTableManager.getInstance(database);
        } else if (precompLookup instanceof Connection) {
            ptm = PrecomputedTableManager.getInstance((Connection) precompLookup);
        } else if (precompLookup instanceof PrecomputedTableManager) {
            ptm = (PrecomputedTableManager) precompLookup;
        } else {
            throw new SQLException("Cannot get a PrecomputedTableManager for lookup object "
                    + precompLookup);
        }
        if (ptm.getPrecomputedTables().isEmpty()) {
            if (context.isVerbose()) {
                System.out .println("QueryOptimiser: no Precomputed Tables");
            }
            return new BestQueryFallback(null, query);
        }
        Set precomputedTables = ptm.getPrecomputedTables();
        OptimiserCache cache = OptimiserCache.getInstance(database);
        return optimiseWith(query, originalQuery, database, explainConnection, context, 
                            precomputedTables, cache);
    }

    /**
     * Runs the optimiser through the query repesented in the String and Query, given the Connection
     * and a set of PrecomputedTables.
     *
     * @param query the query String to optimise
     * @param originalQuery the Query object to optimise - or optionally null
     * @param database a Database
     * @param explainConnection the database connection to use, or null if database is a Database
     * @param context a QueryOptimiserContext, to alter settings
     * @param precomputedTables a Set of PrecomputedTables
     * @param cache an OptimiserCache
     * @return a BestQuery object
     * @throws SQLException if a database error occurs
     */
    public static BestQuery optimiseWith(String query, Query originalQuery, Database database,
            Connection explainConnection, QueryOptimiserContext context, Set precomputedTables,
            OptimiserCache cache) throws SQLException {
        callCount++;
        if (callCount % REPORT_INTERVAL == 0) {
            LOG.info("Optimiser called " + callCount + " times");
        }
        long start = new Date().getTime();
        long parseTime = 0;
        // If we want to do any query caching, here is where we should do it.
        LimitOffsetQuery limitOffsetQuery = new LimitOffsetQuery(query);
        LOG.debug("Original Query: " + limitOffsetQuery.getQuery() + ", "
                + limitOffsetQuery.getLimit() + ", " + limitOffsetQuery.getOffset());
        String cachedQuery = cache.lookup(limitOffsetQuery.getQuery(), limitOffsetQuery.getLimit(),
                limitOffsetQuery.getOffset());
        // TODO: fix so that the OptimiserCache is updated when precomputed tables are deleted
        if (cachedQuery != null) {
            LOG.debug("Optimising query took " + ((new Date()).getTime() - start)
                    + " ms - cache hit: " + query);
            if (context.isVerbose()) {
                System.out .println("QueryOptimiser: cache hit");
            }
            return new BestQueryFallback(null, limitOffsetQuery.reconstruct(cachedQuery));
        }
        try {
            boolean openedConnection = false;
            if (explainConnection == null) {
                openedConnection = true;
                explainConnection = database.getConnection();
            }
            BestQuery bestQuery;
            if (context.getMode() == QueryOptimiserContext.MODE_VERBOSE) {
                bestQuery = new BestQueryExplainerVerbose(explainConnection,
                        context.getTimeLimit());
            } else if (context.getMode() == QueryOptimiserContext.MODE_VERBOSE_LIST) {
                bestQuery = new BestQueryLogger(true);
            } else if (context.getMode() == QueryOptimiserContext.MODE_VERBOSE_SUMMARY) {
                bestQuery = new BestQueryLogger(false);
            } else {
                bestQuery = new BestQueryExplainer(explainConnection, context.getTimeLimit());
            }
            String optimisedQuery = null;
            int expectedRows = 0;
            int expectedTime = 0;
            try {
                // First, add the original string to the BestQuery object, so it has an opportunity
                // to say optimisation is not worth it, before parsing.
                bestQuery.add(query);
                if (originalQuery == null) {
                    originalQuery = new Query(query);
                }
                parseTime = new Date().getTime();
                remapAliasesToAvoidPrecomputePrefix(originalQuery);
                recursiveOptimise(precomputedTables, originalQuery, bestQuery, originalQuery);
            } catch (BestQueryException e) {
                // Ignore - bestQuery decided to cut short the search
                //if (bestQuery instanceof BestQueryExplainer) {
                //    expectedTime = (int) ((BestQueryExplainer) bestQuery).getBestExplainResult()
                //        .getComplete();
                //}
                if (context.isVerbose()) {
                    System.out .println("QueryOptimiser: bailing out early: " + e);
                }
            } finally {
                if (context.isVerbose()) {
                    System.out .println("Optimised SQL: " + bestQuery.getBestQueryString());
                } else {
                    bestQuery.getBestQueryString();
                }
                if (openedConnection) {
                    explainConnection.close();
                }
            }
            optimisedQuery = bestQuery.getBestQueryString();
            //expectedRows = (int) bestQuery.getBestExplainResult().getEstimatedRows();
            // HACKHACKHACK this following line turns the cache into a really dumb one:
            expectedRows = Integer.MAX_VALUE;
            // Add optimised query to the cache here.
            LimitOffsetQuery limitOffsetOptimisedQuery = new LimitOffsetQuery(optimisedQuery);
            LOG.debug("New cache line produced - expectedRows = " + expectedRows + ", limit = "
                    + limitOffsetQuery.getLimit());
            cache.addCacheLine(limitOffsetQuery.getQuery(), limitOffsetOptimisedQuery.getQuery(),
                    limitOffsetQuery.getLimit(), limitOffsetQuery.getOffset(), expectedRows);
            LOG.debug("Optimising " + (expectedTime == 0 ? "" : expectedTime + " ms ")
                    + "query took " + ((new Date()).getTime() - start)
                    + (parseTime == 0 ? " ms without parsing " : " ms including "
                        + (parseTime - start) + " ms for parse ") + "- cache miss: " + query);
            return bestQuery;
        } catch (RuntimeException e) {
            LOG.warn("Exception - query cannot be optimised: " + query, e);
        }
        LOG.debug("Optimising query took " + ((new Date()).getTime() - start)
                + " ms - unparsable query: " + query);
        if (context.isVerbose()) {
            System.out .println("QueryOptimiser: unparsable query");
        }
        return new BestQueryFallback(originalQuery, query);
    }

    /**
     * Remaps the aliases of any table that starts with the ALIAS_PREFIX, to avoid clashes with
     * future precomputed tables.
     *
     * @param query the query to remap
     */
    protected static void remapAliasesToAvoidPrecomputePrefix(Query query) {
        Iterator tableIter = query.getFrom().iterator();
        while (tableIter.hasNext()) {
            AbstractTable table = (AbstractTable) tableIter.next();
            if (table.getAlias().startsWith(ALIAS_PREFIX)) {
                table.setAlias(ALIAS_PREFIX + StringUtil.uniqueString());
            }
        }
    }

    /**
     * Recursively optimises the query, given the set of precomputed tables, and updates the
     * BestQuery object with each Query found.
     * When this method returns, either bestQuery will hold the fastest Query, or the bestQuery
     * object will have decided to cut short proceedings. Either way, use the Query in bestQuery
     * for best results.
     * NOTE: the Query passed into this method at the top level should never be used again by
     * anything (except when this method is called by this method), because this method alters the
     * query. Therefore it is recommended that all Queries are passed into this method as fresh
     * clones of the Query you wish to optimise.
     *
     * @param precomputedTables a Set of PrecomputedTable objects to use
     * @param query a query to optimise
     * @param bestQuery a BestQuery object to update with each optimised Query object
     * @param originalQuery the original Query, as passed to the first instance of
     * recursiveOptimise.
     * @throws BestQueryException if the BestQuery decides to cut short the search
     * @throws SQLException if a database error occurs
     */
    public static void recursiveOptimise(Set precomputedTables, Query query,
            BestQuery bestQuery, Query originalQuery) throws BestQueryException, SQLException {
        // This line creates a Map from PrecomputedTable objects to Sets of optimised Query objects.
        SortedMap map = mergeMultiple(precomputedTables, query, originalQuery);
        // Now we want to iterate through every optimised Query in the Map of Sets.
        Iterator mapIter = map.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) mapIter.next();
            PrecomputedTable p = (PrecomputedTable) mapEntry.getKey();
            Set queries = (Set) mapEntry.getValue();
            // We should prepare a Set of PrecomputedTable objects to reoptimise the Queries in this
            // Set.
            Set newPrecomputedTables = map.headMap(p).keySet();
            // Now we want to iterate through every optimised Query in this Set.
            Iterator queryIter = queries.iterator();
            while (queryIter.hasNext()) {
                Query optimisedQuery = (Query) queryIter.next();
                // First, update BestQuery with this Query.
                bestQuery.add(optimisedQuery);
                // Now we want to call recursiveOptimise on each one.
                recursiveOptimise(newPrecomputedTables, optimisedQuery, bestQuery, originalQuery);
            }
        }
    }

    /**
     * Iteratively calls merge on query with all the PrecomputedTables in a Set, returning the
     * results in a Map from the PrecomputedTable to the Set that merge returns.
     *
     * @param precomputedTables a Set of PrecomputedTable objects to iterate through
     * @param query the Query to pass in to merge
     * @param originalQuery the original Query, as passed to the first instance of
     * recursiveOptimise.
     * @return a Map from all PrecomputedTable objects that produced a non-empty Set, to the Set
     * that was produced by merge
     */
    protected static SortedMap mergeMultiple(Set precomputedTables, Query query,
            Query originalQuery) {
        SortedMap result = new TreeMap();
        // Do precomputed tables is decreasing order of number of constituent tables
        Object sorted[] = (Object[]) precomputedTables.toArray();
        Arrays.sort(sorted, new Comparator() {
            public int compare(Object a, Object b) {
                return ((PrecomputedTable) b).getQuery().getFrom().size()
            - ((PrecomputedTable) a).getQuery().getFrom().size();
            }
        });
        for (int i = 0; i < sorted.length; i++) {
            PrecomputedTable p = (PrecomputedTable) sorted[i];
            Set mergeResult = merge(p, query, originalQuery);
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
     * <li>A similar restriction on the HAVING clauses as the WHERE clauses.</li>
     * <li>Contains all the items in the SELECT list that are present in the Query's SELECT from the
     * tables that are to be replaced</li>
     * <li>If the PrecomputedTable is DISTINCT, then the Query must also be DISTINCT.</li>
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
     * @param originalQuery the original Query, as passed to the first instance of
     * recursiveOptimise.
     * @return a Set of Query objects, one for each combination of the PrecomputedTable in the
     * query
     */
    protected static Set merge(PrecomputedTable precomputedTable, Query query,
            Query originalQuery) {
        Query precompQuery = precomputedTable.getQuery();
        if (!precompQuery.getGroupBy().isEmpty()) {
            return mergeGroupBy(precomputedTable, query, originalQuery);
        }
        Set retval = new HashSet();

        // Find the possible mappings from tables in the
        // PrecomputedTable query to tables in the Query
        Set mappings = new ConsistentSet();
        Collection c = MappingUtil.findCombinations(precompQuery.getFrom(),
                query.getFrom(), new AbstractTableComparator(),
                new OptimiserMappingChecker(precompQuery.getWhere(), query.getWhere()));
        mappings.addAll(c);

        // Create a map from AbstractValue to SelectValue for the PrecomputedTable
        Map valueMap = precomputedTable.getValueMap();

        // These are Maps where we can store away those structures we have built while checking the
        // precomputed table mapping.
        //Map lookupWhereConstraintEqualsSet = new HashMap();

        // Iterate through the mappings and compare combinations. Note that each mapping is a case
        // where the precomputed table has the same set of tables as the Query.
        Iterator mappingsIter = mappings.iterator();
        while (mappingsIter.hasNext()) {
            Map mapping = (Map) mappingsIter.next();

            // Remap the aliases
            // TODO: query.getFrom() is really the wrong thing to use. To be extra-specially
            // paranoid about realiasing things so they clash, this should be the getFrom() of the
            // original Query, before any precomputed tables have been inserted.
            remapAliases(mapping, originalQuery.getFrom());

            // Compare the WHERE constraints.
            Set whereConstraintEqualsSet = new HashSet();
            // NOTE: this if line has a side-effect... Careful.
            if (!compareConstraints(precompQuery.getWhere(), query.getWhere(),
                        whereConstraintEqualsSet)) {
                mappingsIter.remove();
                continue;
            }
            // So now whereConstraintEqualsSet contains all the constraints that can be left out of
            // the WHERE clause

            // If the PrecomputedTable is distinct, then the Query has to be.
            if (precompQuery.isDistinct() && (!query.isDistinct())) {
                mappingsIter.remove();
                continue;
            }
        }

        // Now, we can pass the trimmed mappings Set into MappingUtil.findMultipleCombinations. For
        // each multiple combination, produce a new Query.
        Set multipleMappings = MappingUtil.findMultipleCombinations(mappings);

        Iterator multipleMappingsIter = multipleMappings.iterator();
        while (multipleMappingsIter.hasNext()) {
            Set multipleMapping = (Set) multipleMappingsIter.next();
            Query currentQuery = query;
            try {
                mappingsIter = multipleMapping.iterator();
                while (mappingsIter.hasNext()) {
                    Map mapping = (Map) mappingsIter.next();

                    // Remap the aliases
                    // TODO: currentQuery.getFrom() is really the wrong thing to use. To be
                    // extra-specially paranoid about realiasing things so they clash, this should
                    // be the getFrom() of the original Query, before any precomputed tables have
                    // been inserted.
                    //
                    // Okay, trying the original query table list instead. Watch this space...
                    remapAliases(mapping, originalQuery.getFrom());

                    // Compare the WHERE constraints.
                    Set whereConstraintEqualsSet = new HashSet();
                    // NOTE: this if line has a side-effect... Careful.
                    compareConstraints(precompQuery.getWhere(), currentQuery.getWhere(),
                                whereConstraintEqualsSet);
                    // So now whereConstraintEqualsSet contains all the constraints that can be
                    // left out of the WHERE clause

                    Table precomputedSqlTable = new Table(precomputedTable.getName(),
                            ALIAS_PREFIX + StringUtil.uniqueString());
                    Query newQuery = new Query();

                    // Populate the SELECT list of the new Query. This method will throw an
                    // exception if any of the AbstractValues required are not present in the
                    // precomputed table.
                    reconstructSelectValues(currentQuery.getSelect(), precomputedSqlTable, valueMap,
                            precompQuery.getFrom(), false, newQuery);

                    // Populate the FROM list - first the precomputed table
                    newQuery.addFrom(precomputedSqlTable);
                    // And also the non-covered tables.
                    addNonCoveredFrom(currentQuery.getFrom(), precompQuery.getFrom(),
                            newQuery.getFrom());

                    // Populate the HAVING clause of newQuery with the contents of the HAVING clause
                    // of currentQuery.
                    reconstructAbstractConstraints(currentQuery.getHaving(), precomputedSqlTable,
                            valueMap, precompQuery.getFrom(), false, newQuery.getHaving(),
                            Collections.EMPTY_SET, null, 0, null, false);

                    // Now populate the ORDER BY clause of newQuery from the contents of the ORDER
                    // BY clause of currentQuery.
                    Field orderByField = null;
                    List precompOrderBy = precompQuery.getOrderBy();
                    if ((precomputedTable.getOrderByField() == null) || (!query.getGroupBy()
                                .isEmpty())) {
                        reconstructAbstractValues(currentQuery.getOrderBy(), precomputedSqlTable,
                                valueMap, precompQuery.getFrom(), false, newQuery.getOrderBy());
                    } else {
                        List tempOrderBy = new ArrayList();
                        reconstructAbstractValues(currentQuery.getOrderBy(), precomputedSqlTable,
                                valueMap, precompQuery.getFrom(), false, tempOrderBy);
                        List newOrderBy = newQuery.getOrderBy();

                        // Now, we have a chance to improve the performance of the query by
                        // substituting the orderby_field instead of certain elements of the order
                        // by list. The only useful way to do this is if the order by clause of
                        // the precomputed table exactly matches the start of the order by clause
                        // of the original query.

                        Iterator orderByIter = tempOrderBy.iterator();
                        if (precompOrderBy.size() <= tempOrderBy.size()) {
                            boolean matches = true;
                            for (int i = 0; (i < precompOrderBy.size()) && matches; i++) {
                                Field nextPrecompOrderBy = new Field(((SelectValue) valueMap
                                            .get(precompOrderBy.get(i))).getAlias(),
                                        precomputedSqlTable);
                                AbstractValue origValue = (AbstractValue) orderByIter.next();
                                matches = origValue.equals(nextPrecompOrderBy);
                            }

                            if (matches) {
                                orderByField = new Field(precomputedTable.getOrderByField(),
                                        precomputedSqlTable);
                                newOrderBy.add(orderByField);
                            } else {
                                orderByIter = tempOrderBy.iterator();
                            }
                        }
                        while (orderByIter.hasNext()) {
                            newOrderBy.add(orderByIter.next());
                        }
                        if ((orderByField != null) && currentQuery.isDistinct()) {
                            newQuery.addSelect(new SelectValue(orderByField,
                                                               "orderby_field_from_pt"));
                        }
                    }

                    // Populate the WHERE clause of newQuery with the contents of the WHERE clause
                    // of currentQuery, leaving out those constraints in whereConstraintEqualsSet.
                    if ((orderByField == null) || (!query.getGroupBy().isEmpty())) {
                        reconstructAbstractConstraints(currentQuery.getWhere(), precomputedSqlTable,
                                valueMap, precompQuery.getFrom(), false, newQuery.getWhere(),
                                whereConstraintEqualsSet, null, 0, null, false);
                    } else {
                        Field firstPrecompOrderBy = new Field(((SelectValue) valueMap.get(
                                        precompOrderBy.get(0))).getAlias(), precomputedSqlTable);
                        reconstructAbstractConstraints(currentQuery.getWhere(), precomputedSqlTable,
                                valueMap, precompQuery.getFrom(), false, newQuery.getWhere(),
                                whereConstraintEqualsSet, firstPrecompOrderBy,
                                precompOrderBy.size(), orderByField,
                                precomputedTable.getFirstOrderByHasNoNulls());
                    }

                    // Now populate the GROUP BY clause of newQuery from the contents of the GROUP
                    // BY clause of currentQuery.
                    reconstructAbstractValues(currentQuery.getGroupBy(), precomputedSqlTable,
                            valueMap, precompQuery.getFrom(), false, newQuery.getGroupBy());

                    // Now copy the EXPLAIN, DISTINCT, LIMIT, and OFFSET status to newQuery:
                    newQuery.setDistinct(currentQuery.isDistinct());
                    newQuery.setExplain(currentQuery.isExplain());
                    newQuery.setLimitOffset(currentQuery.getLimit(), currentQuery.getOffset());

                    // Finally, set currentQuery to newQuery.
                    currentQuery = newQuery;
                }
                retval.add(currentQuery);
            } catch (QueryOptimiserException e) {
                continue;
            }

        }

        return retval;
    }

    /**
     * Tries to match a PrecomputedTable with a GROUP BY clause to this query.
     * See merge for a description of how this works. In fact, we aren't implementing this
     * properly. We are imposing the restriction that there can be no more tables than in the
     * PrecomputedTable, therefore the first restriction mentioned above is followed automatically.
     * A PrecomputedTable is deemed to fit if it <ol>
     * <li>Contains exactly the same set of tables as the Query</li>
     * <li>Contains exactly the same WHERE clause as the Query</li>
     * <li>Contains all the items in the SELECT list that are present in the Query's SELECT
     * list.</li>
     * <li>Contains exactly the same GROUP BY clause</li>
     * </ol>
     * So, this leaves very little leeway for optimising different Queries - these are the things
     * that can be different from the PrecomputedTable: <ol>
     * <li>The HAVING clause may be different, although each constraint in the PrecomputedTable
     * must EQUAL or IMPLIES some constraint in the Query.</li>
     * <li>the DISTINCT status can be different, but if the PrecomputedTable is DISTINCT, then
     * the Query must also be DISTINCT.</li>
     * </ol>
     *
     * @param precomputedTable the PrecomputedTable to use in the new Query
     * @param query the Query object to try to fit the PrecomputedTable into
     * @param originalQuery the original query object
     * @return a Set containing maybe a new Query object with the PrecomputedTable inserted
     */
    protected static Set mergeGroupBy(PrecomputedTable precomputedTable, Query query,
            Query originalQuery) {
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

        // Create a map from AbstractValue to SelectValue for the PrecomputedTable
        Map valueMap = precomputedTable.getValueMap();

        // Iterate through the mappings and compare combinations. Note that each mapping is a case
        // where the precomputed table has the same set of tables as the Query.
        Iterator mappingsIter = mappings.iterator();
        while (mappingsIter.hasNext()) {
            Map mapping = (Map) mappingsIter.next();

            // Remap the aliases
            // TODO: query.getFrom() is really the wrong thing to use. To be extra-specially
            // paranoid about realiasing things so they clash, this should be the getFrom() of the
            // original Query, before any precomputed tables have been inserted.
            remapAliases(mapping, originalQuery.getFrom());

            // The constraints must all be exactly the same as those in the Query.
            // TODO: This should probably be replaced by
            // compareConstraints(precompWhere, queryWhere)
            //      && compareConstraints(queryWhere, precompWhere)
            // This will catch the case where the precompWhere and the QueryWhere are actually
            // equal, but expressed in different terms (such as a = 5, compared to
            // a <= 5 and a >= 5, although I don't think the algorithm is quite clever enough to
            // spot this particular example).
            if (!precompQuery.getWhere().equals(query.getWhere())) {
                continue;
            }

            // Constraints are equal, now check the group by. We can use .equals on the Sets,
            // because AbstractValue.equals relies on AbstractTable.equals, which should be happy
            // now that the aliases have been matched up.
            if (!precompQuery.getGroupBy().equals(query.getGroupBy())) {
                continue;
            }

            // Also, we must compare the HAVING clauses - each constraint in the PrecomputedTable
            // must EQUAL or IMPLIES some constraint in the Query.
            Set constraintEqualsSet = new HashSet();
            // NOTE: this if line has a side-effect... Careful.
            if (!compareConstraints(precompQuery.getHaving(), query.getHaving(),
                        constraintEqualsSet)) {
                continue;
            }
            // So now constraintEqualsSet contains all the constraints that can be left out

            // If the PrecomputedTable is distinct, then the Query has to be.
            if (precompQuery.isDistinct() && (!query.isDistinct())) {
                continue;
            }

            Table precomputedSqlTable = new Table(precomputedTable.getName(),
                    ALIAS_PREFIX + StringUtil.uniqueString());
            Query newQuery = new Query();

            try {
                // Populate the SELECT list of the new Query. This method will throw an exception
                // if any of the AbstractValues required are not present in the precomputed table.
                reconstructSelectValues(query.getSelect(), precomputedSqlTable, valueMap,
                        precompQuery.getFrom(), true, newQuery);

                // Populate the FROM list - in this case it is only the precomputed table
                newQuery.addFrom(precomputedSqlTable);

                // Populate the WHERE clause of the new query with the contents of the HAVING clause
                // of the original query, leaving out those constraints in constraintEqualsSet.
                reconstructAbstractConstraints(query.getHaving(), precomputedSqlTable, valueMap,
                        precompQuery.getFrom(), true, newQuery.getWhere(), constraintEqualsSet,
                        null, 0, null, true);

                // Now populate the ORDER BY clause of the new query from the contents of the ORDER
                // BY clause of the original query.
                reconstructAbstractValues(query.getOrderBy(), precomputedSqlTable, valueMap,
                        precompQuery.getFrom(), true, newQuery.getOrderBy());


                // Now copy the EXPLAIN, DISTINCT, LIMIT, and OFFSET status to the new query:
                newQuery.setDistinct(query.isDistinct());
                newQuery.setExplain(query.isExplain());
                newQuery.setLimitOffset(query.getLimit(), query.getOffset());

            } catch (QueryOptimiserException e) {
                continue;
            }
            retval.add(newQuery);

        }
        return retval;
    }


    /**
     * Compares 2 sets of AbstractConstraints
     *
     * @param set1 the first set
     * @param set2 the second set
     * @param equalsSet a Set that should be passed in empty - it will be populated with those
     * AbstractConstraints that have an equal in the other set. Note that if the return value is
     * false, then the contents of this Set is undefined
     * @return true if every element of set1 is equal or less restrictive
     * than some element in set2
     */
    protected static boolean compareConstraints(Set set1, Set set2, Set equalsSet) {
        return compareConstraints(set1, set2, equalsSet, IdentityMap.INSTANCE,
                IdentityMap.INSTANCE);
    }

    /**
     * Compares 2 sets of AbstractConstraints
     *
     * @param set1 the first set
     * @param set2 the second set
     * @param equalsSet a Set that should be passed in empty - it will be populated with those
     * AbstractConstraints that have an equal in the other set. Note that if the return value is
     * false, then the contents of this Set is undefined
     * @param tableMap a Map from Table to Table
     * @param reverseTableMap the reverse of tableMap
     * @return true if every element of set1 is equal or less restrictive
     * than some element in set2
     */
    protected static boolean compareConstraints(Set set1, Set set2, Set equalsSet, Map tableMap,
            Map reverseTableMap) {
        Iterator set1Iter = set1.iterator();
        while (set1Iter.hasNext()) {
            AbstractConstraint constraint1 = (AbstractConstraint) set1Iter.next();
            boolean match = false;
            Iterator set2Iter = set2.iterator();
            while (set2Iter.hasNext()) {
                AbstractConstraint constraint2 = (AbstractConstraint) set2Iter.next();
                int compareResult = constraint2.compare(constraint1, reverseTableMap, tableMap);
                if (AbstractConstraint.checkComparisonImplies(compareResult)) {
                    match = true;
                    if (AbstractConstraint.checkComparisonEquals(compareResult)) {
                        equalsSet.add(constraint2);
                        break;
                    }
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two Lists of SelectValues, and returns true if all the items in the first List are
     * present in the second List, ignoring the SelectValue alias.
     *
     * @param list1 the list of items that must be present in list2
     * @param list2 the list of items to look in
     * @return true if every item in list1 is present in list2, ignoring SelectValue aliases
     * TODO: take this function out - we don't need it.
     */
    protected static boolean compareSelectLists(List list1, List list2) {
        Set allValues = new HashSet();
        Iterator list2Iter = list2.iterator();
        while (list2Iter.hasNext()) {
            SelectValue selectValue = (SelectValue) list2Iter.next();
            allValues.add(selectValue.getValue());
        }
        Iterator list1Iter = list1.iterator();
        while (list1Iter.hasNext()) {
            SelectValue selectValue = (SelectValue) list1Iter.next();
            if (!allValues.contains(selectValue.getValue())) {
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
     * @param tables a Set of tables which the values of the map happen to be in. This permits this
     * method to check that none of the tables that it is about to change the alias of will clash
     * with any pre-existing table aliases. Pre-existing table aliases will be renamed as necessary.
     */
    protected static void remapAliases(Map map, Set tables) {
        Iterator mapIter = map.entrySet().iterator();
        while (mapIter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) mapIter.next();
            AbstractTable firstTable = (AbstractTable) mapEntry.getKey();
            AbstractTable secondTable = (AbstractTable) mapEntry.getValue();
            String firstAlias = firstTable.getAlias();
            // First, find if there's a table already with that alias.
            AbstractTable matchingTable = findTableForAlias(firstAlias, tables);
            if ((matchingTable != null) && (!matchingTable.equals(secondTable))) {
                // There is a table already with that alias. We must find another alias for it.
                boolean used = true;
                String alternativeName = null;
                do {
                    alternativeName = ALIAS_PREFIX + StringUtil.uniqueString();
                } while (findTableForAlias(alternativeName, tables) != null);
                matchingTable.setAlias(alternativeName);
            }
            secondTable.setAlias(firstTable.getAlias());
        }
    }

    /**
     * Finds a table in a Set with the given alias.
     *
     * @param alias the alias to look for
     * @param set the Set to look in
     * @return an AbstractTable that has the alias or null if there isn't one
     */
    protected static AbstractTable findTableForAlias(String alias, Set set) {
        AbstractTable matchingTable = null;
        Iterator tableIter = set.iterator();
        while (tableIter.hasNext() && (matchingTable == null)) {
            matchingTable = (AbstractTable) tableIter.next();
            if (!matchingTable.getAlias().equals(alias)) {
                matchingTable = null;
            }
        }
        return matchingTable;
    }

    /**
     * Compares two AbstractTables using their equalsIgnoreAlias() method.
     */
    protected static class AbstractTableComparator implements Comparator
    {
        /**
         * Constructor.
         */
        public AbstractTableComparator() {
        }
        /**
         * Compare two AbstractTables using equalsIgnoreAlias().
         *
         * @param a the first AbstractTable
         * @param b the second AbstractTable
         * @return zero if the two AbstractTables are equal
         */
        public int compare(Object a, Object b) {
            return (((AbstractTable) a).equalsIgnoreAlias((AbstractTable) b) ? 0 : -1);
        }
    }

    /**
     * Reconstructs an AbstractValue, to form the part of an SQL Query that has been replaced with
     * a PrecomputedTable. If the AbstractValue is present in the SELECT list of the
     * PrecomputedTable, then a new Field replaces the AbstractValue, that is that particular
     * field in the PrecomputedTable. Otherwise, the original AbstractValue will be returned.
     *
     * @param original the original AbstractValue
     * @param precomputedSqlTable the Table object representing the PrecomputedTable, which the
     * new AbstractValue should refer to.
     * @param valueMap a mapping from AbstractValue in the PrecomputedTable onto the SelectValue
     * that contains it.
     * @param tableSet a Set of all the tables that are being replaced - ie the Set of tables in the
     * PrecomputedTable. We use this to work out which unrepresented AbstractValues are problems.
     * @param groupBy true if the PrecomputedTable contains a GROUP BY clause
     * @return a remapped AbstractValue
     * @throws QueryOptimiserException if there is an AbstractValue that cannot be constructed,
     * because it is not present in the PrecomputedTable
     */
    protected static AbstractValue reconstructAbstractValue(AbstractValue original,
            Table precomputedSqlTable, Map valueMap, Set tableSet, boolean groupBy) throws
                QueryOptimiserException {
        SelectValue precompSelectValue = (SelectValue) valueMap.get(original);
        if (precompSelectValue != null) {
            String precompAlias = precompSelectValue.getAlias();
            return new Field(precompAlias, precomputedSqlTable);
        } else if (original instanceof Constant) {
            return original;
        } else if (original instanceof Field) {
            AbstractTable t = ((Field) original).getTable();
            if (tableSet.contains(t)) {
                throw (new QueryOptimiserException("Field not present in PrecomputedTable."));
            }
            return original;
        } else if (original instanceof Function) {
            Function originalFunction = (Function) original;
            if (originalFunction.isAggregate()) {
                if (groupBy) {
                    throw (new QueryOptimiserException(
                                "Aggregate not present in PrecomputedTable."));
                } else {
                    if (originalFunction.getOperation() == Function.COUNT) {
                        return original;
                    } else {
                        Iterator operandIter = originalFunction.getOperands().iterator();
                        AbstractValue value = (AbstractValue) operandIter.next();
                        value = reconstructAbstractValue(value, precomputedSqlTable, valueMap,
                                tableSet, groupBy);
                        Function newFunction = new Function(originalFunction.getOperation());
                        newFunction.add(value);
                        return newFunction;
                    }
                }
            } else {
                Function newFunction = new Function(originalFunction.getOperation());
                Iterator operandIter = originalFunction.getOperands().iterator();
                while (operandIter.hasNext()) {
                    AbstractValue value = (AbstractValue) operandIter.next();
                    value = reconstructAbstractValue(value, precomputedSqlTable, valueMap,
                            tableSet, groupBy);
                    newFunction.add(value);
                }
                return newFunction;
            }
        } else {
            throw (new IllegalArgumentException("Unknown type of AbstractValue."));
        }
    }

    /**
     * Populates the SELECT list of a new Query object, given an old Query's SELECT list to use as a
     * reference, a valueMap (to pass to reconstructAbstractValue), and a Table to represent the
     * PrecomputedTable.
     *
     * @param oldSelect the old Query's SELECT list to use a pattern
     * @param precomputedSqlTable the Table object that remapped AbstractValues should refer to
     * @param valueMap a mapping from AbstractValue in the PrecomputedTable onto the SelectValue
     * that contains it
     * @param tableSet a Set of all the tables that are being replaced - ie the Set of tables in the
     * PrecomputedTable. We use this to work out which unrepresented AbstractValues are problems.
     * @param groupBy true if the PrecomputedTable contains a GROUP BY clause
     * @param newQuery the new Query object to populate
     * @throws QueryOptimiserException if reconstructAbstractValue finds an AbstractValue that
     * cannot be constructed, given the PrecomputedTable
     */
    protected static void reconstructSelectValues(List oldSelect, Table precomputedSqlTable,
            Map valueMap, Set tableSet, boolean groupBy, Query newQuery) throws
                QueryOptimiserException {
        Iterator valueIter = oldSelect.iterator();
        while (valueIter.hasNext()) {
            SelectValue selectValue = (SelectValue) valueIter.next();
            AbstractValue value = selectValue.getValue();
            AbstractValue newValue = reconstructAbstractValue(value, precomputedSqlTable, valueMap,
                    tableSet, groupBy);
            SelectValue newSelectValue = new SelectValue(newValue, selectValue.getAlias());
            newQuery.addSelect(newSelectValue);
        }
    }

    /**
     * Populates the WHERE list of a new Query object, given the old Query's WHERE (or HAVING) list
     * to use as a reference.
     *
     * @param oldConstraints a Set of constraints from the old Query
     * @param precomputedSqlTable the Table object that remapped AbstractValues should refer to
     * @param valueMap a mapping from AbstractValue in the PrecomputedTable onto the SelectValue
     * that contains it.
     * @param tableSet a Set of all the tables that are being replaced - ie the Set of tables in the
     * PrecomputedTable. We use this to work out which unrepresented AbstractValues are problems.
     * @param groupBy true if the PrecomputedTable contains a GROUP BY clause
     * @param newConstraints the Set of Constraints in the Query that is being created
     * @param constraintEqualsSet a Set of Constraints that are present in both the original query
     * and the precomputed table - therefore they can be left out of the new Query
     * @param firstPrecompOrderBy a Field that the column corresponding to the first order by
     * field in the precomputed table would be mapped onto in the destination query
     * @param precompOrderBySize the number of elements in the precomputed table's order by clause
     * @param orderByField a Field that can replace the firstPrecompOrderBy field
     * @param firstPrecompOrderByHasNoNulls true if the firstPrecompOrderBy field does not permit
     * null values
     * @throws QueryOptimiserException if reconstructAbstractValue finds an AbstractValue that
     * cannot be constructed, given the PrecomputedTable
     */
    protected static void reconstructAbstractConstraints(Set oldConstraints,
            Table precomputedSqlTable, Map valueMap, Set tableSet, boolean groupBy,
            Set newConstraints, Set constraintEqualsSet, Field firstPrecompOrderBy,
            int precompOrderBySize, Field orderByField, boolean firstPrecompOrderByHasNoNulls)
        throws QueryOptimiserException {
        Iterator constraintIter = oldConstraints.iterator();
        while (constraintIter.hasNext()) {
            AbstractConstraint old = (AbstractConstraint) constraintIter.next();
            if (!constraintEqualsSet.contains(old)) {
                AbstractConstraint newConstraint = reconstructAbstractConstraint(old,
                        precomputedSqlTable, valueMap, tableSet, groupBy, firstPrecompOrderBy,
                        precompOrderBySize, orderByField, firstPrecompOrderByHasNoNulls);
                newConstraints.add(newConstraint);
            }
        }
    }

    /**
     * Reconstructs an AbstractConstraint object, using reconstructAbstractValue.
     *
     * @param oldConstraint the constraint to reconstruct
     * @param precomputedSqlTable the Table object that remapped AbstractValues should refer to
     * @param valueMap a mapping from AbstractValue in the PrecomputedTable onto the SelectValue
     * that contains it.
     * @param tableSet a Set of all the tables that are being replaced - ie the Set of tables in the
     * PrecomputedTable. We use this to work out which unrepresented AbstractValues are problems.
     * @param groupBy true if the PrecomputedTable contains a GROUP BY clause
     * @param firstPrecompOrderBy a Field that the column corresponding to the first order by
     * field in the precomputed table would be mapped onto in the destination query
     * @param precompOrderBySize the number of elements in the precomputed table's order by clause
     * @param orderByField a Field that can replace the firstPrecompOrderBy field
     * @param firstPrecompOrderByHasNoNulls true if the firstPrecompOrderBy field does not permit
     * null values
     * @return an AbstractConstraint that uses AbstractValues reconstructed by
     * reconstructAbstractValue
     * @throws QueryOptimiserException if reconstructAbstractValue finds an AbstractValue that
     * cannot be constructed, given the PrecomputedTable
     */
    protected static AbstractConstraint reconstructAbstractConstraint(
            AbstractConstraint oldConstraint, Table precomputedSqlTable, Map valueMap,
            Set tableSet, boolean groupBy, Field firstPrecompOrderBy, int precompOrderBySize,
            Field orderByField, boolean firstPrecompOrderByHasNoNulls)
        throws QueryOptimiserException {
        if (oldConstraint instanceof Constraint) {
            AbstractValue left = ((Constraint) oldConstraint).getLeft();
            AbstractValue right = ((Constraint) oldConstraint).getRight();
            left = reconstructAbstractValue(left, precomputedSqlTable, valueMap, tableSet, groupBy);
            right = reconstructAbstractValue(right, precomputedSqlTable, valueMap, tableSet,
                    groupBy);
            int operation = ((Constraint) oldConstraint).getOperation();
            if (left.equals(firstPrecompOrderBy) && (operation == Constraint.GORNULL)
                    && (right instanceof Constant)) {
                String value = ((Constant) right).toString();
                for (int i = 1; i < precompOrderBySize; i++) {
                    value = new BigInteger(value + "00000000000000000000")
                        .add(new BigInteger("50000000000000000000")).toString();
                }
                return new Constraint(new Constant(value), Constraint.LT,
                        orderByField);
            }
            if (right.equals(firstPrecompOrderBy) && (operation == Constraint.LT)
                    && (left instanceof Constant) && firstPrecompOrderByHasNoNulls) {
                String value = ((Constant) left).toString();
                for (int i = 1; i < precompOrderBySize; i++) {
                    value = new BigInteger(value + "00000000000000000000")
                        .add(new BigInteger("50000000000000000000")).toString();
                }
                return new Constraint(new Constant(value), Constraint.LT,
                        orderByField);
            }
            return new Constraint(left, operation, right);
        } else if (oldConstraint instanceof NotConstraint) {
            AbstractConstraint inner = ((NotConstraint) oldConstraint).getConstraint();
            inner = reconstructAbstractConstraint(inner, precomputedSqlTable, valueMap, tableSet,
                    groupBy, firstPrecompOrderBy, precompOrderBySize, orderByField,
                    firstPrecompOrderByHasNoNulls);
            return new NotConstraint(inner);
        } else if (oldConstraint instanceof ConstraintSet) {
            Set cons = ((ConstraintSet) oldConstraint).getConstraints();
            ConstraintSet retval = new ConstraintSet();
            Iterator consIter = cons.iterator();
            while (consIter.hasNext()) {
                AbstractConstraint con = (AbstractConstraint) consIter.next();
                con = reconstructAbstractConstraint(con, precomputedSqlTable, valueMap, tableSet,
                        groupBy, firstPrecompOrderBy, precompOrderBySize, orderByField,
                        firstPrecompOrderByHasNoNulls);
                retval.add(con);
            }
            return retval;
        } else if (oldConstraint instanceof SubQueryConstraint) {
            // TODO: We need to think about this a bit more...
            throw (new UnsupportedOperationException("Need to think about SubQueryConstraints."));
        } else if (oldConstraint instanceof InListConstraint) {
            AbstractValue left = ((InListConstraint) oldConstraint).getLeft();
            Set right = ((InListConstraint) oldConstraint).getRight();
            left = reconstructAbstractValue(left, precomputedSqlTable, valueMap, tableSet, groupBy);
            InListConstraint retval = new InListConstraint(left);
            retval.addAll(right);
            return retval;
        }
        throw (new IllegalArgumentException("Unknown constraint type."));
    }

    /**
     * Reconstructs a Collection of AbstractValue objects, calling reconstructAbstractValue on each
     * one before adding it to another Collection. Reconstructed values will be added to the
     * destination Collection in the same order as the iterator of the original Collection.
     *
     * @param oldValues the Collection of AbstractValue objects to reconstruct
     * @param precomputedSqlTable the Table object that remapped AbstractValues should refer to
     * @param valueMap a mapping from AbstractValue in the PrecomputedTable onto the SelectValue
     * that contains it.
     * @param tableSet a Set of all the tables that are being replaced - ie the Set of tables in the
     * PrecomputedTable. We use this to work out which unrepresented AbstractValues are problems.
     * @param groupBy true if the PrecomputedTable contains a GROUP BY clause
     * @param newValues the Collection to put the reconstructed AbstractValues in
     * @throws QueryOptimiserException if reconstructAbstractValue finds an AbstractValue that
     * cannot be constructed, given the PrecomputedTable
     */
    public static void reconstructAbstractValues(Collection oldValues, Table precomputedSqlTable,
            Map valueMap, Set tableSet, boolean groupBy, Collection newValues) throws
                QueryOptimiserException {
        Iterator valueIter = oldValues.iterator();
        while (valueIter.hasNext()) {
            AbstractValue value = (AbstractValue) valueIter.next();
            value = reconstructAbstractValue(value, precomputedSqlTable, valueMap, tableSet,
                    groupBy);
            newValues.add(value);
        }
    }

    /**
     * Adds all required FROM items to the FROM list of the new query. Adds all items in input,
     * minus the items in subtract, to the Set output.
     *
     * @param input a Set of items to be added to the output
     * @param subtract a Set of items to miss out
     * @param output a destination Set to add items to
     */
    public static void addNonCoveredFrom(Set input, Set subtract, Set output) {
        Iterator inputIter = input.iterator();
        while (inputIter.hasNext()) {
            Object inObj = inputIter.next();
            if (!subtract.contains(inObj)) {
                output.add(inObj);
            }
        }
    }
}
