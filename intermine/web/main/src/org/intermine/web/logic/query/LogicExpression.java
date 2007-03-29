package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.intermine.web.LogicLexer;
import org.intermine.web.LogicParser;

import antlr.collections.AST;

/**
 * In memory representation of constraint logic expression. Parses the expression
 * provided to the constructor. Use the toString method to convert the expression
 * back to text. An IllegalArgumentException will be thrown from the constructor
 * if a parse error occurs (use the cause exception to find out why).
 * 
 * @author Thomas Riley
 * @see org.intermine.web.logic.query.PathQuery
 */
public class LogicExpression
{
    /** The abstract syntax tree produced by the parser. */
    private AST ast;
    /** Root node - always an operator. */
    private Node root;
    
    /**
     * Create a new instance of LogicExpression parsing the given
     * expression.
     * @param expression the logic expression
     * @throws IllegalArgumentException if parse error occurs
     */
    public LogicExpression(String expression) throws IllegalArgumentException {
        root = parse(expression);
    }
    
    /**
     * Parse a logic expression.
     * @param expression logic expression
     */
    private Node parse(String expression) {
        try {
            LogicLexer lexer = new LogicLexer(new StringReader(expression));
            LogicParser parser = new LogicParser(lexer);
            Node root;
            // The root context
            parser.expr();
            ast = parser.getAST();
            //new antlr.DumpASTVisitor().visit(ast);
            if (ast.getText().equals("or")) {
                root = new Or(ast, true);
            } else if (ast.getText().equals("and")) {
                root = new And(ast, true);
            } else {
                root = new Variable(ast.getText());
            }
            return root;
        } catch (antlr.RecognitionException e) {
            new antlr.DumpASTVisitor().visit(ast);
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (antlr.TokenStreamException e) {
            new antlr.DumpASTVisitor().visit(ast);
            IllegalArgumentException e2 = new IllegalArgumentException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            new antlr.DumpASTVisitor().visit(ast);
            throw e;
        }
    }

    /**
     * Get the expression as a string.
     * @return expression as string
     */
    public String toString() {
        return root.toString();
    }
    
    /**
     * Get the root node.
     * @return the root node
     */
    public Node getRootNode() {
        return root;
    }
    
    /**
     * Remove a variable from the expression.
     * @param name variable to remove
     */
    public void removeVariable(String name) {
        removeVariable(name, root);
        String logic = toString();
        root = parse(logic);
    }

    /**
     * Remove a variable from a branch of the tree.
     * @param name variable name
     * @param node root of subtree
     */
    private void removeVariable(String name, Node node) {
        for (Iterator iter = new LinkedHashSet(node.getChildren()).iterator(); iter.hasNext(); ) {
            Node child = (Node) iter.next();
            if (child instanceof Variable && ((Variable) child).getName().equals(name)) {
                node.removeChild(child);
            } else {
                removeVariable(name, child);
            }
        }
    }
    
    /**
     * Remove any variables that aren't in the given set.
     * @param variables set of variable names
     */
    public void removeAllVariablesExcept(Set variables) {
        removeAllVariablesExcept(variables, root);
        String logic = toString();
        root = parse(logic);
    }
    
    /**
     * Remove any variables that aren't in the given set.
     * @param variables set of variable names
     * @param node root of subtree
     */
    private void removeAllVariablesExcept(Set variables, Node node) {
        for (Iterator iter = new LinkedHashSet(node.getChildren()).iterator(); iter.hasNext(); ) {
            Node child = (Node) iter.next();
            if (child instanceof Variable && !variables.contains(((Variable) child).getName())) {
                node.removeChild(child);
            } else {
                removeAllVariablesExcept(variables, child);
            }
        }
    }
    
    /**
     * Get the Set of variable names.
     * @return set of variable names in this expression
     */
    public Set getVariableNames() {
        Set variables = new HashSet();
        getVariableNames(variables, root);
        return variables;
    }
    
    private void getVariableNames(Set variables, Node node) {
        for (Iterator iter = new LinkedHashSet(node.getChildren()).iterator(); iter.hasNext(); ) {
            Node child = (Node) iter.next();
            if (child instanceof Variable) {
                variables.add(((Variable) child).getName());
            } else {
                getVariableNames(variables, child);
            }
        }
    }

    /**
     * Node of parse tree.
     */
    public abstract class Node
    {
        private Set children = new LinkedHashSet();
        
        private Node(AST ast) {
            if (ast != null) {
                AST child = ast.getFirstChild();
                while (child != null) {
                    if (child.getText().equals("or")) {
                        children.add(new Or(child));
                    } else if (child.getText().equals("and")) {
                        children.add(new And(child));
                    } else {
                        children.add(new Variable(child.getText()));
                    }
                    child = child.getNextSibling();
                }
            }
        }
        
        /**
         * Get an unmodifiable copy of the node's children.
         * @return unmodifiable set of node children
         */
        public Set getChildren() {
            return Collections.unmodifiableSet(children);
        }
        
        private void removeChild(Node child) {
            children.remove(child);
        }
    }
    
    /**
     * An operator node.
     */
    public abstract class Operator extends Node
    {
        boolean root = false;
        
        private Operator(AST ast, boolean root) {
            super(ast);
            this.root = root;
        }
                
        private Operator(AST ast) {
            this(ast, false);
        }
        
        /**
         * Override to provide text symbol for this operator. Used in toString.
         * @return operator name
         */
        protected abstract String getOperator();
        
        /**
         * Produce an expression for this branch of the tree.
         * @return expression representing this branch
         */
        public String toString() {
            String expr = "";
            Iterator iter = getChildren().iterator();
            while (iter.hasNext()) {
                if (expr.length() > 0) {
                    expr += " " + getOperator() + " ";
                }
                Node child = (Node) iter.next();
                String subexpr = child.toString();
                if (child instanceof Or && this instanceof And) {
                    subexpr = "(" + subexpr + ")";
                }
                expr += subexpr;
            }
            //if (!root && !getOperator().equals("and")) {
            //    expr = "(" + expr + ")";
            //}
            return expr;
        }        
    }
    
    /**
     * An AND operator node.
     */
    public class And extends Operator
    {
        private And(AST ast, boolean root) {
            super(ast, root);
        }

        private And(AST ast) {
            this(ast, false);
        }
        
        /** 
         * @see Operator#getOperator()
         */
        protected String getOperator() {
            return "and";
        }
    }
    
    /**
     * An OR operator node.
     */
    public class Or extends Operator
    {
        private Or(AST ast, boolean root) {
            super(ast, root);
        }
        
        private Or(AST ast) {
            this(ast, false);
        }
        
        /** 
         * @see Operator#getOperator()
         */
        protected String getOperator() {
            return "or";
        }
    }
    
    /**
     * A variable node.
     */
    public class Variable extends Node
    {
        private String name;
        
        private Variable(String name) {
            super(null);
            this.name = name;
        }
        
        /**
         * Get variable name.
         * @return variable name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Just returns the variable name.
         * @return string representation of this node
         */
        public String toString() {
            return getName();
        }
    }
}
