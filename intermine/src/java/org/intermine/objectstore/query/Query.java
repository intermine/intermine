package org.flymine.objectstore.query;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * This class provides an implementation-independent abstract representation of a query
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class Query
{
    private boolean distinct = true;
    private int maxResults = -1;
    private Constraint constraint = null;
    private Set queryClasses = new HashSet(); // @element-type QueryClass
    private List select = new ArrayList(); // @element-type QueryNode
    private List orderBy = new ArrayList(); // @element-type QueryNode
    private Set groupBy = new HashSet(); // @element-type QueryNode
    private Map aliases = new HashMap();

    private int alias = 1;

    /**
     * Adds a QueryClass to the FROM clause of this Query
     *
     * @param cls the QueryClass to be added
     * @return the updated Query
     */    
    public Query addClass(QueryClass cls) {
        if (cls == null) {
            throw new NullPointerException("cls must not be null");
        }
        queryClasses.add(cls);
        return this;
    }

    /**
     * Remove a QueryClass from the FROM clause
     *
     * @param cls the QueryClass to remove
     * @return the updated Query
     */
    public Query deleteClass(QueryClass cls) {
        queryClasses.remove(cls);
        return this;
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
     * Add a QueryNode to the ORDER BY clause of this Query
     *
     * @param node the node to add
     * @return the updated Query
     */    
    public Query addToOrderBy(QueryNode node) {
        orderBy.add(node);
        return this;
    }

    /**
     * Remove a QueryNode from the ORDER BY clause
     *
     * @param node the node to remove
     * @return the updated Query
     */    
    public Query deleteFromOrderBy(QueryNode node) {
        orderBy.remove(node);
        return this;
    }

    /**
     * Add a QueryNode to the SELECT clause of this Query
     *
     * @param node the QueryNode to add
     * @return the updated Query
     */    
    public Query addToSelect(QueryNode node) {
        select.add(node);
        aliases.put(node, "a" + (alias++));
        return this;
    }

    /**
     * Remove a QueryNode from the SELECT clause
     *
     * @param node the node to remove
     * @return the updated Query
     */    
    public Query deleteFromSelect(QueryNode node) {
        select.remove(node);
        aliases.remove(node);
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
     * Get the number of results returned by this Query
     *
     * @return the number of results
     */    
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Set the maximum number of results this Query should return
     *
     * @param maxResults the number of results
     */    
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }    
    
    /**
     * Get the value of the distinct property
     *
     * @return the value of distinct
     */    
    public boolean getDistinct() {
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
     * Returns a string representation of this Query object
     *
     * @return a String representation
     */    
    public String toString() {
        return null;
    }
}
