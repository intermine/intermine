package org.flymine.objectstore.query.fql;

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import antlr.collections.AST;

import org.flymine.objectstore.query.*;

/**
 * Parser for the FlyMine dialect of OQL (FQL)
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class FqlQueryParser
{
    /**
     * All methods are static - don't allow instance to be constructed
     */
    private FqlQueryParser() {
    }

    /**
     * Construct a new query by parsing a String.
     *
     * @param fq an FqlQuery object to parse
     * @return the Query representing the FqlQuery
     * @throws IllegalArgumentException if the OQLQuery contains an invalid query String
     */
    public static Query parse(FqlQuery fq) {
        Query q = new Query();
        String modelPackage = fq.getPackageName();
        String fql = fq.getQueryString();
        Iterator iterator = fq.getParameters().iterator();

        try {
            InputStream is = new ByteArrayInputStream(fql.getBytes());

            FqlLexer lexer = new FqlLexer(is);
            FqlParser parser = new FqlParser(lexer);
            parser.start_rule();

            AST ast = parser.getAST();

            //antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();
            //System.\\out.println("\n==> Dump of AST <==");
            //visitor.visit(ast);

            if (ast == null) {
                throw new IllegalArgumentException("Invalid FQL string " + fql);
            }

            processFqlStatementAST(ast, q, modelPackage, iterator);

            return q;
        } catch (antlr.RecognitionException e) {
            IllegalArgumentException e2 = new IllegalArgumentException("Exception");
            e2.initCause(e);
            throw e2;
        } catch (antlr.TokenStreamException e) {
            IllegalArgumentException e2 = new IllegalArgumentException("Exception");
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Processes an FQL_STATEMENT AST node produced by antlr.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the FqlQuery
     */
    private static void processFqlStatementAST(AST ast, Query q, String modelPackage,
                                               Iterator iterator) {
        if (ast.getType() != FqlTokenTypes.FQL_STATEMENT) {
            throw new IllegalArgumentException("Expected: an FQL SELECT statement");
        }
        processAST(ast.getFirstChild(), q, modelPackage, iterator);
    }

    /**
     * Processes an AST node produced by antlr, at the top level of the FQL query.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the FqlQuery
     */
    private static void processAST(AST ast, Query q, String modelPackage, Iterator iterator) {
        boolean processSelect = false;
        switch (ast.getType()) {
            case FqlTokenTypes.SELECT_LIST:
                // Always do the select list last.
                processSelect = true;
                break;
            case FqlTokenTypes.FROM_LIST:
                processFromList(ast.getFirstChild(), q, modelPackage, iterator);
                break;
            case FqlTokenTypes.WHERE_CLAUSE:
                q.setConstraint(processConstraint(ast.getFirstChild(), q, modelPackage, iterator));
                break;
            case FqlTokenTypes.GROUP_CLAUSE:
                processGroupClause(ast.getFirstChild(), q);
                break;
            case FqlTokenTypes.ORDER_CLAUSE:
                processOrderClause(ast.getFirstChild(), q);
                break;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                        + ast.getType() + "]");
        }
        if (ast.getNextSibling() != null) {
            processAST(ast.getNextSibling(), q, modelPackage, iterator);
        }
        if (processSelect) {
            processSelectList(ast.getFirstChild(), q, modelPackage);
        }
    }

    /**
     * Processes an AST node that describes a FROM list.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the FqlQuery
     */
    private static void processFromList(AST ast, Query q, String modelPackage, Iterator iterator) {
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.TABLE:
                    processNewTable(ast.getFirstChild(), q, modelPackage);
                    break;
                case FqlTokenTypes.SUBQUERY:
                    processNewSubQuery(ast.getFirstChild(), q, modelPackage, iterator);
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
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     */
    private static void processNewTable(AST ast, Query q, String modelPackage) {
        String tableName = null;
        String tableAlias = null;
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.TABLE_NAME:
                    AST tableNameAst = ast.getFirstChild();
                    do {
                        String temp = tableNameAst.getText();
                        tableName = (tableName == null ? temp : tableName + "." + temp);
                        tableNameAst = tableNameAst.getNextSibling();
                    } while (tableNameAst != null);
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
            c = Class.forName(tableName);
        } catch (ClassNotFoundException e) {
            if (modelPackage != null) {
                try {
                    c = Class.forName(modelPackage + "." + tableName);
                } catch (ClassNotFoundException e2) {
                    throw new IllegalArgumentException("Unknown class name " + tableName
                            + " in package " + modelPackage);
                }
            } else {
                throw new IllegalArgumentException("Unknown class name " + tableName);
            }
        }
        QueryClass qc = new QueryClass(c);
        if (tableAlias == null) {
            tableAlias = tableName;
        }
        q.addFrom(qc, tableAlias);
    }

    /**
     * Processes an AST node that describes a subquery in the FROM list.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the FqlQuery
     */
    private static void processNewSubQuery(AST ast, Query q,
                                           String modelPackage, Iterator iterator) {
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

        Query sq = new Query();
        processFqlStatementAST(subquery, sq, modelPackage, iterator);
        if (tableAlias == null) {
            throw new IllegalArgumentException("No alias for subquery");
        }
        q.addFrom(sq, tableAlias);
    }

    /**
     * Processes an AST node that describes a SELECT list.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     */
    private static void processSelectList(AST ast, Query q, String modelPackage) {
        do {
            switch (ast.getType()) {
                case FqlTokenTypes.SELECT_VALUE:
                    processNewSelect(ast.getFirstChild(), q, modelPackage);
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
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     */
    private static void processNewSelect(AST ast, Query q, String modelPackage) {
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
                    node = processNewQueryNode(ast, q);
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
        q.addToSelect(node, nodeAlias);
    }

    /**
     * Processes an AST node that describes a QueryNode.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @return a QueryNode object corresponding to the input
     */
    private static QueryNode processNewQueryNode(AST ast, Query q) {
        switch (ast.getType()) {
            case FqlTokenTypes.FIELD:
                return processNewField(ast.getFirstChild(), q);
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
                return processNewUnsafeFunction(ast.getFirstChild(), q);
            case FqlTokenTypes.SAFE_FUNCTION:
                return processNewSafeFunction(ast.getFirstChild(), q);
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
     * @param q the Query to build
     * @return a QueryNode object corresponding to the input
     */
    private static QueryNode processNewField(AST ast, Query q) {
        if (ast.getType() != FqlTokenTypes.IDENTIFIER) {
            throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                        + ast.getType() + "]");
        }
        Object obj = q.getReverseAliases().get(ast.getText());

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
            Query q2 = (Query) obj;
            if (secondAst == null) {
                throw new IllegalArgumentException("Path expression " + ast.getText()
                        + " cannot end at a subquery");
            } else {
                AST thirdAst = secondAst.getNextSibling();
                Object secondObj = q2.getReverseAliases().get(secondAst.getText());
                if (secondObj instanceof QueryClass) {
                    if (thirdAst == null) {
                        throw new IllegalArgumentException("Cannot reference classes inside "
                                + "subqueries - only QueryEvaluables, and fields inside classes "
                                + "inside subqueries, for path expression " + ast.getText() + "."
                                + secondAst.getText());
                    } else {
                        AST fourthAst = thirdAst.getNextSibling();
                        if (fourthAst == null) {
                            if (q2.getSelect().contains(secondObj)) {
                                try {
                                    return new QueryField(q2, (QueryClass) secondObj,
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
                        return new QueryField(q2, (QueryEvaluable) secondObj);
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
     * @param q the Query to build
     * @return a QueryExpression object correcponding to the input
     */
    private static QueryExpression processNewUnsafeFunction(AST ast, Query q) {
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
                            firstObj = (QueryEvaluable) processNewQueryNode(ast, q);
                        } else if (secondObj == null) {
                            secondObj = (QueryEvaluable) processNewQueryNode(ast, q);
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
     * @param q the Query to build
     * @return a QueryEvaluable object corresponding to the input
     */
    private static QueryEvaluable processNewSafeFunction(AST ast, Query q) {
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
                            firstObj = (QueryEvaluable) processNewQueryNode(ast, q);
                        } else if (type != -2) {
                            throw new IllegalArgumentException("Too many arguments for aggregate "
                                    + "function");
                        } else if (secondObj == null) {
                            secondObj = (QueryEvaluable) processNewQueryNode(ast, q);
                        } else if (thirdObj == null) {
                            thirdObj = (QueryEvaluable) processNewQueryNode(ast, q);
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
    private static void processOrderClause(AST ast, Query q) {
        do {
            q.addToOrderBy(processNewQueryNode(ast, q));
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a GROUP BY clause.
     *
     * @param ast an AST node to process
     */
    private static void processGroupClause(AST ast, Query q) {
        do {
            q.addToGroupBy(processNewQueryNode(ast, q));
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
     * @param andOr true if ConstraintSet is AND
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the FqlQuery
     * @return a Constraint corresponding to the input
     */
    private static Constraint processConstraintSet(AST ast, boolean andOr,
                                                   Query q, String modelPackage,
                                                   Iterator iterator) {
        Constraint retval = null;
        boolean isSet = false;
        do {
            Constraint temp = processConstraint(ast, q, modelPackage, iterator);
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
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the FqlQuery
     * @return a Constraint corresponding to the input
     */
    private static Constraint processConstraint(AST ast, Query q,
                                                String modelPackage, Iterator iterator) {
        AST subAST;
        switch (ast.getType()) {
            case FqlTokenTypes.AND_CONSTRAINT_SET:
                return processConstraintSet(ast.getFirstChild(), ConstraintSet.AND,
                                            q, modelPackage, iterator);
            case FqlTokenTypes.OR_CONSTRAINT_SET:
                return processConstraintSet(ast.getFirstChild(), ConstraintSet.OR,
                                            q, modelPackage, iterator);
            case FqlTokenTypes.CONSTRAINT:
                return processSimpleConstraint(ast, q, iterator);
            case FqlTokenTypes.SUBQUERY_CONSTRAINT:
                subAST = ast.getFirstChild();
                QueryNode leftb = processNewQueryNode(subAST, q);
                subAST = subAST.getNextSibling();
                if (subAST.getType() != FqlTokenTypes.FQL_STATEMENT) {
                    throw new IllegalArgumentException("Expected: a FQL SELECT statement");
                }
                Query rightb = new Query();
                processFqlStatementAST(subAST, rightb, modelPackage, iterator);
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
                FromElement firstObj = (FromElement) q.getReverseAliases().get(firstString);
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
                        QueryNode qc = processNewQueryNode(subAST.getNextSibling(), q);
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
                Constraint retval = processConstraint(subAST, q, modelPackage, iterator);
                retval.setNegated(!retval.isNegated());
                return retval;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
        }
    }

    private static Constraint processSimpleConstraint(AST ast, Query q, Iterator iterator) {
        AST subAST = ast.getFirstChild();
        QueryNode left = processNewQueryNode(subAST, q);
        subAST = subAST.getNextSibling();
        QueryOp op = null;
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
                if (FqlTokenTypes.QUESTION_MARK == subAST.getType()) {
                    if (left instanceof QueryClass) {
                        try {
                            if (op == SimpleConstraint.EQUALS) {
                                return new ClassConstraint((QueryClass) left,
                                                           ClassConstraint.EQUALS, iterator.next());
                            } else if (op == SimpleConstraint.NOT_EQUALS) {
                                return new ClassConstraint((QueryClass) left,
                                                           ClassConstraint.NOT_EQUALS,
                                                           iterator.next());
                            } else {
                                throw new IllegalArgumentException("Operation is not valid for "
                                                                   + "comparing a class to an "
                                                                   + "object");
                            }
                        } catch (NoSuchElementException e) {
                            throw new IllegalArgumentException("Not enough parameters in "
                                                               + "FqlQuery object");
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot compare a field to an object");
                    }
                } else {
                    QueryNode right = processNewQueryNode(subAST, q);
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

}
