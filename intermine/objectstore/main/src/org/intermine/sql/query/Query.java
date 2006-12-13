package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

// TODO: Tree parser to convert conditions to conjunctive normal form.
//              Done.
//       Handling subqueries that reference values from the scope of the surrounding Query.
//              Note: one should probably delay parsing of subqueries until after all normal
//              tables. This still doesn't allow for subqueries referencing variables created
//              in the surrounding scope by other subqueries, but that is impossible with the
//              current java Query object.
//              Done.
//       Handling conditions that go (A AND B OR C). I believe this is ((A AND B) OR C). Currently
//              the parser rejects this.
//       Handle "WHERE x in ('value1', 'value2')" - translate to an OR_CONSTRAINT_SET

import java.util.*;
import java.io.*;

import antlr.collections.AST;
//import antlr.debug.misc.ASTFrame;

import org.intermine.util.ConsistentSet;


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
    protected List queriesInUnion;

    private Map aliasToTable;
    private Map originalAliasToTable;
    private AbstractTable onlyTable;

    /**
     * Construct a new Query.
     */
    public Query() {
        select = new ArrayList();
        from = new ConsistentSet();
        where = new ConsistentSet();
        groupBy = new ConsistentSet();
        having = new ConsistentSet();
        orderBy = new ArrayList();
        limit = 0;
        offset = 0;
        explain = false;
        distinct = false;
        queriesInUnion = new ArrayList();
        queriesInUnion.add(this);
        onlyTable = null;
        this.aliasToTable = null;
        this.originalAliasToTable = null;
    }

    /**
     * Construct a new Query.
     *
     * @param aliasToTable a map of tables in a surrounding query, which are in the scope of this
     * query.
     */
    public Query(Map aliasToTable) {
        this();

        this.aliasToTable = new HashMap();
        this.aliasToTable.putAll(aliasToTable);
        this.originalAliasToTable = aliasToTable;
    }

    /**
     * Construct a new Query.
     *
     * @param aliasToTable a Map of tables in a surrounding query, which are in the scope of this
     * query
     * @param queriesInUnion a List of Queries which are in the currently-being-created UNION of
     * queries, to which this constructor should add this
     */
    public Query(Map aliasToTable, List queriesInUnion) {
        this();

        this.aliasToTable = new HashMap();
        this.aliasToTable.putAll(aliasToTable);
        this.originalAliasToTable = aliasToTable;

        queriesInUnion.add(this);
        this.queriesInUnion = queriesInUnion;
    }

    /**
     * Construct a new parsed Query from a String.
     *
     * @param sql a SQL SELECT String to parse
     * @throws IllegalArgumentException if the SQL String is invalid
     */
    public Query(String sql) {
        this(sql, true);
    }

    /**
     * Construct a new parsed Query from a String.
     *
     * @param sql a SQL SELECT String to parse
     * @param treeParse true if a tree-parse step is required (usually so)
     * @throws IllegalArgumentException if the SQL String is invalid
     */
    public Query(String sql, boolean treeParse) {
        this();

        aliasToTable = new HashMap();
        originalAliasToTable = new HashMap();
        try {
            InputStream is = new ByteArrayInputStream(sql.getBytes());

            SqlLexer lexer = new SqlLexer(is);
            SqlParser parser = new SqlParser(lexer);
            parser.start_rule();

            AST ast = parser.getAST();
            if (ast == null) {
                throw (new IllegalArgumentException("Invalid SQL string " + sql));
            }
            if (treeParse) {
                AST oldAst;
                do {
                    oldAst = ast;
                    SqlTreeParser treeparser = new SqlTreeParser();
                    treeparser.start_rule(ast);
                    ast = treeparser.getAST();
                    if (ast == null) {
                        throw (new IllegalArgumentException("Invalid SQL string " + sql));
                    }
                } while (!oldAst.equalsList(ast));
            }

            processSqlStatementAST(ast);
        } catch (antlr.RecognitionException e) {
            IllegalArgumentException e2 = new IllegalArgumentException();
            e2.initCause(e);
            throw e2;
        } catch (antlr.TokenStreamException e) {
            IllegalArgumentException e2 = new IllegalArgumentException();
            e2.initCause(e);
            throw e2;
        }
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
        if (aliasToTable != null) {
            aliasToTable.put(obj.getAlias(), obj);
        }
        onlyTable = obj;
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
     * Adds another Query to the UNION set of this query.
     *
     * @param query the Query to UNION with this Query
     */
    public void addToUnion(Query query) {
        queriesInUnion.addAll(query.queriesInUnion);
    }

    /**
     * Returns the List of queries in the UNION set of this query.
     *
     * @return a List of Query objects
     */
    public List getUnion() {
        return queriesInUnion;
    }

    /**
     * Convert this Query into a SQL String query.
     *
     * @return this Query in String form
     */
    public String getSQLString() {
        boolean needComma = false;
        String retval = "";
        Iterator queryIter = queriesInUnion.iterator();
        while (queryIter.hasNext()) {
            Query q = (Query) queryIter.next();
            if (needComma) {
                retval += " UNION ";
            }
            needComma = true;
            retval += q.getSQLStringNoUnion();
        }
        return retval;
    }

    /** Convert this Query into a SQL String query, without regard to the other members of the
     * UNION.
     *
     * @return this Query in String form
     */
    public String getSQLStringNoUnion() {
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
     * Convert this Query into a SQL String query, without regard to the other members of the the
     * UNION, with an extra field in the SELECT list.
     *
     * @param extraSelect an extra String to put into the select list
     * @return this Query in String form
     */
    public String getSQLStringForPrecomputedTable(String extraSelect) {
        List augmentedSelect = new ArrayList(select);
        augmentedSelect.add(extraSelect);
        return (explain ? "EXPLAIN " : "") + "SELECT " + (distinct ? "DISTINCT " : "")
            + collectionToSQLString(augmentedSelect, ", ")
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
            if (needComma) {
                retval += comma;
            }
            needComma = true;
            Object o = iter.next();
            if (o instanceof SQLStringable) {
                retval += ((SQLStringable) o).getSQLString();
            } else {
                retval += (String) o;
            }
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
            // Now, we need to check that the two queriesInUnion Lists are equivalent, without ever
            // calling the .equals() method of the Query objects in the List.
            if (queriesInUnion.size() != q.queriesInUnion.size()) {
                return false;
            }
            boolean used[] = new boolean[queriesInUnion.size()];
            for (int i = 0; i < queriesInUnion.size(); i++) {
                used[i] = false;
            }
            for (int i = 0; i < queriesInUnion.size(); i++) {
                Query thisQ = (Query) queriesInUnion.get(i);
                boolean notFound = true;
                for (int o = 0; (o < queriesInUnion.size()) && notFound; o++) {
                    Query thatQ = (Query) q.queriesInUnion.get(o);
                    if (thisQ.equalsNoUnion(thatQ) && (!used[o])) {
                        notFound = false;
                        used[o] = true;
                    }
                }
                if (notFound) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true if this Query is equivalent to obj, disregarding other queries in the UNION.
     *
     * @param obj the object to compare to
     * @return true if equal
     */
    public boolean equalsNoUnion(Object obj) {
        if (obj instanceof Query) {
            Query q = (Query) obj;
            return select.equals(q.select) && from.equals(q.from) && where.equals(q.where)
                && groupBy.equals(q.groupBy) && having.equals(q.having) && orderBy.equals(q.orderBy)
                && (limit == q.limit) && (offset == q.offset) && (explain == q.explain)
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
        int retval = 0;
        for (int i = 0; i < queriesInUnion.size(); i++) {
            retval += ((Query) queriesInUnion.get(i)).hashCodeNoUnion();
        }
        return retval;
    }

    /**
     * Returns a partial hashcode, ignoring other queries in the union.
     *
     * @return an integer
     */
    public int hashCodeNoUnion() {
        return (3 * select.hashCode()) + (5 * from.hashCode())
            + (7 * where.hashCode()) + (11 * groupBy.hashCode())
            + (13 * having.hashCode()) + (17 * orderBy.hashCode()) + (19 * limit) + (23 * offset)
            + (explain ? 29 : 0) + (distinct ? 31 : 0);
    }

    /**
     * Overrides Object.toString().
     *
     * @return a String representation of this Query
     */
    public String toString() {
        return getSQLString();
    }

    /**
     * Processes a SQL_STATEMENT AST node produced by antlr.
     *
     * @param ast an AST node to process
     */
    private void processSqlStatementAST(AST ast) {
        if (ast.getType() != SqlTokenTypes.SQL_STATEMENT) {
            throw (new IllegalArgumentException("Expected: a SQL SELECT statement"));
        }
        processAST(ast.getFirstChild());
        ast = ast.getNextSibling();
        if ((ast != null) && (ast.getType() == SqlTokenTypes.SQL_STATEMENT)) {
            (new Query(originalAliasToTable, queriesInUnion)).processSqlStatementAST(ast);
        }
    }

    /**
     * Processes an AST node produced by antlr, at the top level of the SQL query.
     *
     * @param ast an AST node to process
     */
    private void processAST(AST ast) {
        boolean processSelect = false;
        switch (ast.getType()) {
            case SqlTokenTypes.LITERAL_explain:
                explain = true;
                break;
            case SqlTokenTypes.LITERAL_distinct:
                distinct = true;
                break;
            case SqlTokenTypes.SELECT_LIST:
                // Always do the select list last.
                processSelect = true;
                break;
            case SqlTokenTypes.FROM_LIST:
                processFromList(ast.getFirstChild());
                break;
            case SqlTokenTypes.WHERE_CLAUSE:
                processWhereClause(ast.getFirstChild());
                break;
            case SqlTokenTypes.GROUP_CLAUSE:
                processGroupClause(ast.getFirstChild());
                break;
            case SqlTokenTypes.HAVING_CLAUSE:
                processHavingClause(ast.getFirstChild());
                break;
            case SqlTokenTypes.ORDER_CLAUSE:
                processOrderClause(ast.getFirstChild());
                break;
            case SqlTokenTypes.LIMIT_CLAUSE:
                processLimitClause(ast.getFirstChild());
                break;
            default:
                throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]"));
        }
        if (ast.getNextSibling() != null) {
            processAST(ast.getNextSibling());
        }
        if (processSelect) {
            processSelectList(ast.getFirstChild());
        }
    }

    /**
     * Processes an AST node that describes a FROM list.
     *
     * @param ast an AST node to process
     */
    private void processFromList(AST ast) {
        boolean processSubQuery = false;
        switch (ast.getType()) {
            case SqlTokenTypes.TABLE:
                processNewTable(ast.getFirstChild());
                break;
            case SqlTokenTypes.SUBQUERY:
                // Always do subqueries last.
                processSubQuery = true;
                break;
            default:
                throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]"));
        }
        if (ast.getNextSibling() != null) {
            processFromList(ast.getNextSibling());
        }
        if (processSubQuery) {
            processNewSubQuery(ast.getFirstChild());
        }
    }

    /**
     * Processes an AST node that describes a table in the FROM list.
     *
     * @param ast an AST node to process
     */
    private void processNewTable(AST ast) {
        String tableName = null;
        String tableAlias = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.TABLE_NAME:
                    tableName = ast.getFirstChild().getText();
                    break;
                case SqlTokenTypes.TABLE_ALIAS:
                    tableAlias = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        addFrom(new Table(tableName, tableAlias));
    }

    /**
     * Processes an AST node that describes a subquery in the FROM list.
     *
     * @param ast an AST node to process
     */
    private void processNewSubQuery(AST ast) {
        AST subquery = null;
        String alias = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.SQL_STATEMENT:
                    if (subquery == null) {
                        subquery = ast;
                    }
                    break;
                case SqlTokenTypes.TABLE_ALIAS:
                    alias = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        Query q = new Query(aliasToTable);
        q.processSqlStatementAST(subquery);
        addFrom(new SubQuery(q, alias));
    }

    /**
     * Processes an AST node that describes a SELECT list.
     *
     * @param ast an AST node to process
     */
    public void processSelectList(AST ast) {
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.SELECT_VALUE:
                    processNewSelect(ast.getFirstChild());
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a SelectValue.
     *
     * @param ast an AST node to process
     */
    public void processNewSelect(AST ast) {
        AbstractValue v = null;
        String alias = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.FIELD_ALIAS:
                    alias = ast.getFirstChild().getText();
                    break;
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.TYPECAST:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    v = processNewAbstractValue(ast);
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        SelectValue sv = new SelectValue(v, alias);
        addSelect(sv);
    }

    /**
     * Processes an AST node that describes a GROUP clause.
     *
     * @param ast an AST node to process
     */
    public void processGroupClause(AST ast) {
        do {
            addGroupBy(processNewAbstractValue(ast));
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a ORDER clause.
     *
     * @param ast an AST node to process
     */
    public void processOrderClause(AST ast) {
        do {
            addOrderBy(processNewAbstractValue(ast));
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes a single AST node that describes an AbstractValue.
     *
     * @param ast as AST node to process
     * @return an AbstractValue object corresponding to the input
     */
    public AbstractValue processNewAbstractValue(AST ast) {
        switch (ast.getType()) {
            case SqlTokenTypes.FIELD:
                return processNewField(ast.getFirstChild());
            case SqlTokenTypes.TYPECAST:
                return processNewTypecast(ast.getFirstChild());
            case SqlTokenTypes.CONSTANT:
                return new Constant(ast.getFirstChild().getText());
            case SqlTokenTypes.UNSAFE_FUNCTION:
                return processNewUnsafeFunction(ast.getFirstChild());
            case SqlTokenTypes.SAFE_FUNCTION:
                return processNewSafeFunction(ast.getFirstChild());
            default:
                throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]"));
        }
    }

    /**
     * Processes an AST node that describes a Field.
     *
     * @param ast an AST node to process
     * @return a Field object corresponding to the input
     */
    public Field processNewField(AST ast) {
        String table = null;
        String field = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.TABLE_ALIAS:
                    table = ast.getFirstChild().getText();
                    break;
                case SqlTokenTypes.FIELD_NAME:
                    field = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        AbstractTable t = null;
        if (table == null) {
            t = onlyTable;
        } else {
            t = (AbstractTable) aliasToTable.get(table);
        }
        return new Field(field, t);
    }

    /**
     * Processes an AST node that describes a typecast.
     *
     * @param ast an AST node to process
     * @return a Function object corresponding to the input
     */
    public Function processNewTypecast(AST ast) {
        AbstractValue obj = null;
        Function retval = null;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.TYPECAST:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    if (obj != null) {
                        throw new IllegalArgumentException("Already have value in typecast "
                                + ast.getText() + " [" + ast.getType() + "]");
                    }
                    obj = processNewAbstractValue(ast);
                    break;
                case SqlTokenTypes.LITERAL_boolean:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("boolean"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_real:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("real"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_double:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("double precision"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_smallint:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("smallint"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_integer:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("integer"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_bigint:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("bigint"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_numeric:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("numeric"));
                    obj = retval;
                    break;
                case SqlTokenTypes.LITERAL_text:
                    retval = new Function(Function.TYPECAST);
                    retval.add(obj);
                    retval.add(new Constant("text"));
                    obj = retval;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return retval;
    }

    /**
     * Processes an AST node that describes an unsafe function.
     *
     * @param ast an AST node to process
     * @return a Function object corresponding to the input
     */
    public Function processNewUnsafeFunction(AST ast) {
        AbstractValue firstObj = null;
        Function retval = null;
        boolean gotType = false;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.TYPECAST:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    if (!gotType) {
                        firstObj = processNewAbstractValue(ast);
                    } else {
                        retval.add(processNewAbstractValue(ast));
                    }
                    break;
                case SqlTokenTypes.PLUS:
                    if (!gotType) {
                        retval = new Function(Function.PLUS);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.MINUS:
                    if (!gotType) {
                        retval = new Function(Function.MINUS);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.ASTERISK:
                    if (!gotType) {
                        retval = new Function(Function.MULTIPLY);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.DIVIDE:
                    if (!gotType) {
                        retval = new Function(Function.DIVIDE);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.POWER:
                    if (!gotType) {
                        retval = new Function(Function.POWER);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.PERCENT:
                    if (!gotType) {
                        retval = new Function(Function.MODULO);
                        retval.add(firstObj);
                        gotType = true;
                    }
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return retval;
    }

    /**
     * Processes an AST node that describes a safe function.
     *
     * @param ast an AST node to process
     * @return a Function object corresponding to the input
     */
    public Function processNewSafeFunction(AST ast) {
        Function retval = null;
        boolean gotType = false;
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.FIELD:
                case SqlTokenTypes.TYPECAST:
                case SqlTokenTypes.CONSTANT:
                case SqlTokenTypes.UNSAFE_FUNCTION:
                case SqlTokenTypes.SAFE_FUNCTION:
                    retval.add(processNewAbstractValue(ast));
                    break;
                case SqlTokenTypes.LITERAL_count:
                    if (!gotType) {
                        retval = new Function(Function.COUNT);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_max:
                    if (!gotType) {
                        retval = new Function(Function.MAX);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_min:
                    if (!gotType) {
                        retval = new Function(Function.MIN);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_sum:
                    if (!gotType) {
                        retval = new Function(Function.SUM);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_avg:
                    if (!gotType) {
                        retval = new Function(Function.AVG);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_strpos:
                    if (!gotType) {
                        retval = new Function(Function.STRPOS);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_lower:
                    if (!gotType) {
                        retval = new Function(Function.LOWER);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_upper:
                    if (!gotType) {
                        retval = new Function(Function.UPPER);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_substr:
                    if (!gotType) {
                        retval = new Function(Function.SUBSTR);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_coalesce:
                    if (!gotType) {
                        retval = new Function(Function.COALESCE);
                        gotType = true;
                    }
                    break;
                case SqlTokenTypes.LITERAL_stddev:
                    if (!gotType) {
                        retval = new Function(Function.STDDEV);
                        gotType = true;
                    }
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return retval;
    }

    /**
     * Processes an AST node that describes a where condition.
     *
     * @param ast an AST node to process
     */
    private void processWhereClause(AST ast) {
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.AND_CONSTRAINT_SET:
                    // Recurse - it's an AND set, equivalent to a WHERE_CLAUSE
                    processWhereClause(ast.getFirstChild());
                    break;
                case SqlTokenTypes.OR_CONSTRAINT_SET:
                case SqlTokenTypes.CONSTRAINT:
                case SqlTokenTypes.NOT_CONSTRAINT:
                case SqlTokenTypes.SUBQUERY_CONSTRAINT:
                case SqlTokenTypes.INLIST_CONSTRAINT:
                case SqlTokenTypes.NULL_CONSTRAINT:
                    addWhere(processNewAbstractConstraint(ast));
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a HAVING condition.
     *
     * @param ast an AST node to process
     */
    private void processHavingClause(AST ast) {
        do {
            switch (ast.getType()) {
                case SqlTokenTypes.AND_CONSTRAINT_SET:
                    // Recurse - it's an AND set, equivalent to a HAVING_CLAUSE
                    processHavingClause(ast.getFirstChild());
                    break;
                case SqlTokenTypes.OR_CONSTRAINT_SET:
                case SqlTokenTypes.CONSTRAINT:
                case SqlTokenTypes.NOT_CONSTRAINT:
                case SqlTokenTypes.SUBQUERY_CONSTRAINT:
                case SqlTokenTypes.INLIST_CONSTRAINT:
                case SqlTokenTypes.NULL_CONSTRAINT:
                    addHaving(processNewAbstractConstraint(ast));
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes an AbstractConstraint.
     *
     * @param ast an AST node to process
     * @return an AbstractConstraint object corresponding to the input
     */
    public AbstractConstraint processNewAbstractConstraint(AST ast) {
        AST subAST;
        switch (ast.getType()) {
            case SqlTokenTypes.CONSTRAINT:
                subAST = ast.getFirstChild();
                AbstractValue left = processNewAbstractValue(subAST);
                subAST = subAST.getNextSibling();
                int op = 0;
                switch (subAST.getType()) {
                    case SqlTokenTypes.LT:
                        op = Constraint.LT;
                        break;
                    case SqlTokenTypes.EQ:
                        op = Constraint.EQ;
                        break;
                    case SqlTokenTypes.LITERAL_like:
                        op = Constraint.LIKE;
                        break;
                    case SqlTokenTypes.GORNULL:
                        op = Constraint.GORNULL;
                        break;
                    default:
                        throw (new IllegalArgumentException("Unknown AST node: " + subAST.getText()
                                    + " [" + subAST.getType() + "]"));
                }
                subAST = subAST.getNextSibling();
                AbstractValue right = processNewAbstractValue(subAST);
                return new Constraint(left, op, right);
            case SqlTokenTypes.NULL_CONSTRAINT:
                subAST = ast.getFirstChild();
                AbstractValue bleft = processNewAbstractValue(subAST);
                return new Constraint(bleft, Constraint.EQ, new Constant("null"));
            case SqlTokenTypes.NOT_CONSTRAINT:
                subAST = ast.getFirstChild();
                AbstractConstraint a = processNewAbstractConstraint(subAST);
                return new NotConstraint(a);
            case SqlTokenTypes.OR_CONSTRAINT_SET:
                ConstraintSet b = new ConstraintSet();
                subAST = ast.getFirstChild();
                do {
                    b.add(processNewAbstractConstraint(subAST));
                    subAST = subAST.getNextSibling();
                } while (subAST != null);
                return b;
            case SqlTokenTypes.SUBQUERY_CONSTRAINT:
                subAST = ast.getFirstChild();
                AbstractValue leftb = processNewAbstractValue(subAST);
                subAST = subAST.getNextSibling();

                if (subAST.getType() != SqlTokenTypes.SQL_STATEMENT) {
                    throw (new IllegalArgumentException("Expected: a SQL SELECT statement"));
                }
                Query rightb = new Query(aliasToTable);
                rightb.processSqlStatementAST(subAST);
                return new SubQueryConstraint(leftb, rightb);
            case SqlTokenTypes.INLIST_CONSTRAINT:
                subAST = ast.getFirstChild();
                AbstractValue leftc = processNewAbstractValue(subAST);
                subAST = subAST.getNextSibling();
                InListConstraint retval = new InListConstraint(leftc);
                while (subAST != null) {
                    AbstractValue rightc = processNewAbstractValue(subAST);
                    retval.add((Constant) rightc);
                    subAST = subAST.getNextSibling();
                }
                return retval;
            default:
                throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]"));
        }
    }

    /**
     * Processes an AST node that describes a Limit clause.
     *
     * @param ast an AST node to process
     */
    public void processLimitClause(AST ast) {
        limit = Integer.parseInt(ast.getText());
        if (ast.getNextSibling() != null) {
            offset = Integer.parseInt(ast.getNextSibling().getText());
        }
    }

    /**
     * A testing method - converts the argument into a Query object, and then converts it back to
     * a String again.
     *
     * @param args command-line arguments
     * @throws Exception anytime
     */
    public static void main(String args[]) throws Exception {
        java.util.Date startTime = new java.util.Date();
        PrintStream out = System.out;

        InputStream is = new ByteArrayInputStream(args[0].getBytes());
        SqlLexer lexer = new SqlLexer(is);
        SqlParser parser = new SqlParser(lexer);
        parser.start_rule();
        AST ast = parser.getAST();

        antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();

        int iters = 0;
        AST oldAst;
        do {
            out.println("\nTime taken so far: " + ((new java.util.Date()).getTime()
                        - startTime.getTime()) + " milliseconds.");
            out.println("\n==> Dump of AST <==");
            visitor.visit(ast);

            //ASTFrame frame = new ASTFrame("AST JTree Example", ast);
            //frame.setVisible(true);

            oldAst = ast;
            SqlTreeParser treeparser = new SqlTreeParser();
            treeparser.start_rule(ast);
            ast = treeparser.getAST();
            iters++;
        } while (!oldAst.equalsList(ast));

        if (ast.getType() != SqlTokenTypes.SQL_STATEMENT) {
            throw (new IllegalArgumentException("Expected: a SQL SELECT statement"));
        }
        Query q = new Query(new HashMap());
        q.processSqlStatementAST(ast);

        out.println("\n" + q.getSQLString());
        out.println("\nTime taken so far: " + ((new java.util.Date()).getTime()
                    - startTime.getTime()) + " milliseconds.");

        out.println("Iterations required: " + iters);
    }
}
