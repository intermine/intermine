package org.flymine.sql.query;

import java.util.*;

/**
 * Represents an SQL query in parsed form.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class Query implements SQLStringable
{
    protected List select;
    protected Set from;
    protected Set where;
    protected Set groupBy;
    protected Set having;
    protected List orderBy;
    protected int limit;
    protected int offset;
    protected boolean explain;
    protected boolean distinct;

    /**
     * Construct a new parsed Query.
     *
     */
    public Query() {
        select = new ArrayList();
        from = new HashSet();
        where = new HashSet();
        groupBy = new HashSet();
        having = new HashSet();
        orderBy = new ArrayList();
        limit = 0;
        offset = 0;
        explain = false;
        distinct = false;
    }

    /**
     * Gets the current distinct status of this query.
     *
     * @return true if this query is distinct
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Sets the distinct status of this query.
     * 
     * @param distinct the new distinct status
     */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }
    
    /**
     * Gets the current explain status of this query.
     *
     * @return true if this query is an explain
     */
    public boolean isExplain() {
        return explain;
    }

    /**
     * Sets the explain status of this query.
     *
     * @param explain the new explain status
     */
    public void setExplain(boolean explain) {
        this.explain = explain;
    }

    /**
     * Gets the list of select fields for this query.
     *
     * @return a List of SelectValue objects representing the select list of the query
     */
    public List getSelect() {
        return select;
    }

    /**
     * Adds a field to the select list of this query. Fields are stored in a List in the order they
     * are added.
     *
     * @param obj a SelectValue to add to the list
     */
    public void addSelect(SelectValue obj) {
        select.add(obj);
    }

    /**
     * Gets the Set of from tables for this query.
     *
     * @return a Set of AbstractTable objects representing the from list of the query
     */
    public Set getFrom() {
        return from;
    }

    /**
     * Adds a table to the from list of this query. The order is not important.
     *
     * @param obj an AbstractTable to add to the set
     */
    public void addFrom(AbstractTable obj) {
        from.add(obj);
    }

    /**
     * Gets the Set of constraints in the where clause of this query.
     *
     * @return a Set of AbstractConstraint objects which, ANDed together form the where clause
     */
    public Set getWhere() {
        return where;
    }

    /**
     * Adds a constraint to the where clause for this query. The order is not important. The
     * constraints in the Set formed are ANDed together to form the where clause. If you wish to OR
     * constraints together, use a ConstraintSet.
     *
     * @param obj an AbstractConstraint to add to the where clause
     */
    public void addWhere(AbstractConstraint obj) {
        where.add(obj);
    }

    /**
     * Gets the Set of fields in the GROUP BY clause of this query.
     *
     * @return a Set of AbstractValue objects representing the GROUP BY clause
     */
    public Set getGroupBy() {
        return groupBy;
    }

    /**
     * Adds a field to the GROUP BY clause of this query. The order is not important.
     *
     * @param obj an AbstractValue to add to the GROUP BY clause
     */
    public void addGroupBy(AbstractValue obj) {
        groupBy.add(obj);
    }

    /**
     * Gets the set of constraints forming the HAVING clause of this query.
     *
     * @return a Set of AbstractConstraints representing the HAVING clause
     */
    public Set getHaving() {
        return having;
    }

    /**
     * Adds a constraint to the HAVING clause of this query. The order is not important.
     *
     * @param obj an AbstractConstraint to add to the HAVING clause
     */
    public void addHaving(AbstractConstraint obj) {
        having.add(obj);
    }
    
    /**
     * Gets the list of fields forming the ORDER BY clause of this query.
     *
     * @return a List of AbstractValues representing the ORDER BY clause
     */
    public List getOrderBy() {
        return orderBy;
    }

    /**
     * Adds a field to the ORDER BY clause of this query. The fields are repesented in the clause in
     * the order they were added.
     *
     * @param obj an AbstractValue to add to the ORDER BY clause
     */
    public void addOrderBy(AbstractValue obj) {
        orderBy.add(obj);
    }

    /**
     * Gets the LIMIT number for this query.
     *
     * @return the maximum number of rows that this query is allowed to return
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Gets the OFFSET number for this query.
     *
     * @return the number of rows in the query to discard before returning the first result
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the LIMIT and OFFSET numbers for this query.
     *
     * @param limit the LIMIT number
     * @param offset the OFFSET number
     */
    public void setLimitOffset(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }
    
    /**
     * Convert this Query into a SQL String query.
     *
     * @return this Query in String form
     */
    public String getSQLString() {
        return (explain ? "EXPLAIN " : "") + "SELECT " + (distinct ? "DISTINCT " : "")
            + collectionToSQLString(select, ", ")
            + (from.isEmpty() ? "" : " FROM " + collectionToSQLString(from, ", "))
            + (where.isEmpty() ? "" : " WHERE " + collectionToSQLString(where, " AND "))
            + (groupBy.isEmpty() ? "" : " GROUP BY " + collectionToSQLString(groupBy, ", ")
                + (having.isEmpty() ? "" : " HAVING " + collectionToSQLString(having, " AND ")))
            + (orderBy.isEmpty() ? "" : " ORDER BY " + collectionToSQLString(orderBy, ", "))
            + (limit == 0 ? "" : " LIMIT " + limit
                + (offset == 0 ? "" : " OFFSET " + offset));
    }

    /**
     * Converts a collection of objects that implement the getSQLString method into a String,
     * with the given comma string between each element.
     *
     * @param c the Collection on objects
     * @param comma the String to use as a separator between elements
     * @return a String representation
     */
    protected static String collectionToSQLString(Collection c, String comma) {
        String retval = "";
        boolean needComma = false;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            SQLStringable o = (SQLStringable) iter.next();
            // TODO: Should we have an interface for classes that implement getSQLString()?
            if (needComma) {
                retval += comma;
            }
            needComma = true;
            retval += o.getSQLString();
        }
        return retval;
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is equivalent
     */
    public boolean equals(Object obj) {
        if (obj instanceof Query) {
            Query q = (Query) obj;
            return select.equals(q.select) && from.equals(q.from) && where.equals(q.where)
                && groupBy.equals(q.groupBy) && having.equals(q.having) && orderBy.equals(q.orderBy)
                && (limit == q.limit) && (offset == q.limit) && (explain == q.explain)
                && (distinct == q.distinct);
        }
        return false;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer created from the contents of the Query
     */
    public int hashCode() {
        return (3 * select.hashCode()) + (5 * from.hashCode())
            + (7 * where.hashCode()) + (11 * groupBy.hashCode())
            + (13 * having.hashCode()) + (17 * orderBy.hashCode()) + (19 * limit) + (23 * offset)
            + (explain ? 29 : 0) + (distinct ? 31 : 0);
    }
}
