package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;

import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.testing.OneTimeTestCase;

public class QueryTestCase extends OneTimeTestCase
{
    public QueryTestCase(String arg1) {
        super(arg1);
    }

    protected void assertEquals(Query q1, Query q2) {
        assertEquals("asserting equal", q1, q2);
    }

    /**
     * The absence of a proper Query.equals() method means that we
     * have to do various tests here. This does rely on constructing
     * the query in the tests in the correct form, ie. we will not
     * notice that an "OR" ConstraintSet with one SimpleConstraint in
     * it is the same as a SimpleConstraint.
     */
    protected void assertEquals(String msg, Query q1, Query q2) {
        if ((q1 != null) && (q2 != null)) {
            msg += ": expected <" + q1.toString() + "> but was <" + q2.toString() + ">";
            //msg += ": q1 = " + q1.toString() + ", q2 = " + q2.toString();

            // Are the SELECT lists equal?
            checkQueryClassLists(msg + ": SELECT lists are not equal", q1.getSelect(), q2.getSelect(), q1, q2);
            // Are the FROM lists equal?
            checkQueryClassLists(msg + ": FROM lists are not equal", q1.getFrom(), q2.getFrom(), q1, q2);
            // Are the constraints equal?
            checkConstraints(msg + ": CONSTRAINTS not the same", q1.getConstraint(), q2.getConstraint(), q1, q2);
            // Are the ORDER BY lists equal?
            checkQueryClassLists(msg + ": ORDER BY lists are not equal", q1.getOrderBy(), q2.getOrderBy(), q1, q2);
            assertEquals(msg + ": LIMIT is the not the same", q1.getLimit(), q2.getLimit());
            // Do the toString methods return the same thing?
            checkToString(msg + ": toString does not return the same String", q1, q2);
        } else if ((q1 == null) && (q2 == null)) {
            // They are equal - albeit null.
        } else {
            assertNotNull(msg + ": q1 is null, while q2 is not null", q1);
            fail(msg + ": q2 is null, while q1 is not null");
        }
    }

    protected void checkQueryClassLists(String msg, Collection l1, Collection l2, Query q1, Query q2) {
        assertEquals(msg + ": lists are different sizes", l1.size(), l2.size());
        Iterator i1 = l1.iterator();
        Iterator i2 = l2.iterator();
        while (i1.hasNext()) {
            Object qc1 = i1.next();
            Object qc2 = i2.next();

            checkObjects(msg, qc1, qc2, q1, q2);
        }
    }

    protected void checkObjects(String msg, Object qc1, Object qc2, Query q1, Query q2) {
        if ((qc1 == null) && (qc2 == null)) {
            return;
        }
        if (qc1 instanceof QueryNode) {
            if (qc2 instanceof QueryNode) {
                checkQueryNodes(msg + ": query nodes are not the same", (QueryNode) qc1, (QueryNode) qc2, q1, q2);
            } else {
                fail(msg + ": QueryNode does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof Query) {
            if (qc2 instanceof Query) {
                assertEquals(msg + ": subquery", (Query) qc1, (Query) qc2);
            } else {
                fail(msg + ": Subquery does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof QueryClassBag) {
            if (qc2 instanceof QueryClassBag) {
                checkQueryClassBags(msg + ": QueryClassBags are not equivalent", (QueryClassBag) qc1, (QueryClassBag) qc2, q1, q2);
            } else {
                fail(msg + ": QueryClassBag does not match  " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof PathExpressionField) {
            if (qc2 instanceof PathExpressionField) {
                PathExpressionField pef1 = (PathExpressionField) qc1;
                PathExpressionField pef2 = (PathExpressionField) qc2;
                checkObjects(msg + ": PathExpressionField qopes are not equal", pef1.getQope(), pef2.getQope(), q1, q2);
                assertEquals(msg + ": PathExpressionField column numbers are not equal", pef1.getFieldNumber(), pef2.getFieldNumber());
            }
        } else if (qc1 instanceof QueryObjectPathExpression) {
            if (qc2 instanceof QueryObjectPathExpression) {
                QueryObjectPathExpression pe1 = (QueryObjectPathExpression) qc1;
                QueryObjectPathExpression pe2 = (QueryObjectPathExpression) qc2;
                checkQueryNodes(msg + ": QueryClasses of QueryObjectPathExpressions don't match", pe1.getQueryClass(), pe2.getQueryClass(), q1, q2);
                assertEquals(msg + ": QueryObjectPathExpression fieldnames are not equal", pe1.getFieldName(), pe2.getFieldName());
                checkQueryClassLists(msg + ": QueryObjectPathExpression SELECT lists are not equal", pe1.getSelect(), pe2.getSelect(), q1, q2);
                checkConstraints(msg + ": QueryObjectPathExpression constraints are not equal", pe1.getConstraint(), pe2.getConstraint(), q1, q2);
            } else {
                fail(msg + ": QueryObjectPathExpression does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof QueryCollectionPathExpression) {
            if (qc2 instanceof QueryCollectionPathExpression) {
                QueryCollectionPathExpression pe1 = (QueryCollectionPathExpression) qc1;
                QueryCollectionPathExpression pe2 = (QueryCollectionPathExpression) qc2;
                checkQueryNodes(msg + ": QueryClasses of QueryCollectionPathExpressions don't match", pe1.getQueryClass(), pe2.getQueryClass(), q1, q2);
                assertEquals(msg + ": QueryCollectionPathExpression fieldNames are not equal", pe1.getFieldName(), pe2.getFieldName());
                checkQueryClassLists(msg + ": QueryCollectionPathExpression SELECT lists are not equal", pe1.getSelect(), pe2.getSelect(), q1, q2);
                checkQueryClassLists(msg + ": QueryCollectionPathExpression FROM lists are not equal", pe1.getFrom(), pe2.getFrom(), q1, q2);
                checkConstraints(msg + ": QueryCollectionPathExpression constraints are not equal", pe1.getConstraint(), pe2.getConstraint(), q1, q2);
                assertEquals(msg + ": QueryCollectionPathExpression isSingleton is not equal", pe1.isSingleton(), pe2.isSingleton());
            } else {
                fail(msg + ": QueryCollectionPathExpression does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof ObjectStoreBag) {
            if (qc2 instanceof ObjectStoreBag) {
                if (((ObjectStoreBag) qc1).getBagId() != ((ObjectStoreBag) qc2).getBagId()) {
                    fail(msg + ": ObjectStoreBag ids are not equal");
                }
            } else {
                fail(msg + ": ObjectStoreBag does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof OrderDescending) {
            if (qc2 instanceof OrderDescending) {
                checkQueryNodes(msg + ": OrderDescending element does not match", ((OrderDescending) qc1).getQueryOrderable(), ((OrderDescending) qc2).getQueryOrderable(), q1, q2);
            } else {
                fail(msg + ": OrderDescending does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof QueryReference) {
            if (qc2 instanceof QueryReference) {
                checkQueryReferences(msg, (QueryReference) qc1, (QueryReference) qc2, q1, q2);
            } else {
                fail(msg + ": QueryReference does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof ObjectStoreBagCombination) {
            if (qc2 instanceof ObjectStoreBagCombination) {
                if (!qc1.equals(qc2)) {
                    fail(msg + ": ObjectStoreBagCombinations are not equal");
                }
            } else {
                fail(msg + ": ObjectStoreBagCombination does not match " + qc2.getClass().getName());
            }
        } else if (qc1 instanceof ObjectStoreBagsForObject) {
            if (qc2 instanceof ObjectStoreBagsForObject) {
                if (!qc1.equals(qc2)) {
                    fail(msg + ": ObjectStoreBagsForObjects are not equal");
                }
            } else {
                fail(msg + ": ObjectStoreBagsForObject does not match " + qc2.getClass().getName());
            }
        } else {
            fail(msg + ": Unknown type of Object in list: " + qc1.getClass().getName());
        }
    }

    protected void checkQueryNodes(String msg, Object qn1, Object qn2, Query q1, Query q2) {
        if ((qn1 == null) && (qn2 == null)) {
            return;
        }

        assertNotNull(msg + ": first QueryNode is null, second isn't", qn1);
        assertNotNull(msg + ": first QueryNode is not null, second is", qn2);

        assertEquals(msg + ": QueryNodes are not the same class", qn1.getClass(), qn2.getClass());

        if (qn1 instanceof QueryClass) {
            QueryClass qc1 = (QueryClass) qn1;
            QueryClass qc2 = (QueryClass) qn2;

            assertEquals(msg + ": QueryClasses do not refer to the same class", qc1.getType(), qc2.getType());
            assertEquals(msg + ": QueryClasses do not have the same alias", q1.getAliases().get(qc1),
                         q2.getAliases().get(qc2));
        } else if (qn1 instanceof QueryField) {
            QueryField qf1 = (QueryField) qn1;
            QueryField qf2 = (QueryField) qn2;
            FromElement fe1 = qf1.getFromElement();
            FromElement fe2 = qf2.getFromElement();

            if (fe1 instanceof QueryClass) {
                if (fe2 instanceof QueryClass) {
                    checkQueryNodes(msg, (QueryClass) fe1, (QueryClass) fe2, q1, q2);
                } else {
                    fail(msg + ": field member of QueryClass does not match field member of subquery");
                }
            } else {
                if (fe2 instanceof QueryClass) {
                    fail(msg + ": field member of subquery does not match field member of QueryClass");
                } else {
                    String alias1 = (String) q1.getAliases().get(fe1);
                    String alias2 = (String) q2.getAliases().get(fe2);
                    assertNotNull(msg + ": alias1 is null - " + q1.getAliases() + ", " + fe1, alias1);
                    assertNotNull(msg + ": alias2 is null - " + q2.getAliases() + ", " + fe2, alias2);
                    assertEquals(msg + ": field members of different subquery aliases", q1.getAliases().get(fe1), q2.getAliases().get(fe2));
                }
            }
            assertEquals(msg + ": QueryField fieldnames are not equal", qf1.getFieldName(), qf2.getFieldName());
        } else if (qn1 instanceof QueryValue) {
            QueryValue qv1 = (QueryValue) qn1;
            QueryValue qv2 = (QueryValue) qn2;

            assertEquals(msg + ": QueryValues are not equal", qv1, qv2);
        } else if (qn1 instanceof QueryFunction) {
            QueryFunction qf1 = (QueryFunction) qn1;
            QueryFunction qf2 = (QueryFunction) qn2;

            checkQueryNodes(msg + ": parameters are not equal", qf1.getParam(), qf2.getParam(), q1, q2);
            assertEquals(msg + ": functions are not the same", qf1.getOperation(), qf2.getOperation());
        } else if (qn1 instanceof QueryExpression) {
            QueryExpression qe1 = (QueryExpression) qn1;
            QueryExpression qe2 = (QueryExpression) qn2;

            assertEquals(msg + ": type of QueryExpressions are not the same", qe1.getType(), qe2.getType());

            checkQueryNodes(msg + ": first QueryEvaluables are not equal", qe1.getArg1(), qe2.getArg1(), q1, q2);
            checkQueryNodes(msg + ": second QueryEvaluables are not equal", qe1.getArg2(), qe2.getArg2(), q1, q2);
            checkQueryNodes(msg + ": third QueryEvaluables are not equal", qe1.getArg3(), qe2.getArg3(), q1, q2);

        }

    }

    protected void checkConstraints(String msg, Constraint c1, Constraint c2, Query q1, Query q2) {
        if ((c1 == null) && (c2 == null)) {
            return;
        }

        assertNotNull(msg + ": first Constraint is null, second isn't", c1);
        assertNotNull(msg + ": first Constraint is not null, second is", c2);

        // Check that the Constraints are the same class
        assertEquals(msg + ": Constraints are not the same class", c1.getClass(), c2.getClass());
        assertEquals(msg + ": Constraints don't have the same op", c1.getOp(), c2.getOp());

        if (c1 instanceof ConstraintSet) {
            ConstraintSet cs1 = (ConstraintSet) c1;
            ConstraintSet cs2 = (ConstraintSet) c2;

            // Check that the size of the ConstraintSets are the same
            assertEquals(msg + ": ConstraintSets are not the same size",
                         cs1.getConstraints().size(), cs2.getConstraints().size());

            // Iterate through the constraints and call this method
            Iterator i1 = cs1.getConstraints().iterator();
            Iterator i2 = cs2.getConstraints().iterator();
            while (i1.hasNext()) {
                Constraint con1 = (Constraint) i1.next();
                Constraint con2 = (Constraint) i2.next();
                checkConstraints(msg, con1, con2, q1, q2);
            }
        } else if (c1 instanceof SimpleConstraint) {
            SimpleConstraint sc1 = (SimpleConstraint) c1;
            SimpleConstraint sc2 = (SimpleConstraint) c2;
            checkQueryNodes(msg + ": first QueryEvaluables are not equal", sc1.getArg1(), sc2.getArg1(), q1, q2);
            checkQueryNodes(msg + ": first QueryEvaluables are not equal", sc1.getArg2(), sc2.getArg2(), q1, q2);
        } else if (c1 instanceof ClassConstraint) {
            ClassConstraint cc1 = (ClassConstraint) c1;
            ClassConstraint cc2 = (ClassConstraint) c2;

            checkQueryNodes(msg + ": first QueryClasses are not equal", cc1.getArg1(), cc2.getArg1(), q1, q2);
            if (cc1.getArg2QueryClass() != null) {
                checkQueryNodes(msg + ": second QueryClasses are not equal", cc1.getArg2QueryClass(),
                                cc2.getArg2QueryClass(), q1, q2);
            } else {
                assertEquals(msg + ": second objects in ClassConstraint not equal",
                             cc1.getArg2Object(), cc2.getArg2Object());
            }
        } else if (c1 instanceof ContainsConstraint) {
            ContainsConstraint cc1 = (ContainsConstraint) c1;
            ContainsConstraint cc2 = (ContainsConstraint) c2;
            checkQueryReferences(msg + ": QueryReferences are not equal", cc1.getReference(), cc2.getReference(), q1, q2);
            checkQueryNodes(msg + ": QueryClasses are not equal", cc1.getQueryClass(), cc2.getQueryClass(), q1, q2);
        } else if (c1 instanceof SubqueryConstraint) {
            SubqueryConstraint cc1 = (SubqueryConstraint) c1;
            SubqueryConstraint cc2 = (SubqueryConstraint) c2;

            QueryNode node1 = cc1.getQueryEvaluable();
            if (node1 == null) {
                node1 = cc1.getQueryClass();
            }
            QueryNode node2 = cc2.getQueryEvaluable();
            if (node2 == null) {
                node2 = cc2.getQueryClass();
            }
            checkQueryNodes(msg + ": nodes of subquery constraint are not equal", node1, node2, q1, q2);
            assertEquals(msg + ": queries of subquery constraint are not equal", cc1.getQuery(), cc2.getQuery());
        } else if (c1 instanceof BagConstraint) {
            BagConstraint cc1 = (BagConstraint) c1;
            BagConstraint cc2 = (BagConstraint) c2;
            checkQueryNodes(msg + ": BagConstraint nodes are not equal", cc1.getQueryNode(), cc2.getQueryNode(), q1, q2);
            assertEquals(msg + ": Bags are not equal", cc1.getBag(), cc2.getBag());
            assertEquals(msg + ": OSBs are not equal", cc1.getOsb(), cc2.getOsb());
        } else if (c1 instanceof SubqueryExistsConstraint) {
            SubqueryExistsConstraint cc1 = (SubqueryExistsConstraint) c1;
            SubqueryExistsConstraint cc2 = (SubqueryExistsConstraint) c2;

            assertEquals(msg + ": queries of subquery exists constraint are not equal", cc1.getQuery(), cc2.getQuery());
        } else if (c1 instanceof OverlapConstraint) {
            OverlapConstraint oc1 = (OverlapConstraint) c1;
            OverlapConstraint oc2 = (OverlapConstraint) c2;
            checkQueryNodes(msg + ": left start is not equal", oc1.getLeft().getStart(), oc2.getLeft().getStart(), q1, q2);
            checkQueryNodes(msg + ": left end is not equal", oc1.getLeft().getEnd(), oc2.getLeft().getEnd(), q1, q2);
            checkQueryReferences(msg + ": left parent is not equal", oc1.getLeft().getParent(), oc2.getLeft().getParent(), q1, q2);
            checkQueryNodes(msg + ": right start is not equal", oc1.getRight().getStart(), oc2.getRight().getStart(), q1, q2);
            checkQueryNodes(msg + ": right end is not equal", oc1.getRight().getEnd(), oc2.getRight().getEnd(), q1, q2);
            checkQueryReferences(msg + ": right parent is not equal", oc1.getRight().getParent(), oc2.getRight().getParent(), q1, q2);
        } else if (c1 instanceof MultipleInBagConstraint) {
            MultipleInBagConstraint bc1 = (MultipleInBagConstraint) c1;
            MultipleInBagConstraint bc2 = (MultipleInBagConstraint) c2;
            assertEquals(msg + ": evaluable list sizes are not the same", bc1.getEvaluables().size(), bc2.getEvaluables().size());
            Iterator<? extends QueryEvaluable> iter1 = bc1.getEvaluables().iterator();
            Iterator<? extends QueryEvaluable> iter2 = bc2.getEvaluables().iterator();
            while (iter1.hasNext()) {
                checkQueryNodes(msg + ": evaluable is not the same", iter1.next(), iter2.next(), q1, q2);
            }
            assertEquals(msg + ": bags are not equal", bc1.getBag(), bc2.getBag());
        } else {
            fail(msg + ": non-supported object in Query");
        }
    }

    protected void checkQueryReferences(String msg, QueryReference qr1, QueryReference qr2, Query q1, Query q2) {
        if ((qr1.getQueryClass() != null) || (qr2.getQueryClass() != null)) {
            checkQueryNodes(msg, qr1.getQueryClass(), qr2.getQueryClass(), q1, q2);
        } else {
            assertEquals(msg + ": collection origin is not equal", ((QueryCollectionReference) qr1).getQcObject(), ((QueryCollectionReference) qr2).getQcObject());
        }
        assertEquals(msg + ": QueryReference types are not equal", qr1.getType(), qr2.getType());
        assertEquals(msg + ": QueryReference fieldnames are not equal", qr1.getFieldName(), qr2.getFieldName());
    }

    protected void checkToString(String msg, Query q1, Query q2) {
        IqlQuery fq1 = q1.getIqlQuery();
        IqlQuery fq2 = q2.getIqlQuery();
        assertEquals(msg, fq1.getQueryString(), fq2.getQueryString());
        assertEquals(msg, fq1.getParameters(), fq2.getParameters());
    }

    protected void checkQueryClassBags(String msg, QueryClassBag qcb1, QueryClassBag qcb2, Query q1, Query q2) {
        assertEquals(msg + ": QueryClassBags do not refer to the same class", qcb1.getType(), qcb2.getType());
        assertEquals(msg + ": QueryClassBags do not have the same alias", q1.getAliases().get(qcb1), q2.getAliases().get(qcb2));
        assertEquals(msg + ": QueryClassBags do not have the same bags", qcb1.getIds(), qcb2.getIds());
        assertEquals(msg + ": QueryClassBags do not have the same osbs", qcb1.getOsb(), qcb2.getOsb());
    }
}
