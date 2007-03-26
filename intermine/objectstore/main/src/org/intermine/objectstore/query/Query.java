package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
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
public class Query implements FromElement
{
    private boolean distinct = true;
    private Constraint constraint = null;
    private Set queryClasses = new LinkedHashSet(); // @element-type FromElement
    private List select = new ArrayList(); // @element-type QueryNode
    private List orderBy = new ArrayList(); // @element-type QueryNode
    private Set groupBy = new LinkedHashSet(); // @element-type QueryNode
    private Map aliases = new HashMap();
    private Map reverseAliases = new HashMap();

    private int aliasNo = 1;

    /**
     * Empty constructor.
     */
    public Query() {
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
        queryClasses.remove(cls);
        return this;
    }

    /**
     * Returns all FromElements in the FROM clause
     *
     * @return list of FromElements
     */
    public Set getFrom() {
        return Collections.unmodifiableSet(queryClasses);
    }

    /**
       * Constrain this Query using either a single constraint or a set of constraints
       *
       * @param constraint the constraint or constraint set
       * @return the updated query
       */
    public Query setConstraint(Constraint constraint) {
        this.constraint = constraint;
        return this;
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
        groupBy.remove(node);
        return this;
    }

    /**
     * Gets the GROUP BY clause of this Query
     *
     * @return the set of GROUP BY nodes
     */
    public Set getGroupBy() {
        return Collections.unmodifiableSet(groupBy);
    }

    /**
     * Add a QueryOrderable to the ORDER BY clause of this Query
     *
     * @param node the node to add
     * @return the updated Query
     */
    public Query addToOrderBy(QueryOrderable node) {
        orderBy.add(node);
        return this;
    }

    /**
     * Remove a QueryOrderable from the ORDER BY clause
     *
     * @param node the node to remove
     * @return the updated Query
     */
    public Query deleteFromOrderBy(QueryOrderable node) {
        orderBy.remove(node);
        return this;
    }

    /**
     * Clears the ORDER BY clause of this Query
     *
     */
    public void clearOrderBy() {
        orderBy.clear();
    }

    /**
     * Gets the ORDER BY clause of this Query
     *
     * @return the List of ORDER BY nodes
     */
    public List getOrderBy() {
        return Collections.unmodifiableList(orderBy);
    }

    /**
     * Gets the effective ORDER BY clause of this Query, such as may be used to create SQL.
     *
     * @return a List of ORDER BY nodes
     */
    public List getEffectiveOrderBy() {
        Set seenQueryClasses = new HashSet();
        List retval = new ArrayList();
        List iterators = new ArrayList();
        iterators.add(orderBy.iterator());
        iterators.add(select.iterator());
        Iterator iter = new CombinedIterator(iterators);
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
     * @return the updated Query
     */
    public Query addToSelect(QuerySelectable node) {
        select.add(node);
        alias(node, null);
        return this;
    }

    /**
     * Add a QuerySelectable to the SELECT clause of this Query
     *
     * @param node the QuerySelectable to add
     * @param alias the alias for this FromElement
     * @return the updated Query
     */
    public Query addToSelect(QuerySelectable node, String alias) {
        select.add(node);
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
        select.remove(node);

        // Don't think the following is sufficient - what if the node is also in the FROM list?
        // Matthew: don't need to do this at all. Extra entries in the alias map will be ignored.
        //String alias = (String) aliases.remove(node);
        //if (alias != null) {
        //    reverseAliases.remove(alias);
        //}
        return this;
    }

    /**
     * Gets the SELECT list
     *
     * @return the (unmodifiable) list
     */
    public List getSelect() {
        return Collections.unmodifiableList(select);
    }

    /**
     * Clears the SELECT list
     *
     */
    public void clearSelect() {
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
        this.distinct = distinct;
    }

    /**
     * Returns the map of SELECTed QueryNodes to String aliases
     *
     * @return the map
     */
    public Map getAliases() {
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Returns the map of String aliases to SELECTed QueryNodes
     *
     * @return the map
     */
    public Map getReverseAliases() {
        return Collections.unmodifiableMap(reverseAliases);
    }

    /**
     * Returns a string representation of this Query object
     *
     * @return a String representation
     */
    public String toString() {
        IqlQuery fq = new IqlQuery(this);
        return fq.toString();
    }

    /**
     * Set an alias for an element in the Query
     *
     * @param obj the element to alias
     * @param alias the alias to give
     */
    public void alias(Object obj, String alias) {

        if ((alias != null) && reverseAliases.containsKey(alias)
            && (!obj.equals(reverseAliases.get(alias)))) {
            throw new IllegalArgumentException("Alias " + alias + " is already in use");
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
