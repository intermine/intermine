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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Static methods to create Lists of Constraint objects in a query and
 * Constraints in a query relating to a given QueryClass, plus helper methods
 * for dealing with Constraints.
 * <br>
 * 'left' and 'right' arguments for each constraint type are defined as follows:
 * SimpleConstraint: left = arg1, right = arg2 (both QueryEvaluables)
 * ClassConstraint: left = QueryClass arg1, right = a QueryClass or example object arg2
 * ContainsConstraint: left = the QueryReference - i.e. field of containing class
 *                     right = the QueryClass this reference is constrained to
 * SubQueryConstraint: left = the QueryClass or QueryEvaluable constrained to be in
 *                            the subquery
 *                     right = the query
 * ConstraintSet: N/A
 * <br>
 * 
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class ConstraintHelper
{
    /**
     * Converts a constraint from a query into a List of Constraint objects.
     *
     * @param query a Query object to list Constraints for
     * @return a List of Constraint objects
     */
    public static List createList(Query query) {
        List retval = new ArrayList();
        if (query != null) {
            addToList(retval, query.getConstraint());
        }
        return retval;
    }


    /**
     * Return a List of Constraint objects that relate to the given FromElement.
     *
     * @param query a Query object to to list contraints for
     * @param fromElement a FromElement that returned constraints relate to
     * @return a List of Constraint objects
     */
    public static List createList(Query query, FromElement fromElement) {
        return filter(createList(query), fromElement, false);
    }

    /**
     * Traverse the given Constraint tree (ie. recursively look for nested constraints if the
     * argument is a ConstraintSet) and call ConstraintTraverseAction for each Constraint.
     * @param c the Constraint to traverse - could be a ConstraintSet (possibly with nested
     * ConstraintSets)
     * @param ca ConstraintTraverseAction.apply() is called for each Constraint found
     */
    public static void traverseConstraints(Constraint c, ConstraintTraverseAction ca) {
        ca.apply(c);
        if (c instanceof ConstraintSet) {
            Iterator iter = ((ConstraintSet) c).getConstraints().iterator();
            while (iter.hasNext()) {
                Constraint childConstraint = (Constraint) iter.next();
                traverseConstraints(childConstraint, ca);
            }
        }
    }

    /**
     * Return a List of Constraint objects that relate to the given FromElement.
     *
     * @param query a Query object to to list contraints for
     * @param fromElement a FromElement that returned constraints relate to
     * @return a List of Constraint objects
     */
    public static List listRelatedConstraints(Query query, FromElement fromElement) {
        return filter(createList(query), fromElement, true);
    }

    /**
     * Return a subset of the given List that contains only Constraints
     * that relate to the given FromElement or constraints associated
     * with nothing if fromElement is null.
     *
     * @param list a list of Constraints to filter
     * @param fromElement a fromElement that returned constraints relate to
     * @param related if tru list all releted constraints, otherwise just associated
     * @return a List of Constraint objects
     */
    public static List filter(List list, FromElement fromElement, boolean related) {
        List filtered = new ArrayList();
        Iterator iter = list.iterator();
        if (related) {
            while (iter.hasNext()) {
                Constraint c = (Constraint) iter.next();
                if (fromElement != null) {
                    if (isRelatedTo(c, fromElement)) {
                        filtered.add(c);
                    }
                } else if (isRelatedToNothing(c)) {
                    filtered.add(c);
                }
            }
        } else {
            while (iter.hasNext()) {
                Constraint c = (Constraint) iter.next();
                if (fromElement != null) {
                    if (isAssociatedWith(c, fromElement)) {
                        filtered.add(c);
                    }
                } else if (isAssociatedWithNothing(c)) {
                    filtered.add(c);
                }
            }
        }
        return filtered;
    }


    /**
     * Adds all the constraints present in the argument into the given List.
     *
     * @param list a List of Constraints, to which to add more entries
     * @param constraint a Constraint to pick apart
     */
    public static void addToList(List list, Constraint constraint) {
        if (constraint != null) {
            if (constraint instanceof ConstraintSet) {
                if (((ConstraintSet) constraint).getOp() == ConstraintOp.AND) {
                    Set constraints = ((ConstraintSet) constraint).getConstraints();
                    Iterator conIter = constraints.iterator();
                    while (conIter.hasNext()) {
                        addToList(list, (Constraint) conIter.next());
                    }
                } else {
                    list.add(constraint);
                }
            } else {
                list.add(constraint);
            }
        }
    }


    /**
     * Returns true if the constraint is associated with the given FromElement.
     * Associated with means relating directly to a field of the QueryClass but
     * NOT a cross-reference contraint (which compares two arbitrary fields of
     * different QueryClasses.
     *
     * @param constraint the constraint in question
     * @param fromElement the FromElement to check
     * @return true if associated
     */
    public static boolean isAssociatedWith(Constraint constraint, FromElement fromElement) {

        Object left = getLeftArgument(constraint);
        Object right = getRightArgument(constraint);

        // ignore cross-references
        if (left instanceof QueryEvaluable && !(isCrossReference(constraint))) {
            // not a cross-reference -> at most one QueryClass.  find it.
            QueryClass qc = null;  // TODO test for a bug here? left not assoc by right is
            if (getQueryFields((QueryEvaluable) left).iterator().hasNext()) {
                QueryField qf = (QueryField) getQueryFields((QueryEvaluable) left)
                    .iterator().next();
                qc = (QueryClass) qf.getFromElement();
            } else if (getQueryFields((QueryEvaluable) right).iterator().hasNext()) {
                QueryField qf = (QueryField) getQueryFields((QueryEvaluable) right)
                    .iterator().next();
                qc = (QueryClass) qf.getFromElement();
            } else {
                return false;   // does not relate to any QueryClass
            }
            return (fromElement == qc);
        } else if (left instanceof QueryClass && right instanceof QueryClass) {
            return (fromElement == left || fromElement == right);
        } else if (left instanceof QueryClass) {
            return (fromElement == left);
        } else if (left instanceof QueryReference) {
            return (fromElement == ((QueryReference) left).getQueryClass());
        } else if (right instanceof Query) {
            return (fromElement == right);
        }
        return false;
    }


    /**
     * Returns true if the given constraint is associated with no particular FromElement.
     * @param c the constraint to examine
     * @return true if constraint is not associated with a FromElement
    */
    public static boolean isAssociatedWithNothing(Constraint c) {
        Object left = getLeftArgument(c);
        Object right = getRightArgument(c);

        if (isCrossReference(c)) {
            return true;
        } else if (left instanceof QueryEvaluable) {
            if (getQueryFields((QueryEvaluable) left).size() > 0) {
                return false;
            } else if (right instanceof QueryEvaluable) {
                return (getQueryFields((QueryEvaluable) left).size() > 0);
            }
        } else if (left instanceof QueryClass) {
            return false;
        } else if (left instanceof QueryReference) {
            return false;
        }
        return true;
    }


    /**
     * Returns true if the constraint is associated with the given FromElement.
     *
     * @param constraint the constraint in question
     * @param fromElement the FromElement to check
     * @return true if associated
     */
    public static boolean isRelatedTo(Constraint constraint, FromElement fromElement) {
        if (isAssociatedWith(constraint, fromElement)) {
            return true;
        }

        // also want to find out if this is referred to in a Contains or Subquery
        // constratint that is associated with another fromElement.

        if (constraint instanceof ContainsConstraint) {
            if (fromElement == ((ContainsConstraint) constraint).getQueryClass()) {
                return true;
            }
        } else if (constraint instanceof SubqueryConstraint) {
            if (fromElement == ((SubqueryConstraint) constraint).getQuery()) {
                return true;
            }
        } else if (constraint instanceof SimpleConstraint) {
            SimpleConstraint sc = (SimpleConstraint) constraint;
            Set qFields = (getQueryFields(sc.getArg1()));
            qFields.addAll(getQueryFields(sc.getArg2()));
            Iterator iter = qFields.iterator();
            while (iter.hasNext()) {
                if (fromElement == ((QueryField) iter.next()).getFromElement()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Returns true if the given constraint is related to no FromElement.  This should
     * only return true if c is a SimpleConstraint that only references constants.
     *
     * @param c the constraint to examine
     * @return true if constraint is not associated with a FromElement
    */
    public static boolean isRelatedToNothing(Constraint c) {
        if (c instanceof SimpleConstraint) {
            SimpleConstraint sc = (SimpleConstraint) c;
            Set fields = getQueryFields(sc.getArg1());
            fields.addAll(getQueryFields(sc.getArg2()));
            if (fields.size() == 0) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns true if the Constraint is a cross-reference between two QueryClasses.
     * A constraint is deemed to be a cross-reference if it compares fields of
     * two different QueryClasses, either directly or via QueryExpressions.
     *
     * @param constraint the contraint to test
     * @return true if the contraint is a cross-reference
     */
    public static boolean isCrossReference(Constraint constraint) {
        if (constraint instanceof SimpleConstraint) {
            // if QueryField exposed part of a subquery QueryField.getFromElement()
            // returns a query, does not cause any problem.
            Set qcs = new HashSet();
            Iterator leftIter = getQueryFields(((SimpleConstraint) constraint).getArg1())
                .iterator();
            while (leftIter.hasNext()) {
                qcs.add(((QueryField) leftIter.next()).getFromElement());
            }
            Iterator rightIter = getQueryFields(((SimpleConstraint) constraint).getArg2())
                .iterator();
            while (rightIter.hasNext()) {
                qcs.add(((QueryField) rightIter.next()).getFromElement());
            }
            if (qcs.size() > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Descends into QueryExpression and QueryFunction objects to find
     * all instances of QueryField.  Will return a single element
     * set for QueryField and an empty set for QueryValue.
     *
     * @param qe a QueryEvalubale to find QueryFields for
     * @return a set of QueryFields
     */
    protected static Set getQueryFields(QueryEvaluable qe) {
        Set fields = new HashSet();

        if (qe instanceof QueryField) {
            fields.add((QueryField) qe);
        } else if (qe instanceof QueryFunction) {
            fields.addAll(getQueryFields(((QueryFunction) qe).getParam()));
        } else if (qe instanceof QueryExpression) {
            fields.addAll(getQueryFields(((QueryExpression) qe).getArg1()));
            fields.addAll(getQueryFields(((QueryExpression) qe).getArg2()));
            fields.addAll(getQueryFields(((QueryExpression) qe).getArg3()));
        }
        return fields;
    }



    /**
     * Get the left argument of the given constraint, will return null
     * if passed a ConstraintSet.
     * @param constraint a constraint
     * @return the left argument of given constraint
     */
    public static Object getLeftArgument(Constraint constraint) {
        Object left;
        if (constraint instanceof ConstraintSet) {
            left = null;
        } else if (constraint instanceof ClassConstraint) {
            left = ((ClassConstraint) constraint).getArg1();
        } else if (constraint instanceof ContainsConstraint) {
            left = ((ContainsConstraint) constraint).getReference();
        } else if (constraint instanceof SimpleConstraint) {
            left = ((SimpleConstraint) constraint).getArg1();
        } else if (constraint instanceof BagConstraint) {
            left = ((BagConstraint) constraint).getQueryNode();
        } else if (constraint instanceof SubqueryConstraint) {
            left = ((SubqueryConstraint) constraint).getQueryEvaluable();
            if (left == null) {
                left = ((SubqueryConstraint) constraint).getQueryClass();
            }
        } else {
            throw new IllegalArgumentException("Unknown Constraint type: "
                                               + constraint.getClass().getName());
        }
        return left;
    }


    /**
     * Get the right argument of the given constraint, will return null
     * if passed a ConstraintSet.
     * @param constraint a constraint
     * @return the right argument of given constraint
     */
    public static Object getRightArgument(Constraint constraint) {
        Object right;
        if (constraint instanceof ConstraintSet) {
            right = null;
        } else if (constraint instanceof ClassConstraint) {
            right = ((ClassConstraint) constraint).getArg2QueryClass();
            if (right == null) {
                right = ((ClassConstraint) constraint).getArg2Object();
            }
        } else if (constraint instanceof ContainsConstraint) {
            right = ((ContainsConstraint) constraint).getQueryClass();
        } else if (constraint instanceof SimpleConstraint) {
            right = ((SimpleConstraint) constraint).getArg2();
        } else if (constraint instanceof SubqueryConstraint) {
            right = ((SubqueryConstraint) constraint).getQuery();
        } else if (constraint instanceof BagConstraint) {
            right = ((BagConstraint) constraint).getBag();
            if (right == null) {
                right = ((BagConstraint) constraint).getOsb();
            }
        } else {
            throw new IllegalArgumentException("Unknown Constraint type: "
                                               + constraint.getClass().getName());
        }
        return right;
    }
}
