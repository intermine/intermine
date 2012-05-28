package org.intermine.objectstore.query.iql;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.ObjectStoreBagsForObject;
import org.intermine.objectstore.query.OrderDescending;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryClassBag;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryForeignKey;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryOrderable;
import org.intermine.objectstore.query.QueryPathExpressionWithSelect;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.objectstore.query.SubqueryExistsConstraint;
import org.intermine.objectstore.query.UnknownTypeValue;

import antlr.collections.AST;

/**
 * Parser for the InterMine dialect of OQL (IQL)
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public final class IqlQueryParser
{
    /**
     * All methods are static - don't allow instance to be constructed
     */
    private IqlQueryParser() {
    }

    /**
     * Construct a new query by parsing a String.
     *
     * @param iq an IqlQuery object to parse
     * @return the Query representing the IqlQuery
     * @throws IllegalArgumentException if the OQLQuery contains an invalid query String
     */
    public static Query parse(IqlQuery iq) {
        Query q = new Query();
        q.setDistinct(false);
        String modelPackage = iq.getPackageName();
        String iql = iq.getQueryString();
        Iterator<?> iterator = iq.getParameters().iterator();
        AST ast = null;

        try {
            InputStream is = new ByteArrayInputStream(iql.getBytes());

            IqlLexer lexer = new IqlLexer(is);
            IqlParser parser = new IqlParser(lexer);
            parser.start_rule();

            ast = parser.getAST();

            if (ast == null) {
                throw new IllegalArgumentException("Invalid IQL string " + iql);
            }

            processIqlStatementAST(ast, q, modelPackage, iterator);

            return q;
        } catch (antlr.RecognitionException e) {
            System .out.println("Dumping AST Tree:");
            antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();
            visitor.visit(ast);
            StringBuffer message = new StringBuffer();
            try {
                InputStream is = new ByteArrayInputStream(iql.getBytes());
                IqlLexer lexer = new IqlLexer(is);
                boolean needComma = false;
                antlr.Token token;
                do {
                    token = lexer.nextToken();
                    if (needComma) {
                        message.append(", ");
                    }
                    needComma = true;
                    message.append(token.toString());
                } while (token.getType() != antlr.Token.EOF_TYPE);
            } catch (antlr.TokenStreamException e3) {
            }
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage()
                    + ". Lexer stream: " + message.toString());
            e2.initCause(e);
            throw e2;
        } catch (antlr.TokenStreamException e) {
            System .out.println("Dumping AST Tree:");
            antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();
            visitor.visit(ast);
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            System .out.println("Dumping AST Tree:");
            antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();
            visitor.visit(ast);
            throw e;
        } catch (ClassCastException e) {
            System .out.println("Dumping AST Tree:");
            antlr.DumpASTVisitor visitor = new antlr.DumpASTVisitor();
            visitor.visit(ast);
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Processes an IQL_STATEMENT AST node produced by antlr.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processIqlStatementAST(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        if (ast.getType() != IqlTokenTypes.IQL_STATEMENT) {
            throw new IllegalArgumentException("Expected: an IQL SELECT statement");
        }
        processAST(ast.getFirstChild(), q, modelPackage, iterator);
        for (QuerySelectable qs : q.getSelect()) {
            if (qs instanceof QueryValue) {
                QueryValue qv = (QueryValue) qs;
                if (UnknownTypeValue.class.equals(qv.getType())) {
                    if (((UnknownTypeValue) qv.getValue()).getApproximateType()
                            == UnknownTypeValue.TYPE_STRING) {
                        qv.youAreType(String.class);
                    }
                }
            }
        }
    }

    /**
     * Processes an AST node produced by antlr, at the top level of the IQL query.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processAST(AST ast, Query q, String modelPackage, Iterator<?> iterator) {
        AST selectAST = null;
        AST orderAST = null;
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.LITERAL_distinct:
                    q.setDistinct(true);
                    break;
                case IqlTokenTypes.SELECT_LIST:
                    selectAST = ast;
                    break;
                case IqlTokenTypes.FROM_LIST:
                    processFromList(ast.getFirstChild(), q, modelPackage, iterator);
                    break;
                case IqlTokenTypes.WHERE_CLAUSE:
                    q.setConstraint(processConstraint(ast.getFirstChild(), q, modelPackage,
                                iterator));
                    break;
                case IqlTokenTypes.GROUP_CLAUSE:
                    processGroupClause(ast.getFirstChild(), q, modelPackage, iterator);
                    break;
                case IqlTokenTypes.ORDER_CLAUSE:
                    orderAST = ast;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        if (selectAST != null) {
            processSelectList(selectAST.getFirstChild(), q, modelPackage, iterator);
        }
        if (orderAST != null) {
            processOrderClause(orderAST.getFirstChild(), q, modelPackage, iterator);
        }
    }

    /**
     * Processes an AST node that describes a FROM list.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processFromList(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.TABLE:
                    processNewTable(ast.getFirstChild(), q, modelPackage, iterator);
                    break;
                case IqlTokenTypes.SUBQUERY:
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
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processNewTable(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        String tableAlias = null;
        String tableName = null;
        Set<Class<?>> classes = new HashSet<Class<?>>();
        boolean isBag = false;
        ObjectStoreBag osb = null;
        if (ast.getType() == IqlTokenTypes.QUESTION_MARK) {
            isBag = true;
            ast = ast.getNextSibling();
        } else if (ast.getType() == IqlTokenTypes.OBJECTSTOREBAG) {
            isBag = true;
            osb = processNewObjectStoreBag(ast.getFirstChild());
            ast = ast.getNextSibling();
        }
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.TABLE_NAME:
                    tableName = null;
                    AST tableNameAst = ast.getFirstChild();
                    do {
                        String temp = unescape(tableNameAst.getText());
                        tableName = (tableName == null ? temp : tableName + "." + temp);
                        tableNameAst = tableNameAst.getNextSibling();
                    } while (tableNameAst != null);
                    Class<?> c = null;
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
                    classes.add(c);
                    break;
                case IqlTokenTypes.TABLE_ALIAS:
                    tableAlias = unescape(ast.getFirstChild().getText());
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        if (tableAlias == null) {
            if (classes.size() == 1) {
                int index = tableName.lastIndexOf('.');
                if (index == -1) {
                    tableAlias = tableName;
                } else {
                    tableAlias = tableName.substring(index + 1);
                }
            } else {
                throw new IllegalArgumentException("Dynamic classes in the FROM clause must have"
                        + " an alias");
            }
        }
        if (isBag) {
            if (osb == null) {
                QueryClassBag qcb = new QueryClassBag(classes, (Collection<?>) iterator.next());
                q.addFrom(qcb, tableAlias);
            } else {
                QueryClassBag qcb = new QueryClassBag(classes, osb);
                q.addFrom(qcb, tableAlias);
            }
        } else {
            QueryClass qc = new QueryClass(classes);
            q.addFrom(qc, tableAlias);
        }
    }

    /**
     * Processes an AST node that describes a subquery in the FROM list.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processNewSubQuery(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        AST subquery = null;
        String tableAlias = null;
        int limit = Integer.MAX_VALUE;
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.IQL_STATEMENT:
                    if (subquery == null) {
                        subquery = ast;
                    }
                    break;
                case IqlTokenTypes.TABLE_ALIAS:
                    tableAlias = unescape(ast.getFirstChild().getText());
                    break;
                case IqlTokenTypes.SUBQUERY_LIMIT:
                    limit = Integer.parseInt(ast.getFirstChild().getText());
                    break;
                default:
                    throw (new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                                + ast.getType() + "]"));
            }
            ast = ast.getNextSibling();
        } while (ast != null);

        Query sq = new Query();
        sq.setDistinct(false);
        processIqlStatementAST(subquery, sq, modelPackage, iterator);
        sq.setLimit(limit);
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
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @param modelPackage the package for unqualified class names
     */
    private static void processSelectList(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.SELECT_VALUE:
                    processNewSelect(ast.getFirstChild(), q, modelPackage, iterator);
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
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @param modelPackage the package for unqualified class names
     */
    private static void processNewSelect(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        QuerySelectable node = null;
        String nodeAlias = null;
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.FIELD_ALIAS:
                    nodeAlias = unescape(ast.getFirstChild().getText());
                    break;
                case IqlTokenTypes.FIELD:
                case IqlTokenTypes.CONSTANT:
                case IqlTokenTypes.UNSAFE_FUNCTION:
                case IqlTokenTypes.SAFE_FUNCTION:
                case IqlTokenTypes.TYPECAST:
                    node = processNewQuerySelectable(ast, q, modelPackage, iterator);
                    break;
                case IqlTokenTypes.OBJECTSTOREBAG:
                    if (node instanceof ObjectStoreBagCombination) {
                        ((ObjectStoreBagCombination) node).addBag(processNewObjectStoreBag(ast
                                .getFirstChild()));
                    } else {
                        node = processNewObjectStoreBag(ast.getFirstChild());
                    }
                    break;
                case IqlTokenTypes.LITERAL_union:
                    if (node instanceof ObjectStoreBag) {
                        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(
                                ObjectStoreBagCombination.UNION);
                        osbc.addBag((ObjectStoreBag) node);
                        node = osbc;
                    } else if (node instanceof ObjectStoreBagCombination) {
                        if (((ObjectStoreBagCombination) node).getOp()
                                != ObjectStoreBagCombination.UNION) {
                            throw new IllegalArgumentException("Cannot mix UNION, INTERSECT, "
                                    + "EXCEPT, and ALLBUTINTERSECT in a bag fetch query");
                        }
                    } else {
                        throw new IllegalArgumentException("UNION can only apply to bag fetches");
                    }
                    break;
                case IqlTokenTypes.LITERAL_intersect:
                    if (node instanceof ObjectStoreBag) {
                        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(
                                ObjectStoreBagCombination.INTERSECT);
                        osbc.addBag((ObjectStoreBag) node);
                        node = osbc;
                    } else if (node instanceof ObjectStoreBagCombination) {
                        if (((ObjectStoreBagCombination) node).getOp()
                                != ObjectStoreBagCombination.INTERSECT) {
                            throw new IllegalArgumentException("Cannot mix UNION, INTERSECT, "
                                    + "EXCEPT, and ALLBUTINTERSECT in a bag fetch query");
                        }
                    } else {
                        throw new IllegalArgumentException(
                                "INTERSECT can only apply to bag fetches");
                    }
                    break;
                case IqlTokenTypes.LITERAL_except:
                    if (node instanceof ObjectStoreBag) {
                        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(
                                ObjectStoreBagCombination.EXCEPT);
                        osbc.addBag((ObjectStoreBag) node);
                        node = osbc;
                    } else if (node instanceof ObjectStoreBagCombination) {
                        if (((ObjectStoreBagCombination) node).getOp()
                                != ObjectStoreBagCombination.EXCEPT) {
                            throw new IllegalArgumentException("Cannot mix UNION, INTERSECT, "
                                    + "EXCEPT, and ALLBUTINTERSECT in a bag fetch query");
                        }
                    } else {
                        throw new IllegalArgumentException("EXCEPT can only apply to bag fetches");
                    }
                    break;
                case IqlTokenTypes.LITERAL_allbutintersect:
                    if (node instanceof ObjectStoreBag) {
                        ObjectStoreBagCombination osbc = new ObjectStoreBagCombination(
                                ObjectStoreBagCombination.ALLBUTINTERSECT);
                        osbc.addBag((ObjectStoreBag) node);
                        node = osbc;
                    } else if (node instanceof ObjectStoreBagCombination) {
                        if (((ObjectStoreBagCombination) node).getOp()
                                != ObjectStoreBagCombination.ALLBUTINTERSECT) {
                            throw new IllegalArgumentException("Cannot mix UNION, INTERSECT, "
                                    + "EXCEPT, and ALLBUTINTERSECT in a bag fetch query");
                        }
                    } else {
                        throw new IllegalArgumentException("ALLBUTINTERSECT can only apply to bag"
                                + " fetches");
                    }
                    break;
                case IqlTokenTypes.BAGS_FOR:
                    node = processNewBagsFor(ast.getFirstChild(), iterator);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        if ((nodeAlias == null) != (node instanceof QueryClass
                    || node instanceof ObjectStoreBag
                    || node instanceof ObjectStoreBagCombination
                    || node instanceof ObjectStoreBagsForObject)) {
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
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a QueryNode object corresponding to the input
     */
    private static QueryNode processNewQueryNode(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        Object retval = processNewQueryNodeOrReference(ast, q, false, modelPackage, iterator);
        if (retval instanceof QueryObjectReference) {
            QueryObjectReference qor = (QueryObjectReference) retval;
            throw new IllegalArgumentException("Object reference " + qor.getQueryClass().getType()
                    .getName() + "." + qor.getFieldName() + " present where a QueryNode is"
                    + " required.");
        }
        return (QueryNode) retval;
    }

    /**
     * Processes an AST node that describes a QuerySelectable.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a QuerySelectable object corresponding to the input
     */
    private static QuerySelectable processNewQuerySelectable(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        return (QuerySelectable) processNewQueryNodeOrReference(ast, q, true, modelPackage,
                iterator);
    }

    private static Object processNewQueryNodeOrReference(AST ast, Query q,
            boolean isSelect, String modelPackage, Iterator<?> iterator) {
        switch (ast.getType()) {
            case IqlTokenTypes.FIELD:
                return processNewField(ast.getFirstChild(), q, isSelect, modelPackage, iterator);
            case IqlTokenTypes.CONSTANT:
                return processNewQueryValue(ast.getFirstChild());
            case IqlTokenTypes.UNSAFE_FUNCTION:
                return processNewUnsafeFunction(ast.getFirstChild(), q, modelPackage, iterator);
            case IqlTokenTypes.SAFE_FUNCTION:
                return processNewSafeFunction(ast.getFirstChild(), q, modelPackage, iterator);
            case IqlTokenTypes.TYPECAST:
                return processNewTypeCast(ast.getFirstChild(), q, modelPackage, iterator);
            case IqlTokenTypes.ORDER_DESC:
                return new OrderDescending((QueryOrderable) processNewQueryNodeOrReference(ast
                            .getFirstChild(), q, isSelect, modelPackage, iterator));
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
        }
    }

    /**
     * Process an AST node that describes a QueryValue.
     *
     * @param ast an AST node to process
     * @return a QueryValue object
     */
    private static QueryValue processNewQueryValue(AST ast) {
        String value = unescape(ast.getText());
        return new QueryValue(new UnknownTypeValue(value));
    }

    /**
     * Process an AST node that describes an ObjectStoreBag.
     *
     * @param ast an AST node to process
     * @return an ObjectStoreBag object
     */
    private static ObjectStoreBag processNewObjectStoreBag(AST ast) {
        String value = unescape(ast.getText());
        return new ObjectStoreBag(Integer.parseInt(value));
    }

    /**
     * Process an AST node that describes an ObjectStoreBagsForObject.
     *
     * @param ast an AST node to process
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return an ObjectStoreBagsForObject object
     */
    private static ObjectStoreBagsForObject processNewBagsFor(AST ast, Iterator<?> iterator) {
        String value = unescape(ast.getText());
        AST sibling = ast.getNextSibling();
        if (sibling != null) {
            if (IqlTokenTypes.QUESTION_MARK == sibling.getType()) {
                @SuppressWarnings("unchecked") Collection<ObjectStoreBag> param =
                    (Collection) iterator.next();
                return new ObjectStoreBagsForObject(new Integer(Integer.parseInt(value)), param);
            }
            throw new IllegalArgumentException("Unknown AST node: " + sibling.getText() + " ["
                    + sibling.getType() + "]");
        }
        return new ObjectStoreBagsForObject(new Integer(Integer.parseInt(value)));
    }

    /**
     * Processes an AST node that describes a QueryField or QueryClass in the SELECT list.
     * There are several possible arrangements:
     * 1. a     where a is a QueryClass.
     * 2. a.b   where a is a QueryClass, and b is a QueryField.
     * 3. a.b   where a is a QueryClass, and b is a QueryObjectReference.
     * 3. a.b   where a is a Query, and b is a QueryEvaluable.
     * 4. a.b.c where a is a Query, b is a QueryClass, and c is a QueryField.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param isSelect true if this is on a SELECT list
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a QueryNode object corresponding to the input
     */
    private static Object processNewField(AST ast, Query q, boolean isSelect, String modelPackage,
            Iterator<?> iterator) {
        if (ast.getType() != IqlTokenTypes.IDENTIFIER) {
            throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                    + ast.getType() + "]");
        }
        Object obj = q.getReverseAliases().get(unescape(ast.getText()));
        String text = ast.getText();

        if ((obj instanceof QueryClass) || (obj instanceof QueryClassBag)) {
            ast = ast.getNextSibling();
            if (ast == null) {
                return obj;
            } else {
                if (obj instanceof QueryClassBag) {
                    if ("id".equals(ast.getText()) && (ast.getNextSibling() == null)) {
                        return new QueryField((QueryClassBag) obj);
                    } else {
                        throw new IllegalArgumentException("Can only access the \"id\" attribute of"
                                + " QueryClassBag \"" + text + "\"");
                    }
                }
                AST collectionSelectAst = null;
                while (ast != null) {
                    text += "." + ast.getText();
                    if (obj instanceof QueryClass) {
                        try {
                            obj = new QueryField((QueryClass) obj, unescape(ast.getText()));
                        } catch (IllegalArgumentException e) {
                            if (isSelect) {
                                try {
                                    obj = new QueryObjectPathExpression((QueryClass) obj, unescape(
                                                ast.getText()));
                                } catch (IllegalArgumentException e2) {
                                    obj = new QueryCollectionPathExpression((QueryClass) obj,
                                            unescape(ast.getText()));
                                }
                            } else {
                                obj = new QueryObjectReference((QueryClass) obj, unescape(ast
                                            .getText()));
                            }
                        }
                    } else if (obj instanceof QueryObjectPathExpression) {
                        if ("id".equals(ast.getText())) {
                            obj = new QueryForeignKey(((QueryObjectPathExpression) obj)
                                    .getQueryClass(), ((QueryObjectPathExpression) obj)
                                    .getFieldName());
                        } else {
                            QueryObjectPathExpression ref = (QueryObjectPathExpression) obj;
                            Collection<Integer> empty = Collections.emptySet();
                            switch(ast.getType()) {
                                case IqlTokenTypes.IDENTIFIER:
                                    throw new IllegalArgumentException("Path expression " + text
                                            + " extends beyond a reference");
                                case IqlTokenTypes.COLLECTION_SELECT_LIST:
                                    collectionSelectAst = ast.getFirstChild();
                                    break;
                                case IqlTokenTypes.WHERE_CLAUSE:
                                    ref.setConstraint(processConstraint(ast.getFirstChild(),
                                                ref.getQuery(empty, false), modelPackage,
                                                iterator));
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unknown AST node " + ast);
                            }
                        }
                    } else if (obj instanceof QueryField) {
                        throw new IllegalArgumentException("Path expression " + text + " extends "
                                + "beyond a field");
                    } else if (obj instanceof QueryForeignKey) {
                        throw new IllegalArgumentException("Path expression " + text + " extends "
                                + "beyond a foreign key");
                    } else if (obj instanceof QueryCollectionPathExpression) {
                        QueryCollectionPathExpression col = (QueryCollectionPathExpression) obj;
                        Collection<InterMineObject> empty = Collections.emptySet();
                        switch(ast.getType()) {
                            case IqlTokenTypes.IDENTIFIER:
                                throw new IllegalArgumentException("Path expression " + text
                                        + " extends beyond a collection");
                            case IqlTokenTypes.LITERAL_singleton:
                                col.setSingleton(true);
                                break;
                            case IqlTokenTypes.COLLECTION_SELECT_LIST:
                                collectionSelectAst = ast.getFirstChild();
                                break;
                            case IqlTokenTypes.WHERE_CLAUSE:
                                col.setConstraint(processConstraint(ast.getFirstChild(),
                                            col.getQuery(empty), modelPackage, iterator));
                                break;
                            case IqlTokenTypes.FROM_LIST:
                                Query colQuery = new Query();
                                processFromList(ast.getFirstChild(), colQuery, modelPackage,
                                        iterator);
                                for (FromElement from : colQuery.getFrom()) {
                                    String alias = colQuery.getAliases().get(from);
                                    col.addFrom(from, alias);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown AST node " + ast);
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown type");
                    }
                    ast = ast.getNextSibling();
                }
                if (collectionSelectAst != null) {
                    QueryPathExpressionWithSelect col = (QueryPathExpressionWithSelect) obj;
                    Query colQuery;
                    if (col instanceof QueryCollectionPathExpression) {
                        Collection<InterMineObject> empty = Collections.emptySet();
                        colQuery = ((QueryCollectionPathExpression) col).getQuery(empty);
                    } else {
                        Collection<Integer> empty = Collections.emptySet();
                        colQuery = ((QueryObjectPathExpression) col).getQuery(empty, false);
                    }
                    do {
                        switch(collectionSelectAst.getType()) {
                            case IqlTokenTypes.FIELD:
                            case IqlTokenTypes.CONSTANT:
                            case IqlTokenTypes.UNSAFE_FUNCTION:
                            case IqlTokenTypes.SAFE_FUNCTION:
                            case IqlTokenTypes.TYPECAST:
                                col.addToSelect(processNewQuerySelectable(collectionSelectAst,
                                            colQuery, modelPackage, iterator));
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown AST node "
                                        + collectionSelectAst);
                        }
                        collectionSelectAst = collectionSelectAst.getNextSibling();
                    } while (collectionSelectAst != null);
                }
                return obj;
            }
        } else if (obj instanceof Query) {
            AST secondAst = ast.getNextSibling();
            Query q2 = (Query) obj;
            if (secondAst == null) {
                throw new IllegalArgumentException("Path expression " + ast.getText()
                        + " cannot end at a subquery");
            } else {
                AST thirdAst = secondAst.getNextSibling();
                Object secondObj = q2.getReverseAliases().get(unescape(secondAst.getText()));
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
                                return new QueryField(q2, (QueryClass) secondObj,
                                                      unescape(thirdAst.getText()));
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
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a QueryExpression object correcponding to the input
     */
    private static QueryExpression processNewUnsafeFunction(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        QueryEvaluable firstObj = null;
        QueryEvaluable secondObj = null;
        int type = -1;
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.FIELD:
                case IqlTokenTypes.CONSTANT:
                case IqlTokenTypes.UNSAFE_FUNCTION:
                case IqlTokenTypes.SAFE_FUNCTION:
                case IqlTokenTypes.TYPECAST:
                    try {
                        if (firstObj == null) {
                            firstObj = (QueryEvaluable) processNewQueryNode(ast, q, modelPackage,
                                    iterator);
                        } else if (secondObj == null) {
                            secondObj = (QueryEvaluable) processNewQueryNode(ast, q, modelPackage,
                                    iterator);
                        } else {
                            throw new IllegalArgumentException("QueryExpressions can only have two "
                                    + "arguments");
                        }
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Expressions cannot contain classes as "
                                + "arguments");
                    }
                    break;
                case IqlTokenTypes.PLUS:
                    type = QueryExpression.ADD;
                    break;
                case IqlTokenTypes.MINUS:
                    type = QueryExpression.SUBTRACT;
                    break;
                case IqlTokenTypes.ASTERISK:
                    type = QueryExpression.MULTIPLY;
                    break;
                case IqlTokenTypes.DIVIDE:
                    type = QueryExpression.DIVIDE;
                    break;
                case IqlTokenTypes.PERCENT:
                    type = QueryExpression.MODULO;
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
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a QueryEvaluable object corresponding to the input
     */
    private static QueryEvaluable processNewSafeFunction(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        QueryEvaluable firstObj = null;
        QueryEvaluable secondObj = null;
        QueryEvaluable thirdObj = null;
        int type = -1;
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.FIELD:
                case IqlTokenTypes.CONSTANT:
                case IqlTokenTypes.UNSAFE_FUNCTION:
                case IqlTokenTypes.SAFE_FUNCTION:
                case IqlTokenTypes.TYPECAST:
                    try {
                        if (type == QueryFunction.COUNT) {
                            throw new IllegalArgumentException("Count() does not take an argument");
                        } else if (firstObj == null) {
                            firstObj = (QueryEvaluable) processNewQueryNode(ast, q, modelPackage,
                                    iterator);
                        } else if (type > -2) {
                            throw new IllegalArgumentException("Too many arguments for aggregate "
                                    + "function");
                        } else if (secondObj == null) {
                            secondObj = (QueryEvaluable) processNewQueryNode(ast, q, modelPackage,
                                    iterator);
                        } else if (thirdObj == null) {
                            thirdObj = (QueryEvaluable) processNewQueryNode(ast, q, modelPackage,
                                    iterator);
                        } else {
                            throw new IllegalArgumentException("Too many arguments in substring");
                        }
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Functions cannot contain classes as "
                                + "arguments");
                    }
                    break;
                case IqlTokenTypes.LITERAL_count:
                    type = QueryFunction.COUNT;
                    break;
                case IqlTokenTypes.LITERAL_sum:
                    type = QueryFunction.SUM;
                    break;
                case IqlTokenTypes.LITERAL_avg:
                    type = QueryFunction.AVERAGE;
                    break;
                case IqlTokenTypes.LITERAL_min:
                    type = QueryFunction.MIN;
                    break;
                case IqlTokenTypes.LITERAL_max:
                    type = QueryFunction.MAX;
                    break;
                case IqlTokenTypes.LITERAL_substr:
                    type = -2;
                    break;
                case IqlTokenTypes.LITERAL_indexof:
                    type = -3;
                    break;
                case IqlTokenTypes.LITERAL_lower:
                    type = -4;
                    break;
                case IqlTokenTypes.LITERAL_upper:
                    type = -5;
                    break;
                case IqlTokenTypes.LITERAL_stddev:
                    type = QueryFunction.STDDEV;
                    break;
                case IqlTokenTypes.LITERAL_ceil:
                    type = QueryFunction.CEIL;
                    break;
                case IqlTokenTypes.LITERAL_floor:
                    type = QueryFunction.FLOOR;
                    break;
                case IqlTokenTypes.LITERAL_greatest:
                    type = -6;
                    break;
                case IqlTokenTypes.LITERAL_least:
                    type = -7;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        if (type == -2) {
            if (secondObj == null) {
                throw new IllegalArgumentException("Not enough arguments for substring function");
            } else if (thirdObj == null) {
                return new QueryExpression(firstObj, QueryExpression.SUBSTRING, secondObj);
            } else {
                return new QueryExpression(firstObj, secondObj, thirdObj);
            }
        } else if (type == -3) {
            if (thirdObj != null) {
                throw new IllegalArgumentException("Too many arguments for indexof function");
            }
            if (secondObj == null) {
                throw new IllegalArgumentException("Too few arguments for indexof function");
            }
            return new QueryExpression(firstObj, QueryExpression.INDEX_OF, secondObj);
        } else if (type == -4) {
            return new QueryExpression(QueryExpression.LOWER, firstObj);
        } else if (type == -5) {
            return new QueryExpression(QueryExpression.UPPER, firstObj);
        } else if (type == -6) {
            return new QueryExpression(firstObj, QueryExpression.GREATEST, secondObj);
        } else if (type == -7) {
            return new QueryExpression(firstObj, QueryExpression.LEAST, secondObj);
        } else if (type == QueryFunction.COUNT) {
            return new QueryFunction();
        } else {
            if (firstObj == null) {
                throw new IllegalArgumentException("Need an argument for this function");
            } else {
                if (firstObj instanceof QueryField) {
                    return new QueryFunction(firstObj, type);
                } else if (firstObj instanceof QueryExpression) {
                    return new QueryFunction(firstObj, type);
                } else {
                    throw new IllegalArgumentException("Arguments to aggregate functions may "
                            + "be fields or expressions only");
                }
            }
        }
    }

    /**
     * Processes an AST node that describes a typecast.
     *
     * @param ast an AST node to process
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a QueryEvaluable object corresponding to the input
     */
    private static QueryCast processNewTypeCast(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        QueryEvaluable value = null;
        String type = null;
        do {
            switch (ast.getType()) {
                case IqlTokenTypes.FIELD:
                case IqlTokenTypes.CONSTANT:
                case IqlTokenTypes.UNSAFE_FUNCTION:
                case IqlTokenTypes.SAFE_FUNCTION:
                case IqlTokenTypes.TYPECAST:
                    try {
                        value = (QueryEvaluable) processNewQueryNode(ast, q, modelPackage,
                                iterator);
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("TypeCasts cannot contains classes as"
                                + " arguments");
                    }
                    break;
                case IqlTokenTypes.IDENTIFIER:
                    type = unescape(ast.getText());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown AST node " + ast.getText() + " ["
                            + ast.getType() + "]");
            }
            ast = ast.getNextSibling();
        } while (ast != null);
        Class<?> typeClass = null;
        if ("String".equals(type)) {
            typeClass = String.class;
        } else if ("Boolean".equals(type)) {
            typeClass = Boolean.class;
        } else if ("Short".equals(type)) {
            typeClass = Short.class;
        } else if ("Integer".equals(type)) {
            typeClass = Integer.class;
        } else if ("Long".equals(type)) {
            typeClass = Long.class;
        } else if ("Float".equals(type)) {
            typeClass = Float.class;
        } else if ("Double".equals(type)) {
            typeClass = Double.class;
        } else if ("BigDecimal".equals(type)) {
            typeClass = BigDecimal.class;
        } else if ("Date".equals(type)) {
            typeClass = Date.class;
        } else {
            throw new IllegalArgumentException("Invalid type cast to " + type);
        }
        return new QueryCast(value, typeClass);
    }

    /**
     * Processes an AST node that describes a ORDER BY clause.
     *
     * @param ast an AST node to process
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processOrderClause(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        do {
            QueryOrderable qo = (QueryOrderable) processNewQueryNodeOrReference(ast, q, false,
                    modelPackage, iterator);
            for (QuerySelectable qs : q.getSelect()) {
                if (qo.equals(qs)) {
                    qo = (QueryOrderable) qs;
                    break;
                }
            }
            q.addToOrderBy(qo);
            ast = ast.getNextSibling();
        } while (ast != null);
    }

    /**
     * Processes an AST node that describes a GROUP BY clause.
     *
     * @param ast an AST node to process
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     */
    private static void processGroupClause(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        do {
            q.addToGroupBy(processNewQueryNode(ast, q, modelPackage, iterator));
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
     * @param op AND, OR, NAND, or NOR
     * @param q the Query to build
     * @param modelPackage the package for unqualified class names
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a Constraint corresponding to the input
     */
    private static Constraint processConstraintSet(AST ast, ConstraintOp op, Query q,
            String modelPackage, Iterator<?> iterator) {
        Constraint retval = null;
        boolean isSet = false;
        do {
            Constraint temp = processConstraint(ast, q, modelPackage, iterator);
            if (retval == null) {
                retval = temp;
            } else if (!isSet) {
                Constraint temp2 = retval;
                retval = new ConstraintSet(op);
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
     * @param iterator an iterator through the list of parameters of the IqlQuery
     * @return a Constraint corresponding to the input
     */
    private static Constraint processConstraint(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        AST subAST;
        switch (ast.getType()) {
            case IqlTokenTypes.AND_CONSTRAINT_SET:
                return processConstraintSet(ast.getFirstChild(), ConstraintOp.AND,
                                            q, modelPackage, iterator);
            case IqlTokenTypes.OR_CONSTRAINT_SET:
                return processConstraintSet(ast.getFirstChild(), ConstraintOp.OR,
                                            q, modelPackage, iterator);
            case IqlTokenTypes.LITERAL_true:
                return new ConstraintSet(ConstraintOp.AND);
            case IqlTokenTypes.LITERAL_false:
                return new ConstraintSet(ConstraintOp.OR);
            case IqlTokenTypes.CONSTRAINT:
                return processSimpleConstraint(ast, q, modelPackage, iterator);
            case IqlTokenTypes.SUBQUERY_CONSTRAINT:
                subAST = ast.getFirstChild();
                QueryNode leftb = processNewQueryNode(subAST, q, modelPackage, iterator);
                subAST = subAST.getNextSibling();
                if (subAST.getType() != IqlTokenTypes.IQL_STATEMENT) {
                    throw new IllegalArgumentException("Expected: an IQL SELECT statement");
                }
                Query rightb = new Query();
                rightb.setDistinct(false);
                processIqlStatementAST(subAST, rightb, modelPackage, iterator);
                if (leftb instanceof QueryClass) {
                    return new SubqueryConstraint((QueryClass) leftb, ConstraintOp.IN,
                            rightb);
                } else {
                    return new SubqueryConstraint((QueryEvaluable) leftb, ConstraintOp.IN,
                            rightb);
                }
            case IqlTokenTypes.SUBQUERY_EXISTS_CONSTRAINT:
                subAST = ast.getFirstChild();
                if (subAST.getType() != IqlTokenTypes.IQL_STATEMENT) {
                    throw new IllegalArgumentException("Expected: an IQL SELECT statement");
                }
                Query subquery = new Query();
                subquery.setDistinct(false);
                processIqlStatementAST(subAST, subquery, modelPackage, iterator);
                return new SubqueryExistsConstraint(ConstraintOp.EXISTS, subquery);
            case IqlTokenTypes.CONTAINS_CONSTRAINT:
                subAST = ast.getFirstChild();
                if (subAST.getType() != IqlTokenTypes.FIELD) {
                    throw new IllegalArgumentException("Expected a Collection or Object Reference "
                            + "as the first argument of the ContainsConstraint");
                }
                AST subSubAST = subAST.getFirstChild();
                String firstString = unescape(subSubAST.getText());
                if ("?".equals(firstString)) {
                    // Grab object from parameters
                    subSubAST = subSubAST.getNextSibling();
                    if (subSubAST != null) {
                        String secondString = unescape(subSubAST.getText());
                        if (subSubAST.getNextSibling() != null) {
                            throw new IllegalArgumentException("Path expression " + firstString
                                    + "." + secondString + "."
                                    + subSubAST.getNextSibling().getText() + " extends beyond a "
                                    + " collection");
                        }
                        QueryCollectionReference ref = null;
                        try {
                            ref = new QueryCollectionReference((InterMineObject) iterator.next(),
                                    secondString);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Object " + firstString + "."
                                    + secondString + " does not exist, or is not a collection");
                        }
                        if (subAST.getNextSibling().getType() == IqlTokenTypes.QUESTION_MARK) {
                            return new ContainsConstraint(ref, ConstraintOp.CONTAINS,
                                    (InterMineObject) iterator.next());
                        } else {
                            QueryNode qc = processNewQueryNode(subAST.getNextSibling(), q,
                                    modelPackage, iterator);
                            if (qc instanceof QueryClass) {
                                return new ContainsConstraint(ref, ConstraintOp.CONTAINS,
                                        (QueryClass) qc);
                            } else {
                                throw new IllegalArgumentException("Collection " + firstString
                                        + "." + secondString + " cannot contain anything but a "
                                        + "QueryClass or InterMineObject");
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Path expression for collection cannot "
                                + "end on a QueryClass");
                    }
                } else {
                    FromElement firstObj = (FromElement) q.getReverseAliases().get(firstString);
                    if ((firstObj instanceof QueryClass) || (firstObj instanceof QueryClassBag)) {
                        subSubAST = subSubAST.getNextSibling();
                        if (subSubAST != null) {
                            String secondString = unescape(subSubAST.getText());
                            if (subSubAST.getNextSibling() != null) {
                                throw new IllegalArgumentException("Path expression " + firstString
                                        + "." + secondString + "."
                                        + subSubAST.getNextSibling().getText() + " extends beyond a"
                                        + " collection or object reference");
                            }
                            QueryReference ref = null;
                            try {
                                if (firstObj instanceof QueryClass) {
                                    try {
                                        // See if it is an object reference.
                                        ref = new QueryObjectReference((QueryClass) firstObj,
                                                secondString);
                                    } catch (IllegalArgumentException e) {
                                        // Okay, it wasn't. See if it is a collection.
                                        ref = new QueryCollectionReference((QueryClass) firstObj,
                                                secondString);
                                    }
                                } else {
                                    ref = new QueryCollectionReference((QueryClassBag) firstObj,
                                            secondString);
                                }
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("Object "
                                                + firstString + "." + secondString
                                                + " does not exist, or is not a collection or "
                                                + "object reference");
                            }

                            // Now we have a collection or object reference. Now we need a class or
                            // object.
                            if (subAST.getNextSibling().getType() == IqlTokenTypes.QUESTION_MARK) {
                                return new ContainsConstraint(ref, ConstraintOp.CONTAINS,
                                        (InterMineObject) iterator.next());
                            } else {
                                QueryNode qc = processNewQueryNode(subAST.getNextSibling(), q,
                                        modelPackage, iterator);
                                if (qc instanceof QueryClass) {
                                    return new ContainsConstraint(ref, ConstraintOp.CONTAINS,
                                            (QueryClass) qc);
                                } else {
                                    throw new IllegalArgumentException("Collection or object "
                                            + "reference " + firstString + "." + secondString
                                            + " cannot contain anything but a QueryClass or "
                                            + "InterMineObject");
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Path expression for collection "
                                    + "cannot end on a QueryClass");
                        }
                    } else if (firstObj instanceof Query) {
                        throw new IllegalArgumentException("Cannot access a collection or object "
                                + "reference inside subquery " + firstString);
                    } else {
                        throw new IllegalArgumentException("No such object " + firstString
                                + " while looking for a collection or object reference");
                    }
                }
            case IqlTokenTypes.BAG_CONSTRAINT:
                subAST = ast.getFirstChild();
                //if (subAST.getType() != IqlTokenTypes.FIELD) {
                //    throw new IllegalArgumentException("Expected a QueryEvaluable or QueryClass "
                //            + "as the first argument of a BagConstraint");
                //}
                QueryNode leftd = processNewQueryNode(subAST, q, modelPackage, iterator);
                ObjectStoreBag osb = null;
                if (subAST.getNextSibling() != null) {
                    if (subAST.getNextSibling().getType() == IqlTokenTypes.OBJECTSTOREBAG) {
                        osb = processNewObjectStoreBag(subAST.getNextSibling().getFirstChild());
                        if (subAST.getNextSibling().getNextSibling() != null) {
                            throw new IllegalArgumentException("Expected no further data after "
                                    + "ObjectStoreBag");
                        }
                        return new BagConstraint(leftd, ConstraintOp.IN, osb);
                    } else {
                        throw new IllegalArgumentException("Invalid AST node for BagConstraint: "
                                + subAST.getNextSibling().getText());
                    }
                }
                Object nextParam = iterator.next();
                if (nextParam instanceof Collection<?>) {
                    return new BagConstraint(leftd, ConstraintOp.IN, (Collection<?>) nextParam);
                } else {
                    throw new ClassCastException("Parameter " + nextParam
                            + " not a Collection or ObjectStoreBag");
                }
            case IqlTokenTypes.NOT_CONSTRAINT:
                subAST = ast.getFirstChild();
                Constraint retval = processConstraint(subAST, q, modelPackage, iterator);
                retval.negate();
                return retval;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText() + " ["
                            + ast.getType() + "]");
        }
    }

    private static Constraint processSimpleConstraint(AST ast, Query q, String modelPackage,
            Iterator<?> iterator) {
        AST subAST = ast.getFirstChild();
        Object left = processNewQueryNodeOrReference(subAST, q, false, modelPackage, iterator);
        subAST = subAST.getNextSibling();
        ConstraintOp op = null;
        switch (subAST.getType()) {
            case IqlTokenTypes.EQ:
                op = ConstraintOp.EQUALS;
                break;
            case IqlTokenTypes.NOT_EQ:
                op = ConstraintOp.NOT_EQUALS;
                break;
            case IqlTokenTypes.LT:
                op = ConstraintOp.LESS_THAN;
                break;
            case IqlTokenTypes.LE:
                op = ConstraintOp.LESS_THAN_EQUALS;
                break;
            case IqlTokenTypes.GT:
                op = ConstraintOp.GREATER_THAN;
                break;
            case IqlTokenTypes.GE:
                op = ConstraintOp.GREATER_THAN_EQUALS;
                break;
            case IqlTokenTypes.LITERAL_like:
                op = ConstraintOp.MATCHES;
                break;
            case IqlTokenTypes.NOTLIKE:
                op = ConstraintOp.DOES_NOT_MATCH;
                break;
            case IqlTokenTypes.ISNULL:
                op = ConstraintOp.IS_NULL;
                break;
            case IqlTokenTypes.ISNOTNULL:
                op = ConstraintOp.IS_NOT_NULL;
                break;
            default:
                throw new IllegalArgumentException("Unknown AST node: " + ast.getText()
                        + " [" + ast.getType() + "]");
        }
        subAST = subAST.getNextSibling();
        if ((op == ConstraintOp.IS_NULL) || (op == ConstraintOp.IS_NOT_NULL)) {
            if (subAST != null) {
                throw new IllegalArgumentException("IS (NOT) NULL only takes one argument");
            } else if (left instanceof QueryClass) {
                throw new IllegalArgumentException("Cannot compare a class to null");
            } else if (left instanceof QueryObjectReference) {
                return new ContainsConstraint((QueryObjectReference) left, op);
            } else {
                return new SimpleConstraint((QueryEvaluable) left, op);
            }
        } else {
            if (subAST == null) {
                throw new IllegalArgumentException("Most simple constraints require two "
                        + "arguments");
            } else {
                if (IqlTokenTypes.QUESTION_MARK == subAST.getType()) {
                    if (left instanceof QueryClass) {
                        try {
                            if (op == ConstraintOp.EQUALS) {
                                return new ClassConstraint((QueryClass) left,
                                        ConstraintOp.EQUALS,
                                        (InterMineObject) iterator.next());
                            } else if (op == ConstraintOp.NOT_EQUALS) {
                                return new ClassConstraint((QueryClass) left,
                                        ConstraintOp.NOT_EQUALS,
                                        (InterMineObject) iterator.next());
                            } else {
                                throw new IllegalArgumentException("Operation is not valid for "
                                                                   + "comparing a class to an "
                                                                   + "object");
                            }
                        } catch (NoSuchElementException e) {
                            throw new IllegalArgumentException("Not enough parameters in "
                                                               + "IqlQuery object");
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot compare a field to an object");
                    }
                } else {
                    QueryNode right = processNewQueryNode(subAST, q, modelPackage, iterator);
                    if (left instanceof QueryClass) {
                        if (right instanceof QueryClass) {
                            if (op == ConstraintOp.EQUALS) {
                                return new ClassConstraint((QueryClass) left,
                                        ConstraintOp.EQUALS, (QueryClass) right);
                            } else if (op == ConstraintOp.NOT_EQUALS) {
                                return new ClassConstraint((QueryClass) left,
                                        ConstraintOp.NOT_EQUALS, (QueryClass) right);
                            } else {
                                throw new IllegalArgumentException("Operation is not valid for "
                                        + "comparing two classes");
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot compare a class to a "
                                    + "value");
                        }
                    } else if (left instanceof QueryEvaluable) {
                        if (right instanceof QueryClass) {
                            throw new IllegalArgumentException("Cannot compare a value to a "
                                    + "class");
                        } else {
                            return new SimpleConstraint((QueryEvaluable) left, op,
                                    (QueryEvaluable) right);
                        }
                    } else {
                        throw new IllegalArgumentException("Cannot compare a QueryObjectReference"
                                + " using a SimpleConstraint - use CONTAINS or DOES NOT CONTAIN"
                                + " instead");
                    }
                }
            }
        }
    }

    /**
     * Unescapes a String, by removing quotes from the beginning and end.
     *
     * @param word the String
     * @return the unescaped String
     */
    public static String unescape(String word) {
        if (word != null) {
            if ((word.charAt(0) == '"') && (word.charAt(word.length() - 1) == '"')) {
                return word.substring(1, word.length() - 1);
            } else if ((word.charAt(0) == '"') || (word.charAt(word.length() - 1) == '"')) {
                throw new IllegalArgumentException("Identifier " + word + " is not properly escaped"
                        + " by surrounding with double quotes");
            }
        }
        return word;
    }
}
