package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.util.CombinedIterator;

/**
 * This class provides an implementation-independent abstract representation of a query
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class Query implements FromElement, Queryable
{
    private boolean distinct = true;
    private Constraint constraint = null;
    private Set<FromElement> queryClasses = new LinkedHashSet<FromElement>();
    private List<QuerySelectable> select = new ArrayList<QuerySelectable>();
    private List<QueryOrderable> orderBy = new ArrayList<QueryOrderable>();
    private Set<QueryNode> groupBy = new LinkedHashSet<QueryNode>();
    private Map<Object, String> aliases = new IdentityHashMap<Object, String>();
    private Map<String, Object> reverseAliases = new HashMap<String, Object>();
    private int limit = Integer.MAX_VALUE;
    // This object caches the current query's IQL, to improve performance. All methods that morph
    // this must set this reference to null.
    private IqlQuery iqlQuery;

    private int aliasNo = 1;

    /**
     * Empty constructor.
     */
    public Query() {
        // empty
    }

    /**
     * Sets the LIMIT parameter for this query - note that this is only honoured in a subquery.
     *
     * @param limit the new limit parameter - the results will be truncated to this many rows
     */
    public void setLimit(int limit) {
        iqlQuery = null;
        this.limit = limit;
    }

    /**
     * Returns the LIMIT parameter for this query.
     *
     * @return the limit parameter
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Adds a FromElement to the FROM clause of this Query
     *
     * @param cls the FromElement to be added
     * @return the updated Query
     */
    public Query addFrom(FromElement cls) {
        if (cls == null) {
            throw new NullPointerException("cls must not be null");
        }
        iqlQuery = null;
        queryClasses.add(cls);
        alias(cls, null);
        return this;
    }

    /**
     * Adds a FromElement to the FROM clause of this Query
     *
     * @param cls the FromElement to be added
     * @param alias the alias for this FromElement
     * @return the updated Query
     */
    public Query addFrom(FromElement cls, String alias) {
        if (cls == null) {
            throw new NullPointerException("cls must not be null");
        }
        iqlQuery = null;
        queryClasses.add(cls);
        alias(cls, alias);
        return this;
    }

    /**
     * Remove a FromElement from the FROM clause
     *
     * @param cls the FromElement to remove
     * @return the updated Query
     */
    public Query deleteFrom(FromElement cls) {
        iqlQuery = null;
        queryClasses.remove(cls);
        String alias = aliases.remove(cls);
        if (alias != null) {
            reverseAliases.remove(alias);
        }
        return this;
    }

    /**
     * Returns all FromElements in the FROM clause
     *
     * @return set of FromElements
     */
    public Set<FromElement> getFrom() {
        return Collections.unmodifiableSet(queryClasses);
    }

    /**
       * Constrain this Query using either a single constraint or a set of constraints
       *
       * @param constraint the constraint or constraint set
       */
    public void setConstraint(Constraint constraint) {
        iqlQuery = null;
        this.constraint = constraint;
    }

    /**
       * Get the current constraint on this Query
       *
       * @return the constraint
       */
    public Constraint getConstraint() {
        return constraint;
    }

    /**
     * Add a QueryNode to the GROUP BY clause of this Query
     *
     * @param node the node to add
     * @return the updated Query
     */
    public Query addToGroupBy(QueryNode node) {
        iqlQuery = null;
        groupBy.add(node);
        return this;
    }

    /**
     * Remove a QueryNode from the GROUP BY clause
     *
     * @param node the node to remove
     * @return the updated Query
     */
    public Query deleteFromGroupBy(QueryNode node) {
        iqlQuery = null;
        groupBy.remove(node);
        return this;
    }

    /**
     * Gets the GROUP BY clause of this Query
     *
     * @return the set of GROUP BY nodes
     */
    public Set<QueryNode> getGroupBy() {
        return Collections.unmodifiableSet(groupBy);
    }

    /**
     * Add a QueryOrderable to the ORDER BY clause of this Query
     *
     * @param node the node to add
     * @return the updated Query
     */
    public Query addToOrderBy(QueryOrderable node) {
        iqlQuery = null;
        orderBy.add(node);
        return this;
    }

    /**
     * Add a QueryOrderable to the ORDER BY clause of this Query
     *
     * @param node the node to add
     * @param direction ascending or descending
     * @return the updated Query
     */
    public Query addToOrderBy(QueryOrderable node, String direction) {
        iqlQuery = null;
        if ("desc".equals(direction)) {
            OrderDescending o = new OrderDescending(node);
            orderBy.add(o);
        } else {
            orderBy.add(node);
        }

        return this;
    }


    /**
     * Remove a QueryOrderable from the ORDER BY clause
     *
     * @param node the node to remove
     * @return the updated Query
     */
    public Query deleteFromOrderBy(QueryOrderable node) {
        iqlQuery = null;
        orderBy.remove(node);
        return this;
    }

    /**
     * Clears the ORDER BY clause of this Query
     *
     */
    public void clearOrderBy() {
        iqlQuery = null;
        orderBy.clear();
    }

    /**
     * Gets the ORDER BY clause of this Query
     *
     * @return the List of ORDER BY nodes
     */
    public List<QueryOrderable> getOrderBy() {
        return Collections.unmodifiableList(orderBy);
    }

    /**
     * Gets the effective ORDER BY clause of this Query, such as may be used to create SQL.
     *
     * @return a List of ORDER BY nodes
     */
    public List<Object> getEffectiveOrderBy() {
        Set<Object> seenQueryClasses = new HashSet<Object>();
        List<Object> retval = new ArrayList<Object>();
        List<Iterator<? extends Object>> iterators = new ArrayList<Iterator<? extends Object>>();
        iterators.add(orderBy.iterator());
        iterators.add(select.iterator());
        Iterator<Object> iter = new CombinedIterator<Object>(iterators);
        while (iter.hasNext()) {
            Object node = iter.next();
            if (node instanceof QueryClass) {
                if (!seenQueryClasses.contains(node)) {
                    retval.add(node);
                    seenQueryClasses.add(node);
                }
            } else if (node instanceof QueryField) {
                FromElement qc = ((QueryField) node).getFromElement();
                if ((qc instanceof QueryClass) || (qc instanceof QueryClassBag)) {
                    if (!seenQueryClasses.contains(qc)) {
                        if ("id".equals(((QueryField) node).getFieldName())) {
                            seenQueryClasses.add(qc);
                        }
                        retval.add(node);
                    }
                } else if (qc instanceof Query) {
                    retval.add(node);
                }
            } else if (node instanceof QueryObjectReference) {
                QueryClass qc = ((QueryObjectReference) node).getQueryClass();
                if (!seenQueryClasses.contains(qc)) {
                    retval.add(node);
                }
            } else {
                retval.add(node);
            }
        }
        return Collections.unmodifiableList(retval);
    }

    /**
     * Add a QuerySelectable to the SELECT clause of this Query
     *
     * @param node the QuerySelectable to add
     */
    public void addToSelect(QuerySelectable node) {
        iqlQuery = null;
        select.add(node);
        if (node instanceof PathExpressionField) {
            alias(((PathExpressionField) node).getQope(), null);
        }
        alias(node, null);
    }

    /**
     * Add a QuerySelectable to the SELECT clause of this Query
     *
     * @param node the QuerySelectable to add
     * @param alias the alias for this FromElement
     * @return the updated Query
     */
    public Query addToSelect(QuerySelectable node, String alias) {
        iqlQuery = null;
        select.add(node);
        if (node instanceof PathExpressionField) {
            alias(((PathExpressionField) node).getQope(), null);
        }
        alias(node, alias);
        return this;
    }

    /**
     * Remove a QuerySelectable from the SELECT clause
     *
     * @param node the QuerySelectable to remove
     * @return the updated Query
     */
    public Query deleteFromSelect(QuerySelectable node) {
        iqlQuery = null;
        select.remove(node);

        if (!(node instanceof FromElement)) {
            String alias = aliases.remove(node);
            if (alias != null) {
                reverseAliases.remove(alias);
            }
        }
        return this;
    }

    /**
     * Gets the SELECT list
     *
     * @return the (unmodifiable) list
     */
    public List<QuerySelectable> getSelect() {
        return Collections.unmodifiableList(select);
    }

    /**
     * Clears the SELECT list
     *
     */
    public void clearSelect() {
        iqlQuery = null;
        for (QuerySelectable qs : select) {
            if (!(qs instanceof FromElement)) {
                String alias = aliases.remove(qs);
                if (alias != null) {
                    reverseAliases.remove(alias);
                }
            }
        }
        select.clear();
    }

    /**
     * Get the value of the distinct property
     *
     * @return the value of distinct
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Set the value of the distinct property, which determines whether duplicates are
     * permitted in the results returned by this Query
     *
     * @param distinct the value of distinct
     */
    public void setDistinct(boolean distinct) {
        iqlQuery = null;
        this.distinct = distinct;
    }

    /**
     * Returns the map of QuerySelectables and FromElements to String aliases
     *
     * @return the map
     */
    public Map<Object, String> getAliases() {
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Returns the map of String aliases to QuerySelectables and FromElements
     *
     * @return the map
     */
    public Map<String, Object> getReverseAliases() {
        return Collections.unmodifiableMap(reverseAliases);
    }

    /**
     * Returns a string representation of this Query object
     *
     * @return a String representation
     */
    @Override
    public String toString() {
        return getIqlQuery().toString();
    }

    /**
     * Returns an IqlQuery object representing this query, that may have been cached.
     *
     * @return an IqlQuery object
     */
    public IqlQuery getIqlQuery() {
        if (iqlQuery == null) {
            iqlQuery = new IqlQuery(this);
        }
        return iqlQuery;
    }

    /**
     * Set an alias for an element in the Query.
     *
     * @param obj the element to alias
     * @param alias the alias to give
     */
    public void alias(Object obj, String alias) {
        iqlQuery = null;
        if ((alias != null) && reverseAliases.containsKey(alias)
            && (!obj.equals(reverseAliases.get(alias)))) {
            throw new IllegalArgumentException("Alias " + alias + " is already in use. Adding to "
                    + toString() + " object " + obj + " with alias " + alias);
        }

        if ((alias != null) && aliases.containsKey(obj)
            && (!alias.equals(aliases.get(obj)))) {
            throw new IllegalArgumentException("Cannot re-alias the same element");
        }

        while (reverseAliases.containsKey("a" + aliasNo + "_")) {
            aliasNo++;
        }

        if (!aliases.containsKey(obj)) {
            if (alias == null) {
                alias = "a" + aliasNo + "_";
            }
            aliases.put(obj, alias);
            reverseAliases.put(alias, obj);
        }
    }
}
