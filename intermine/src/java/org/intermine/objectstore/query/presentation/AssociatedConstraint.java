package org.flymine.objectstore.query.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;

/**
 * Printable representation of a query constraint.
 *
 * @author Matthew Wakeling
 */
public class AssociatedConstraint extends PrintableConstraint
{
    /**
     * Constructor, takes a Query and a Constraint.
     *
     * @param query a Query with which to look up aliases
     * @param constraint a Constraint to encapsulate
     */
    public AssociatedConstraint(Query query, Constraint constraint) {
        if (constraint instanceof ConstraintSet) {
            throw new IllegalArgumentException("A ConstraintSet is not associated with a particular"
                    + " FromElement.");
        }
        this.query = query;
        this.constraint = constraint;
    }

    /**
     * @see PrintableConstraint#isAssociatedWith
     */
    public boolean isAssociatedWith(FromElement fromElement) {
        Object left = getLeftArgument();
        if (left instanceof QueryField) {
            return (fromElement == ((QueryField) left).getFromElement());
        } else if (left instanceof QueryClass) {
            return (fromElement == left);
        } else if (left instanceof QueryReference) {
            return (fromElement == ((QueryReference) left).getQueryClass());
        }
        return false;
    }

    /**
     * @see PrintableConstraint#isAssociatedWithNothing
     */
    public boolean isAssociatedWithNothing() {
        Object left = getLeftArgument();
        if (left instanceof QueryField) {
            return false;
        } else if (left instanceof QueryClass) {
            return false;
        } else if (left instanceof QueryReference) {
            return false;
        }
        return true;
    }

    /**
     * Returns the text of the left-hand side of this constraint, omitting any FromElement alias.
     *
     * @return a String
     */
    public String getLeft() {
        Object left = getLeftArgument();
        if (left instanceof QueryField) {
            QueryField qf = (QueryField) left;
            return qf.getFieldName() + (qf.getSecondFieldName() == null ? "" : "."
                    + qf.getSecondFieldName());
        } else if (left instanceof QueryNode) {
            return FqlQuery.nodeToString(query, (QueryNode) left);
        } else if (left instanceof QueryReference) {
            return ((QueryReference) left).getFieldName();
        }
        throw new IllegalArgumentException("Unknown left argument type");
    }

    private Object getLeftArgument() {
        Object left;
        if (constraint instanceof ClassConstraint) {
            left = ((ClassConstraint) constraint).getArg1();
        } else if (constraint instanceof ContainsConstraint) {
            left = ((ContainsConstraint) constraint).getReference();
        } else if (constraint instanceof SimpleConstraint) {
            left = ((SimpleConstraint) constraint).getArg1();
        } else if (constraint instanceof SubqueryConstraint) {
            left = ((SubqueryConstraint) constraint).getQueryEvaluable();
            if (left == null) {
                left = ((SubqueryConstraint) constraint).getQueryClass();
            }
        } else {
            throw new IllegalArgumentException("Unknown Constraint type");
        }
        return left;
    }

    /**
     * Returns the text of the operation of the constraint.
     *
     * @return a String
     */
    public String getOp() {
        if (constraint instanceof ClassConstraint) {
            return (((ClassConstraint) constraint).isNotEqual() ? "!=" : "=");
        } else if (constraint instanceof ContainsConstraint) {
            return (((ContainsConstraint) constraint).isNotContains() ? "DOES NOT CONTAIN"
                    : "CONTAINS");
        } else if (constraint instanceof SimpleConstraint) {
            return ((SimpleConstraint) constraint).getType().toString();
        } else if (constraint instanceof SubqueryConstraint) {
            return (((SubqueryConstraint) constraint).isNotIn() ? "IS NOT IN" : "IN");
        }
        throw new IllegalArgumentException("Unknown Constraint type");
    }

    /**
     * Returns the text of the right-hand side of this constraint.
     *
     * @return a String
     */
    public String getRight() {
        if (constraint instanceof ClassConstraint) {
            ClassConstraint con = (ClassConstraint) constraint;
            QueryClass qc = con.getArg2QueryClass();
            return (qc == null ? "\"" + con.getArg2Object() + "\""
                    : FqlQuery.nodeToString(query, qc));
        } else if (constraint instanceof ContainsConstraint) {
            return FqlQuery.nodeToString(query, ((ContainsConstraint) constraint).getQueryClass());
        } else if (constraint instanceof SimpleConstraint) {
            return FqlQuery.nodeToString(query, ((SimpleConstraint) constraint).getArg2());
        } else if (constraint instanceof SubqueryConstraint) {
            return ((SubqueryConstraint) constraint).getQuery().toString();
        }
        throw new IllegalArgumentException("Unknown Constraint type");
    }
}
