package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;

import antlr.collections.AST;



import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.objectstore.ojb.PersistenceBrokerFlyMine;
import org.flymine.objectstore.ojb.FlyMineSqlSelectStatement;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.gnu.readline.ReadlineCompleter;
import java.io.File;

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
    private String modelPackage = null;

    private int alias = 1;

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
        alias(cls);
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
     * Gets the ORDER BY clause of this Query
     *
     * @return the List of ORDER BY nodes
     */
    public List getOrderBy() {
        return Collections.unmodifiableList(orderBy);
    }

    /**
     * Add a QueryNode to the SELECT clause of this Query
     *
     * @param node the QueryNode to add
     * @return the updated Query
     */
    public Query addToSelect(QueryNode node) {
        select.add(node);
        alias(node);
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
        String alias = (String) aliases.remove(node);
        if (alias != null) {
            reverseAliases.remove(alias);
        }
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
     * Returns a string representation of this Query object
     *
     * @return a String representation
     */
    public String toString() {
        boolean needComma = false;
        String retval = "SELECT ";
        Iterator selectIter = select.iterator();
        while (selectIter.hasNext()) {
            QueryNode qn = (QueryNode) selectIter.next();
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            String nodeAlias = (String) aliases.get(qn);
            if (qn instanceof QueryClass) {
                retval += nodeToString(qn);
            } else {
                retval += nodeToString(qn) + (nodeAlias == null ? "" : " AS " + nodeAlias);
            }
        }
        needComma = false;
        retval += " FROM ";
        Iterator qcIter = queryClasses.iterator();
        while (qcIter.hasNext()) {
            FromElement fe = (FromElement) qcIter.next();
            String classAlias = (String) aliases.get(fe);
            if (needComma) {
                retval += ", ";
            }
            needComma = true;
            if (fe instanceof QueryClass) {
                retval += fe.toString() + (classAlias == null ? "" : " AS " + classAlias);
            } else {
                retval += "(" + fe.toString() + ")" + (classAlias == null ? "" : " AS "
                        + classAlias);
            }
        }
        retval += (constraint == null ? "" : " WHERE " + constraintToString(constraint));
        if (!groupBy.isEmpty()) {
            retval += " GROUP BY ";
            Iterator groupIter = groupBy.iterator();
            needComma = false;
            while (groupIter.hasNext()) {
                QueryNode qn = (QueryNode) groupIter.next();
                if (needComma) {
                    retval += ", ";
                }
                needComma = true;
                retval += nodeToString(qn);
            }
        }
        if (!orderBy.isEmpty()) {
            retval += " ORDER BY ";
            Iterator orderIter = orderBy.iterator();
            needComma = false;
            while (orderIter.hasNext()) {
                QueryNode qn = (QueryNode) orderIter.next();
                if (needComma) {
                    retval += ", ";
                }
                needComma = true;
                retval += nodeToString(qn);
            }
        }
        return retval;
    }

    private String nodeToString(QueryNode qn) {
        String nodeAlias = (String) aliases.get(qn);
        if (qn instanceof QueryClass) {
            return nodeAlias;
        } else if (qn instanceof QueryField) {
            return aliases.get(((QueryField) qn).getFromElement()) + "."
                + ((QueryField) qn).getFieldName();
        } else if (qn instanceof QueryValue) {
            return ((QueryValue) qn).getValue().toString();
        } else if (qn instanceof QueryExpression) {
            QueryExpression qe = (QueryExpression) qn;
            if (qe.getOperation() == QueryExpression.SUBSTRING) {
                return "SUBSTR(" + nodeToString(qe.getArg1()) + ", " + nodeToString(qe.getArg2())
                    + ", " + nodeToString(qe.getArg3()) + ")";
            } else {
                String retval = nodeToString(qe.getArg1());
                switch (qe.getOperation()) {
                    case QueryExpression.ADD:
                        retval += " + ";
                        break;
                    case QueryExpression.SUBTRACT:
                        retval += " - ";
                        break;
                    case QueryExpression.MULTIPLY:
                        retval += " * ";
                        break;
                    case QueryExpression.DIVIDE:
                        retval += " / ";
                        break;
                    default:
                        throw (new IllegalArgumentException("Invalid QueryExpression operation: "
                                    + qe.getOperation()));
                }
                retval += nodeToString(qe.getArg2());
                return retval;
            }
        } else if (qn instanceof QueryFunction) {
            QueryFunction qf = (QueryFunction) qn;
            if (qf.getOperation() == QueryFunction.COUNT) {
                return "COUNT(*)";
            } else {
                String retval = null;
                switch (qf.getOperation()) {
                    case QueryFunction.SUM:
                        retval = "SUM(";
                        break;
                    case QueryFunction.AVERAGE:
                        retval = "AVG(";
                        break;
                    case QueryFunction.MIN:
                        retval = "MIN(";
                        break;
                    case QueryFunction.MAX:
                        retval = "MAX(";
                        break;
                    default:
                        throw (new IllegalArgumentException("Invalid QueryFunction operation: "
                                    + qf.getOperation()));
                }
                retval += nodeToString(qf.getParam()) + ")";
                return retval;
            }
        } else {
            return qn.toString();
        }
    }

    private String constraintToString(Constraint cc) {
        if (cc instanceof SimpleConstraint) {
            SimpleConstraint c = (SimpleConstraint) cc;
            if ((c.getType() == SimpleConstraint.IS_NULL)
                    || (c.getType() == SimpleConstraint.IS_NOT_NULL)) {
                return nodeToString(c.getArg1()) + c.getOpString();
            } else {
                return nodeToString(c.getArg1()) + c.getOpString() + nodeToString(c.getArg2());
            }
        } else if (cc instanceof SubqueryConstraint) {
            SubqueryConstraint c = (SubqueryConstraint) cc;
            return (c.getQueryEvaluable() == null ? nodeToString(c.getQueryClass())
                    : nodeToString(c.getQueryEvaluable())) + (c.isNotIn() ? " NOT IN (" : " IN (")
                    + c.getQuery().toString() + ")";
        } else if (cc instanceof ClassConstraint) {
            ClassConstraint c = (ClassConstraint) cc;
            return nodeToString(c.getArg1()) + (c.isNotEqual() ? " != " : " = ")
                + (c.getArg2QueryClass() == null ? "<example object: "
                        + c.getArg2Object().toString() + ">" : nodeToString(c.getArg2QueryClass()));
        } else if (cc instanceof ContainsConstraint) {
            ContainsConstraint c = (ContainsConstraint) cc;
            QueryReference ref = c.getReference();
            return aliases.get(ref.getQueryClass()) + "." + ref.getFieldName()
                + (c.isNotContains() ? " DOES NOT CONTAIN " : " CONTAINS ")
                + aliases.get(c.getQueryClass());
        } else if (cc instanceof ConstraintSet) {
            ConstraintSet c = (ConstraintSet) cc;
            boolean needComma = false;
            String retval = (c.isNegated() ? "( NOT (" : "(");
            Iterator conIter = c.getConstraints().iterator();
            while (conIter.hasNext()) {
                Constraint subC = (Constraint) conIter.next();
                if (needComma) {
                    retval += (c.getDisjunctive() ? " OR " : " AND ");
                }
                needComma = true;
                retval += constraintToString(subC);
            }
            return retval + (c.isNegated() ? "))" : ")");
        } else {
            throw new IllegalArgumentException("Unknown constraint type: " + cc);
        }
    }

    private void alias(Object obj) {
        if (!aliases.containsKey(obj)) {
            String aliasString = "a" + (alias++) + "_";
            aliases.put(obj, aliasString);
            reverseAliases.put(aliasString, obj);
        }
    }

    /**
     * Overrides Object.equals()
     *
     * @param obj and Object to compare to
     * @return true if object is equivalent
     */
    public boolean equals(Object obj) {
        if (obj instanceof Query) {
            Query q = (Query) obj;
            return (distinct == q.distinct) && select.equals(q.select)
                && queryClasses.equals(q.queryClasses)
                && (constraint != null) ? (constraint.equals(q.constraint)) : (q.constraint == null)
                && groupBy.equals(q.groupBy) && orderBy.equals(q.orderBy)
                && aliases.equals(q.aliases);
        }
        return false;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer created from the contents of the Query
     */
    public int hashCode() {
        return (distinct ? 29 : 0) + (5 * select.hashCode())
            + (7 * queryClasses.hashCode())
            + ((constraint != null) ? constraint.hashCode() : 31)
            + (13 * groupBy.hashCode()) + (15 * orderBy.hashCode())
            + (17 * aliases.hashCode());
    }

    /**
     * A testing method - converts the argument into a Query object, and then converts it back to
     * a String again.
     *
     * @param args command-line arguments
     * @throws Exception anytime
     */
    public static void main(String args[]) throws Exception {
        PrintStream out = System.out;
        if (args.length > 1) {
            out.println("Usage: java org.flymine.objectstore.query.Query - to enter shell-mode");
            out.println("       java org.flymine.objectstore.query.Query \"<FQL Query>\" - to run");
            out.println("                      a one-off query");
        } else {
            try {
                //Properties props = new Properties();
                //props.load(new FileInputStream("/home/mnw21/flymine.properties"));
                //System.setProperties(props);

                ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
                PersistenceBroker pb = ((ObjectStoreOjbImpl) os).getPersistenceBroker();
                DescriptorRepository dr = ((PersistenceBrokerFlyMine) pb).getDescriptorRepository();
                if (args.length == 1) {
                    runQuery(args[0], dr, os);
                } else {
                    doShell(dr, os);
                }
            } catch (Exception e) {
                out.println("Exception caught: " + e);
                e.printStackTrace(out);
            }
        }
    }

    private static void doShell(DescriptorRepository dr, ObjectStore os) throws Exception {
        PrintStream out = System.out;
        try {
            Readline.load(ReadlineLibrary.GnuReadline);
        } catch (UnsatisfiedLinkError ignore_me) {
            out.println("couldn't load readline lib. Using simple stdin.");
        }
        Readline.initReadline("FQLShell");
        try {
            Readline.readHistoryFile(System.getProperty("user.home") + File.separator
                    + ".fqlshell_history");
        } catch (RuntimeException e) {
            // Doesn't matter.
        }
        Readline.setCompleter(new ReadlineCompleter() {
            public String completer(String text, int state) {
                return null;
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Readline.writeHistoryFile(System.getProperty("user.home") + File.separator
                        + ".fqlshell_history");
                } catch (Exception e) {
                    // Don't mind
                }
                PrintStream out = System.out;
                out.println("\n");
                Readline.cleanup();
            }
        });

        out.println("\nFlyMine Query shell. Type in an FQL query, or \"quit;\" to exit.");
        out.println("End your query with \";\" then a newline. Other newlines are ignored");
        out.flush();
        String currentQuery = "";
        String lastQuery = Readline.getHistoryLine(Readline.getHistorySize() - 1);
        //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        do {
            //currentQuery += in.readLine();
            String line = Readline.readline(currentQuery.equals("") ? "> " : "", false);
            currentQuery += (line == null ? "" : line);
            if (!(currentQuery.equals("") || currentQuery.equals("quit;"))) {
                if (currentQuery.endsWith(";")) {
                    if (!currentQuery.equals(lastQuery)) {
                        Readline.addToHistory(currentQuery);
                        lastQuery = currentQuery;
                    }
                    currentQuery = currentQuery.substring(0, currentQuery.length() - 1);
                    try {
                        runQuery(currentQuery, dr, os);
                    } catch (Exception e) {
                        e.printStackTrace(out);
                    }
                    currentQuery = "";
                } else {
                    currentQuery += "\n";
                }
            }
        } while (!"quit;".equals(currentQuery));
    }

    private static void runQuery(String fql, DescriptorRepository dr, ObjectStore os)
            throws Exception {
        java.util.Date startTime = new java.util.Date();
        PrintStream out = System.out;

        InputStream is = new ByteArrayInputStream(fql.getBytes());
        FqlLexer lexer = new FqlLexer(is);
        FqlParser parser = new FqlParser(lexer);
        parser.start_rule();
        AST ast = parser.getAST();

        antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();

        //out.println("\n==> Dump of AST <==");
        //visitor.visit(ast);

        //ASTFrame frame = new ASTFrame("AST JTree Example", ast);
        //frame.setVisible(true);

        Query q = new Query();
        q.modelPackage = "org.flymine.model.testmodel";
        q.processFqlStatementAST(ast);

        out.println("\nQuery: " + q.toString());
        //out.println("\nTime taken so far: " + ((new java.util.Date()).getTime()
        //            - startTime.getTime()) + " milliseconds.");

        FlyMineSqlSelectStatement s1 = new FlyMineSqlSelectStatement(q, dr);
        out.println("\nSQL query: " + s1.getStatement());

        Results res = os.execute(q);
        out.print("Column headings: ");
        outputList(res.getColumnAliases());
        out.print("Column types: ");
        outputList(res.getColumnTypes());
        Iterator rowIter = res.iterator();
        while (rowIter.hasNext()) {
            outputList((List) (rowIter.next()));
        }
    }

    private static void outputList(List l) {
        PrintStream out = System.out;
        boolean needComma = false;
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (needComma) {
                out.print(", ");
            }
            needComma = true;
            out.print(o.toString());
        }
        out.println("");
    }

    /**
     * Construct a new query by parsing a String.
     *
     * @param fql an FQL SELECT String to parse
     * @param modelPackage the base package for object classes
     * @throws IllegalArgumentException if the FQL String is invalid
     */
    public Query(String fql, String modelPackage) {
        this();
        this.modelPackage = modelPackage;

        try {
            InputStream is = new ByteArrayInputStream(fql.getBytes());

            FqlLexer lexer = new FqlLexer(is);
            FqlParser parser = new FqlParser(lexer);
            parser.start_rule();

            AST ast = parser.getAST();

            if (ast == null) {
                throw new IllegalArgumentException("Invalid FQL string " + fql);
            }

            processFqlStatementAST(ast);
        } catch (antlr.RecognitionException e) {
            throw new IllegalArgumentException("Exception: " + e);
        } catch (antlr.TokenStreamException e) {
            throw new IllegalArgumentException("Exception: " + e);
        }
    }

    /**
     * Processes an FQL_STATEMENT AST node produced by antlr.
     *
     * @param ast an AST node to process
     */
    private void processFqlStatementAST(AST ast) {
        if (ast.getType() != FqlTokenTypes.FQL_STATEMENT) {
            throw new IllegalArgumentException("Expected: an FQL SELECT statement");
        }
        processAST(ast.getFirstChild());
    }

    /**
     * Processes an AST node produced by antlr, at the top level of the FQL query.
     *
     * @param ast an AST node to process
     */
    private void processAST(AST ast) {
        boolean processSelect = false;
        switch (ast.getType()) {
            case FqlTokenTypes.SELECT_LIST:
                // Always do the select list last.
                processSelect = true;
                break;
            case FqlTokenTypes.FROM_LIST:
                processFromList(ast.getFirstChild());
                break;
            case FqlTokenTypes.WHERE_CLAUSE:
                constraint = processConstraint(ast.getFirstChild());
                break;
            case FqlTokenTypes.GROUP_CLAUSE:
                processGroupClause(ast.getFirstChild());
                break;
            case FqlTokenTypes.ORDER_CLAUSE:
                processOrderClause(ast.getFirstChild());
                break;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                        + ast.getType() + "]");
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
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.TABLE:
                    processNewTable(ast.getFirstChild());
                    break;
                case FqlTokenTypes.SUBQUERY:
                    processNewSubQuery(ast.getFirstChild());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
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
                case FqlTokenTypes.TABLE_NAME:
                    tableName = ast.getFirstChild().getText();
                    break;
                case FqlTokenTypes.TABLE_ALIAS:
                    tableAlias = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        Class c = null;
        try {
            c = Class.forName(modelPackage + "." + tableName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class name " + tableName + " in package "
                    + modelPackage);
        }
        QueryClass qc = new QueryClass(c);
        queryClasses.add(qc);
        if (tableAlias == null) {
            tableAlias = tableName;
        }
        if (reverseAliases.containsKey(tableAlias)) {
            throw new IllegalArgumentException("Alias " + tableAlias + " is used more than once.");
        }
        aliases.put(qc, tableAlias);
        reverseAliases.put(tableAlias, qc);
    }

    /**
     * Processes an AST node that describes a subquery in the FROM list.
     *
     * @param ast an AST node to process
     */
    private void processNewSubQuery(AST ast) {
        AST subquery = null;
        String tableAlias = null;
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.FQL_STATEMENT:
                    if (subquery == null) {
                        subquery = ast;
                    }
                    break;
                case FqlTokenTypes.TABLE_ALIAS:
                    tableAlias = ast.getFirstChild().getText();
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        Query q = new Query();
        q.modelPackage = modelPackage;
        q.processFqlStatementAST(subquery);
        if (tableAlias == null) {
            throw new IllegalArgumentException("No alias for subquery");
        }
        if (reverseAliases.containsKey(tableAlias)) {
            throw new IllegalArgumentException("Alias " + tableAlias + " is used more than once.");
        }
        aliases.put(q, tableAlias);
        reverseAliases.put(tableAlias, q);
        queryClasses.add(q);
    }

    /**
     * Processes an AST node that describes a SELECT list.
     *
     * @param ast an AST node to process
     */
    private void processSelectList(AST ast) {
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.SELECT_VALUE:
                    processNewSelect(ast.getFirstChild());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a Select value.
     *
     * @param ast an AST node to process
     */
    private void processNewSelect(AST ast) {
        QueryNode node = null;
        String nodeAlias = null;
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.FIELD_ALIAS:
                    nodeAlias = ast.getFirstChild().getText();
                    break;
                case FqlTokenTypes.FIELD:
                case FqlTokenTypes.CONSTANT:
                case FqlTokenTypes.UNSAFE_FUNCTION:
                case FqlTokenTypes.SAFE_FUNCTION:
                    node = processNewQueryNode(ast);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        if ((nodeAlias == null) != (node instanceof QueryClass)) {
            throw new IllegalArgumentException("No alias for item in SELECT list, or an alias "
                    + "present for a QueryClass");
        }
        select.add(node);
        if (!(node instanceof QueryClass)) {
            aliases.put(node, nodeAlias);
            reverseAliases.put(nodeAlias, node);
        }
    }

    /**
     * Processes an AST node that describes a QueryNode.
     *
     * @param ast an AST node to process
     * @return a QueryNode object corresponding to the input
     */
    private QueryNode processNewQueryNode(AST ast) {
        switch (ast.getType()) {
            case FqlTokenTypes.FIELD:
                return processNewField(ast.getFirstChild());
            case FqlTokenTypes.CONSTANT:
                String value = ast.getFirstChild().getText();
                try {
                    return new QueryValue(Long.valueOf(value));
                } catch (NumberFormatException e) {
                    // No problem - not a representable integer
                }
                try {
                    return new QueryValue(Double.valueOf(value));
                } catch (NumberFormatException e) {
                    // No problem - not a representable number
                }

                if ("true".equals(value)) {
                    return new QueryValue(Boolean.TRUE);
                } else if ("false".equals(value)) {
                    return new QueryValue(Boolean.FALSE);
                } else if ((value.charAt(0) == '\'')
                        && (value.charAt(value.length() - 1) == '\'')) {
                    String innerValue = value.substring(1, value.length() - 1);
                    try {
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        return new QueryValue(format.parse(innerValue));
                    } catch (ParseException e) {
                        // No problem - not a parsable date
                    }
                    return new QueryValue(innerValue);
                } else {
                    throw new IllegalArgumentException("Unparsable constant \"" + value + "\"");
                }
            case FqlTokenTypes.UNSAFE_FUNCTION:
                return processNewUnsafeFunction(ast.getFirstChild());
            case FqlTokenTypes.SAFE_FUNCTION:
                return processNewSafeFunction(ast.getFirstChild());
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
        }
    }

    /**
     * Processes an AST node that describes a QueryField or QueryClass in the SELECT list.
     * There are several possible arrangements:
     * 1. a     where a is a QueryClass.
     * 2. a.b   where a is a QueryClass, and b is a QueryField.
     * 3. a.b   where a is a Query, and b is a QueryEvaluable.
     * 4. a.b.c where a is a Query, b is a QueryClass, and c is a QueryField.
     *
     * @param ast an AST node to process
     * @return a QueryNode object corresponding to the input
     */
    private QueryNode processNewField(AST ast) {
        if (ast.getType() != FqlTokenTypes.IDENTIFIER) {
            throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                        + ast.getType() + "]");
        }
        Object obj = reverseAliases.get(ast.getText());

        if (obj instanceof QueryClass) {
            AST secondAst = ast.getNextSibling();
            if (secondAst == null) {
                return (QueryClass) obj;
            } else {
                AST thirdAst = secondAst.getNextSibling();
                if (thirdAst == null) {
                    try {
                        return new QueryField((QueryClass) obj, secondAst.getText());
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException(e.toString());
                    }
                } else {
                    throw new IllegalArgumentException("Path expression " + ast.getText() + "."
                            + secondAst.getText() + "." + thirdAst.getText() + " extends beyond a "
                            + "field");
                }
            }
        } else if (obj instanceof Query) {
            AST secondAst = ast.getNextSibling();
            Query q = (Query) obj;
            if (secondAst == null) {
                throw new IllegalArgumentException("Path expression " + ast.getText()
                        + " cannot end at a subquery");
            } else {
                AST thirdAst = secondAst.getNextSibling();
                Object secondObj = q.reverseAliases.get(secondAst.getText());
                if (secondObj instanceof QueryClass) {
                    if (thirdAst == null) {
                        throw new IllegalArgumentException("Cannot reference classes inside "
                                + "subqueries - only QueryEvaluables, and fields inside classes "
                                + "inside subqueries, for path expression " + ast.getText() + "."
                                + secondAst.getText());
                    } else {
                        AST fourthAst = thirdAst.getNextSibling();
                        if (fourthAst == null) {
                            if (q.select.contains(secondObj)) {
                                try {
                                    return new QueryField(q, (QueryClass) secondObj,
                                            thirdAst.getText());
                                } catch (NoSuchFieldException e) {
                                    throw new IllegalArgumentException(e.toString());
                                }
                            } else {
                                throw new IllegalArgumentException(ast.getText() + "."
                                        + secondAst.getText() + "." + thirdAst.getText()
                                        + " is not available, because " + secondAst.getText()
                                        + " is not in the SELECT list of subquery "
                                        + ast.getText());
                            }
                        } else {
                            throw new IllegalArgumentException("Path expression " + ast.getText()
                                    + "." + secondAst.getText() + "." + thirdAst.getText() + "."
                                    + fourthAst.getText() + " extends beyond a field");
                        }
                    }
                } else if (secondObj instanceof QueryEvaluable) {
                    if (thirdAst == null) {
                        return new QueryField(q, (QueryEvaluable) secondObj);
                    } else {
                        throw new IllegalArgumentException("Path expression " + ast.getText() + "."
                                + secondAst.getText() + "." + thirdAst.getText() + " extends "
                                + "beyond a field");
                    }
                } else if (secondObj instanceof Query) {
                    throw new IllegalArgumentException("Cannot reference subquery "
                            + secondAst.getText() + " inside subquery " + ast.getText());
                } else {
                    throw new IllegalArgumentException("No such object " + secondAst.getText()
                            + " found in subquery " + ast.getText());
                }
            }
        } else {
            throw new IllegalArgumentException("No such object " + ast.getText());
        }
    }

    /**
     * Processes an AST node that describes an unsafe function.
     *
     * @param ast an AST node to process
     * @return a QueryExpression object correcponding to the input
     */
    private QueryExpression processNewUnsafeFunction(AST ast) {
        QueryEvaluable firstObj = null;
        QueryEvaluable secondObj = null;
        int type = -1;
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.FIELD:
                case FqlTokenTypes.CONSTANT:
                case FqlTokenTypes.UNSAFE_FUNCTION:
                case FqlTokenTypes.SAFE_FUNCTION:
                    try {
                        if (firstObj == null) {
                            firstObj = (QueryEvaluable) processNewQueryNode(ast);
                        } else if (secondObj == null) {
                            secondObj = (QueryEvaluable) processNewQueryNode(ast);
                        } else {
                            throw new IllegalArgumentException("QueryExpressions can only have two "
                                    + "arguments");
                        }
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Expressions cannot contain classes as "
                                + "arguments");
                    }
                    break;
                case FqlTokenTypes.PLUS:
                    type = QueryExpression.ADD;
                    break;
                case FqlTokenTypes.MINUS:
                    type = QueryExpression.SUBTRACT;
                    break;
                case FqlTokenTypes.ASTERISK:
                    type = QueryExpression.MULTIPLY;
                    break;
                case FqlTokenTypes.DIVIDE:
                    type = QueryExpression.DIVIDE;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return new QueryExpression(firstObj, type, secondObj);
    }

    /**
     * Processes an AST node that describes a safe function.
     *
     * @param ast an AST node to process
     * @return a QueryEvaluable object corresponding to the input
     */
    private QueryEvaluable processNewSafeFunction(AST ast) {
        QueryEvaluable firstObj = null;
        QueryEvaluable secondObj = null;
        QueryEvaluable thirdObj = null;
        int type = -1;
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.FIELD:
                case FqlTokenTypes.CONSTANT:
                case FqlTokenTypes.UNSAFE_FUNCTION:
                case FqlTokenTypes.SAFE_FUNCTION:
                    try {
                        if (type == QueryFunction.COUNT) {
                            throw new IllegalArgumentException("Count() does not take an argument");
                        } else if (firstObj == null) {
                            firstObj = (QueryEvaluable) processNewQueryNode(ast);
                        } else if (type != -2) {
                            throw new IllegalArgumentException("Too many arguments for aggregate "
                                    + "function");
                        } else if (secondObj == null) {
                            secondObj = (QueryEvaluable) processNewQueryNode(ast);
                        } else if (thirdObj == null) {
                            thirdObj = (QueryEvaluable) processNewQueryNode(ast);
                        } else {
                            throw new IllegalArgumentException("Too many arguments in substring");
                        }
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Functions cannot contain classes as "
                                + "arguments");
                    }
                    break;
                case FqlTokenTypes.LITERAL_count:
                    type = QueryFunction.COUNT;
                    break;
                case FqlTokenTypes.LITERAL_sum:
                    type = QueryFunction.SUM;
                    break;
                case FqlTokenTypes.LITERAL_avg:
                    type = QueryFunction.AVERAGE;
                    break;
                case FqlTokenTypes.LITERAL_min:
                    type = QueryFunction.MIN;
                    break;
                case FqlTokenTypes.LITERAL_max:
                    type = QueryFunction.MAX;
                    break;
                case FqlTokenTypes.LITERAL_substr:
                    type = -2;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        if (type == -2) {
            if (thirdObj == null) {
                throw new IllegalArgumentException("Not enough arguments for substring function");
            } else {
                return new QueryExpression(firstObj, secondObj, thirdObj);
            }
        } else if (type == QueryFunction.COUNT) {
            return new QueryFunction();
        } else {
            if (firstObj == null) {
                throw new IllegalArgumentException("Need an argument for this function");
            } else {
                if (firstObj instanceof QueryField) {
                    return new QueryFunction((QueryField) firstObj, type);
                } else if (firstObj instanceof QueryExpression) {
                    return new QueryFunction((QueryExpression) firstObj, type);
                } else {
                    throw new IllegalArgumentException("Arguments to aggregate functions may "
                            + "be fields or expressions only");
                }
            }
        }
    }

    /**
     * Processes an AST node that describes a ORDER BY clause.
     *
     * @param ast an AST node to process
     */
    private void processOrderClause(AST ast) {
        do {
            orderBy.add(processNewQueryNode(ast));
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a GROUP BY clause.
     *
     * @param ast an AST node to process
     */
    private void processGroupClause(AST ast) {
        do {
            groupBy.add(processNewQueryNode(ast));
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a ConstraintSet.
     * This method will also recognise any other type of constraint, and treat it as a ConstraintSet
     * with only one entry. Such a ConstraintSet is output as the entry, rather than a real
     * ConstraintSet.
     *
     * @param ast an AST node to process
     * @return a Constraint corresponding to the input
     */
    private Constraint processConstraintSet(AST ast, boolean andOr) {
        Constraint retval = null;
        boolean isSet = false;
        do {
            Constraint temp = processConstraint(ast);
            if (retval == null) {
                retval = temp;
            } else if (!isSet) {
                Constraint temp2 = retval;
                retval = new ConstraintSet(andOr);
                ((ConstraintSet) retval).addConstraint(temp2);
                ((ConstraintSet) retval).addConstraint(temp);
                isSet = true;
            } else {
                ((ConstraintSet) retval).addConstraint(temp);
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        return retval;
    }

    /**
     * Processes an AST node that describes a Constraint.
     *
     * @param ast an AST node to process
     * @return a Constraint corresponding to the input
     */
    private Constraint processConstraint(AST ast) {
        AST subAST;
        switch (ast.getType()) {
            case FqlTokenTypes.AND_CONSTRAINT_SET:
                return processConstraintSet(ast.getFirstChild(), ConstraintSet.AND);
            case FqlTokenTypes.OR_CONSTRAINT_SET:
                return processConstraintSet(ast.getFirstChild(), ConstraintSet.OR);
            case FqlTokenTypes.CONSTRAINT:
                return processSimpleConstraint(ast);
            case FqlTokenTypes.SUBQUERY_CONSTRAINT:
                subAST = ast.getFirstChild();
                QueryNode leftb = processNewQueryNode(subAST);
                subAST = subAST.getNextSibling();
                if (subAST.getType() != FqlTokenTypes.FQL_STATEMENT) {
                    throw new IllegalArgumentException("Expected: a FQL SELECT statement");
                }
                Query rightb = new Query();
                rightb.modelPackage = modelPackage;
                rightb.processFqlStatementAST(subAST);
                if (leftb instanceof QueryClass) {
                    return new SubqueryConstraint(rightb, SubqueryConstraint.CONTAINS,
                            (QueryClass) leftb);
                } else {
                    return new SubqueryConstraint(rightb, SubqueryConstraint.CONTAINS,
                            (QueryEvaluable) leftb);
                }
            case FqlTokenTypes.CONTAINS_CONSTRAINT:
                subAST = ast.getFirstChild();
                if (subAST.getType() != FqlTokenTypes.FIELD) {
                    throw new IllegalArgumentException("Expected a Collection or Object Reference "
                            + "as the first argument of the ContainsConstraint");
                }
                QueryReference leftc = null;
                AST subSubAST = subAST.getFirstChild();
                String firstString = subSubAST.getText();
                FromElement firstObj = (FromElement) reverseAliases.get(firstString);
                if (firstObj instanceof QueryClass) {
                    subSubAST = subSubAST.getNextSibling();
                    if (subSubAST != null) {
                        String secondString = subSubAST.getText();
                        if (subSubAST.getNextSibling() != null) {
                            throw new IllegalArgumentException("Path expression " + firstString
                                    + "." + secondString + "."
                                    + subSubAST.getNextSibling().getText() + " extends beyond a "
                                    + "collection or object reference");
                        }
                        QueryReference ref = null;
                        try {
                            try {
                                // See if it is an object reference.
                                ref = new QueryObjectReference((QueryClass) firstObj, secondString);
                            } catch (IllegalArgumentException e) {
                                // Okay, it wasn't. See if it is a collection.
                                ref = new QueryCollectionReference((QueryClass) firstObj,
                                        secondString);
                            }
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Object " + firstString + "."
                                    + secondString + " is not a collection or object reference");
                        } catch (NoSuchFieldException e) {
                            throw new IllegalArgumentException("No such object " + firstString + "."
                                    + secondString);
                        }
                        // Now we have a collection or object reference. Now we need a class.
                        QueryNode qc = processNewQueryNode(subAST.getNextSibling());
                        if (qc instanceof QueryClass) {
                            return new ContainsConstraint(ref, ContainsConstraint.CONTAINS,
                                    (QueryClass) qc);
                        } else {
                            throw new IllegalArgumentException("Collection or object reference "
                                    + firstString + "." + secondString + " cannot contain "
                                    + "anything but a QueryClass");
                        }
                    } else {
                        throw new IllegalArgumentException("Path expression for collection cannot "
                                + "end on a QueryClass");
                    }
                } else if (firstObj instanceof Query) {
                    throw new IllegalArgumentException("Cannot access a collection or object "
                            + "reference inside subquery " + firstString);
                } else {
                    throw new IllegalArgumentException("No such object " + firstString + " while "
                            + "looking for a collection or object reference");
                }
            case FqlTokenTypes.NOT_CONSTRAINT:
                subAST = ast.getFirstChild();
                Constraint retval = processConstraint(subAST);
                retval.setNegated(!retval.isNegated());
                return retval;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
        }
    }

    private Constraint processSimpleConstraint(AST ast) {
        AST subAST = ast.getFirstChild();
        QueryNode left = processNewQueryNode(subAST);
        subAST = subAST.getNextSibling();
        int op = 0;
        switch (subAST.getType()) {
            case FqlTokenTypes.EQ:
                op = SimpleConstraint.EQUALS;
                break;
            case FqlTokenTypes.NOT_EQ:
                op = SimpleConstraint.NOT_EQUALS;
                break;
            case FqlTokenTypes.LT:
                op = SimpleConstraint.LESS_THAN;
                break;
            case FqlTokenTypes.LE:
                op = SimpleConstraint.LESS_THAN_EQUALS;
                break;
            case FqlTokenTypes.GT:
                op = SimpleConstraint.GREATER_THAN;
                break;
            case FqlTokenTypes.GE:
                op = SimpleConstraint.GREATER_THAN_EQUALS;
                break;
            case FqlTokenTypes.LITERAL_like:
                op = SimpleConstraint.MATCHES;
                break;
            case FqlTokenTypes.NOTLIKE:
                op = SimpleConstraint.DOES_NOT_MATCH;
                break;
            case FqlTokenTypes.ISNULL:
                op = SimpleConstraint.IS_NULL;
                break;
            case FqlTokenTypes.ISNOTNULL:
                op = SimpleConstraint.IS_NOT_NULL;
                break;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText()
                        + " [" + ast.getType() + "]");
        }
        subAST = subAST.getNextSibling();
        if ((op == SimpleConstraint.IS_NULL) || (op == SimpleConstraint.IS_NOT_NULL)) {
            if (subAST != null) {
                throw new IllegalArgumentException("IS (NOT) NULL only takes one argument");
            } else if (left instanceof QueryClass) {
                throw new IllegalArgumentException("Cannot compare a class to null");
            } else {
                return new SimpleConstraint(((QueryEvaluable) left), op);
            }
        } else {
            if (subAST == null) {
                throw new IllegalArgumentException("Most simple constraints require two "
                        + "arguments");
            } else {
                QueryNode right = processNewQueryNode(subAST);
                if (left instanceof QueryClass) {
                    if (right instanceof QueryClass) {
                        if (op == SimpleConstraint.EQUALS) {
                            return new ClassConstraint((QueryClass) left,
                                    ClassConstraint.EQUALS, (QueryClass) right);
                        } else if (op == SimpleConstraint.NOT_EQUALS) {
                            return new ClassConstraint((QueryClass) left,
                                    ClassConstraint.NOT_EQUALS, (QueryClass) right);
                        } else {
                            throw new IllegalArgumentException("Operation is not valid for "
                                    + "comparing two classes");
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot compare a class to a "
                                + "value");
                    }
                } else {
                    if (right instanceof QueryClass) {
                        throw new IllegalArgumentException("Cannot compare a value to a "
                                + "class");
                    } else {
                        return new SimpleConstraint((QueryEvaluable) left, op,
                                (QueryEvaluable) right);
                    }
                }
            }
        }
    }

}
