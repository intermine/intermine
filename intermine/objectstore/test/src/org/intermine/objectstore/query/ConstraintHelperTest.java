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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.query.iql.IqlQueryParser;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.CEO;


public class ConstraintHelperTest extends TestCase
{
    private Query q, subquery1, subquery2;
    private QueryClass qc1, qc2, qc3, subQc1;
    private SimpleConstraint simpleConstraint1, simpleConstraint2, simpleConstraint3, simpleConstraint4, simpleConstraint5, simpleConstraint6, simpleConstraint7;
    private ClassConstraint classConstraint1, classConstraint2;
    private ContainsConstraint containsConstraint1;
    private SubqueryConstraint subqueryConstraint1, subqueryConstraint2;
    private BagConstraint bagConstraint1;
    private ConstraintSet cs1;

    public ConstraintHelperTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        // Set up a query with every type of constraint in it
        q = new Query();
        qc1 = new QueryClass(Company.class);
        qc2 = new QueryClass(Department.class);
        qc3 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryField qf2 = new QueryField(qc2, "name");
        QueryField qf3 = new QueryField(qc3, "name");
        QueryField qf4 = new QueryField(qc1, "vatNumber");

        QueryCollectionReference qcr1 = new QueryCollectionReference(qc1, "departments");

        QueryValue value1 = new QueryValue("Company1");
        QueryValue value2 = new QueryValue(new Integer(1001));

        QueryExpression expr1 = new QueryExpression(qf1, new QueryValue(new Integer(1)), new QueryValue(new Integer(1)));
        QueryExpression expr2 = new QueryExpression(qf2, new QueryValue(new Integer(1)), new QueryValue(new Integer(1)));
        QueryFunction func1 = new QueryFunction(qf4, QueryFunction.SUM);

        subquery1 = new Query();
        subQc1 = new QueryClass(Department.class);
        subquery1.addToSelect(subQc1);
        subquery1.addFrom(subQc1);

        subquery2 = new Query();
        QueryClass subQc2 = new QueryClass(Department.class);
        QueryField subQf1 = new QueryField(subQc2, "name");
        subquery2.addToSelect(subQf1);
        subquery2.addFrom(subQc2);

        q.addFrom(qc1, "company1");
        q.addFrom(qc2, "department1");
        q.addFrom(qc3, "department2");
        cs1 = new ConstraintSet(ConstraintOp.AND);
        simpleConstraint1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, value1);
        cs1.addConstraint(simpleConstraint1);
        simpleConstraint2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, qf3);
        cs1.addConstraint(simpleConstraint2);
        simpleConstraint3 = new SimpleConstraint(expr1, ConstraintOp.EQUALS, expr2);
        cs1.addConstraint(simpleConstraint1);
        simpleConstraint4 = new SimpleConstraint(value1, ConstraintOp.EQUALS, qf1);
        cs1. addConstraint(simpleConstraint4);
        simpleConstraint5 = new SimpleConstraint(value1, ConstraintOp.EQUALS, expr2);
        cs1. addConstraint(simpleConstraint5);
        simpleConstraint6 = new SimpleConstraint(value2, ConstraintOp.EQUALS, func1);
        cs1. addConstraint(simpleConstraint6);
        simpleConstraint7 = new SimpleConstraint(qf1, ConstraintOp.IS_NULL);
        cs1. addConstraint(simpleConstraint7);
        classConstraint1 = new ClassConstraint(qc2, ConstraintOp.NOT_EQUALS, qc3);
        cs1.addConstraint(classConstraint1);
        classConstraint2 = new ClassConstraint(qc2, ConstraintOp.NOT_EQUALS, new Department());
        cs1.addConstraint(classConstraint2);
        containsConstraint1 = new ContainsConstraint(qcr1, ConstraintOp.CONTAINS, qc2);
        cs1.addConstraint(containsConstraint1);
        subqueryConstraint1 = new SubqueryConstraint(qc2, ConstraintOp.IN, subquery1);
        cs1.addConstraint(subqueryConstraint1);
        subqueryConstraint2 = new SubqueryConstraint(qf1, ConstraintOp.IN, subquery2);
        cs1.addConstraint(subqueryConstraint2);

        List bagOfNames = new ArrayList();
        bagOfNames.add("Fred");
        bagOfNames.add("Maureen");
        bagOfNames.add("Eric");
        bagConstraint1 = new BagConstraint(qf1, ConstraintOp.IN, bagOfNames);
        cs1.addConstraint(bagConstraint1);

        cs1.addConstraint(subqueryConstraint2);

        q.setConstraint(cs1);
    }


    public void testCreateListNoConstraints() throws Exception {
        IqlQuery fq = new IqlQuery("select a from Company as a", "org.intermine.model.testmodel");
        q = IqlQueryParser.parse(fq);
        List expected = new ArrayList();
        List got = ConstraintHelper.createList(q);

        assertEquals(expected, got);
    }

    public void testCreateListSingleConstraint() throws Exception {
        IqlQuery fq = new IqlQuery("select a from Company as a where a.vatNumber = 5", "org.intermine.model.testmodel");
        q = IqlQueryParser.parse(fq);
        List expected = new ArrayList();
        expected.add(q.getConstraint());
        List got = ConstraintHelper.createList(q);

        assertEquals(expected, got);
    }

    public void testCreateListAnd() throws Exception {
        IqlQuery fq = new IqlQuery("select a from Company as a where a.vatNumber = 5 and a.name = 'hello'", "org.intermine.model.testmodel");
        q = IqlQueryParser.parse(fq);
        List expected = new ArrayList();
        Iterator conIter = ((ConstraintSet) q.getConstraint()).getConstraints().iterator();
        while (conIter.hasNext()) {
            expected.add((Constraint) conIter.next());
        }
        List got = ConstraintHelper.createList(q);

        assertEquals(expected, got);
    }

    public void testCreateListOr() throws Exception {
        IqlQuery fq = new IqlQuery("select a from Company as a where a.vatNumber = 5 or a.name = 'hello'", "org.intermine.model.testmodel");
        q = IqlQueryParser.parse(fq);
        List expected = new ArrayList();
        expected.add(q.getConstraint());
        List got = ConstraintHelper.createList(q);

        assertEquals(expected, got);
    }

    public void testTraverseConstraint() {
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.OR);

        cs2.addConstraint(cs1);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryValue value1 = new QueryValue("Company1");
        simpleConstraint1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, value1);
        cs2.addConstraint(simpleConstraint1);

        final Map foundConstraints = new HashMap();

        foundConstraints.put(ConstraintSet.class, new ArrayList());
        foundConstraints.put(BagConstraint.class, new ArrayList());
        foundConstraints.put(SimpleConstraint.class, new ArrayList());
        foundConstraints.put(SubqueryConstraint.class, new ArrayList());
        foundConstraints.put(ClassConstraint.class, new ArrayList());
        foundConstraints.put(ContainsConstraint.class, new ArrayList());

        ConstraintHelper.traverseConstraints(cs2, new ConstraintTraverseAction() {
            public void apply(Constraint c) {
                ((List) foundConstraints.get(c.getClass())).add(c);
            }
        });

        assertEquals(2, ((List) foundConstraints.get(ConstraintSet.class)).size());
        assertEquals(1, ((List) foundConstraints.get(BagConstraint.class)).size());
        assertEquals(7, ((List) foundConstraints.get(SimpleConstraint.class)).size());
        assertEquals(2, ((List) foundConstraints.get(SubqueryConstraint.class)).size());
        assertEquals(2, ((List) foundConstraints.get(ClassConstraint.class)).size());
        assertEquals(1, ((List) foundConstraints.get(ContainsConstraint.class)).size());
    }

    public void testFilterQueryClass() throws Exception {
        q = new Query();
        qc1 = new QueryClass(Company.class);
        qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                        ConstraintOp.CONTAINS,
                                                        qc2);
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        c.addConstraint(cc1);
        q.setConstraint(c);

        List expectedAll = new ArrayList(Arrays.asList(new Object[] {sc1, sc2, cc1}));
        List expected1 = new ArrayList(Arrays.asList(new Object[] {sc1, cc1}));
        List expected2 = new ArrayList(Arrays.asList(new Object[] {sc2}));
        List got = ConstraintHelper.createList(q);

        assertEquals(expectedAll, ConstraintHelper.createList(q));
        assertEquals(expected1, ConstraintHelper.filter(got, qc1, false));
        assertEquals(expected2, ConstraintHelper.filter(got, qc2, false));
    }


    public void testCreateListQueryClass() throws Exception {
        q = new Query();
        qc1 = new QueryClass(Company.class);
        qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                        ConstraintOp.CONTAINS,
                                                        qc2);
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        c.addConstraint(cc1);
        q.setConstraint(c);

        List expected1 = new ArrayList(Arrays.asList(new Object[] {sc1, cc1}));
        List expected2 = new ArrayList(Arrays.asList(new Object[] {sc2}));

        assertEquals(expected1, ConstraintHelper.createList(q, qc1));
        assertEquals(expected2, ConstraintHelper.createList(q, qc2));
    }


    public void testIsAssociatedWith() throws Exception {
        assertTrue(ConstraintHelper.isAssociatedWith(classConstraint1, qc2));
        assertTrue(ConstraintHelper.isAssociatedWith(classConstraint1, qc3));
        assertTrue(ConstraintHelper.isAssociatedWith(classConstraint2, qc2));

        assertTrue(ConstraintHelper.isAssociatedWith(containsConstraint1, qc1));
        assertFalse(ConstraintHelper.isAssociatedWith(containsConstraint1, qc2));

        assertTrue(ConstraintHelper.isAssociatedWith(subqueryConstraint1, qc2));
        assertTrue(ConstraintHelper.isAssociatedWith(subqueryConstraint2, qc1));
        assertFalse(ConstraintHelper.isAssociatedWith(subqueryConstraint1, subquery1));

        assertTrue(ConstraintHelper.isAssociatedWith(simpleConstraint1, qc1));
        assertTrue(ConstraintHelper.isAssociatedWith(simpleConstraint4, qc1));
        assertFalse(ConstraintHelper.isAssociatedWith(simpleConstraint2, qc2));
        assertFalse(ConstraintHelper.isAssociatedWith(simpleConstraint2, qc3));

        // QueryExpression & QueryFunction
        assertTrue(ConstraintHelper.isAssociatedWith(simpleConstraint5, qc2));
        assertTrue(ConstraintHelper.isAssociatedWith(simpleConstraint6, qc1));

        // single argument constraint
        assertTrue(ConstraintHelper.isAssociatedWith(simpleConstraint7, qc1));
    }

    public void testIsRelatedTo() throws Exception {
        // both sides of ClassConstraint
        assertTrue(ConstraintHelper.isRelatedTo(classConstraint1, qc2));
        assertTrue(ConstraintHelper.isRelatedTo(classConstraint1, qc3));
        assertTrue(ConstraintHelper.isRelatedTo(classConstraint2, qc2));

        // both sides of ContainsConstraint
        assertTrue(ConstraintHelper.isRelatedTo(containsConstraint1, qc1));
        assertTrue(ConstraintHelper.isRelatedTo(containsConstraint1, qc2));

        // both sides oc SubqueryConstraint
        assertTrue(ConstraintHelper.isRelatedTo(subqueryConstraint1, qc2));
        assertTrue(ConstraintHelper.isRelatedTo(subqueryConstraint2, qc1));
        assertTrue(ConstraintHelper.isRelatedTo(subqueryConstraint1, subquery1));

        // SimpleConstraints
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint1, qc1));
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint4, qc1));
        // cross-reference constraint
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint2, qc2));
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint2, qc3));

        // QueryExpression & QueryFunction
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint5, qc2));
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint6, qc1));

        // single argument constraint
        assertTrue(ConstraintHelper.isRelatedTo(simpleConstraint7, qc1));
    }

    public void testIsRelatedToNothing() throws Exception {
        QueryValue qv1 = new QueryValue(new Integer(101));
        QueryValue qv2 = new QueryValue(new Integer(202));
        QueryExpression qe1 = new QueryExpression (qv1, QueryExpression.ADD, qv2);

        SimpleConstraint sc1 = new SimpleConstraint(qv1, ConstraintOp.EQUALS, qv2);
        SimpleConstraint sc2 = new SimpleConstraint(qe1, ConstraintOp.EQUALS, qv2);
        assertTrue(ConstraintHelper.isRelatedToNothing(sc1));
        assertTrue(ConstraintHelper.isRelatedToNothing(sc2));

        // everything from testIsRelatedTo should be false

        assertFalse(ConstraintHelper.isRelatedToNothing(classConstraint1));
        assertFalse(ConstraintHelper.isRelatedToNothing(classConstraint1));
        assertFalse(ConstraintHelper.isRelatedToNothing(classConstraint2));

        // both sides of ContainsConstraint
        assertFalse(ConstraintHelper.isRelatedToNothing(containsConstraint1));
        assertFalse(ConstraintHelper.isRelatedToNothing(containsConstraint1));

        // both sides oc SubqueryConstraint
        assertFalse(ConstraintHelper.isRelatedToNothing(subqueryConstraint1));
        assertFalse(ConstraintHelper.isRelatedToNothing(subqueryConstraint2));
        assertFalse(ConstraintHelper.isRelatedToNothing(subqueryConstraint1));

        // SimpleConstraints
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint1));
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint4));
        // cross-reference constraint
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint2));
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint2));

        // QueryExpression & QueryFunction
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint5));
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint6));

        // single argument constraint
        assertFalse(ConstraintHelper.isRelatedToNothing(simpleConstraint7));
    }

    public void testIsAssociatedWith2() throws Exception {
        Query subQ = new Query();
        QueryClass subQC = new QueryClass(Company.class);
        subQ.alias(subQC, "Company");
        subQ.addFrom(subQC);
        subQ.addToSelect(subQC);

        q = new Query();
        QueryClass qcA = new QueryClass(Company.class);
        QueryClass qcB = new QueryClass(Department.class);
        q.alias(qcA, "a");
        q.addFrom(qcA);
        q.addToSelect(qcA);
        q.alias(qcB, "b");
        q.addFrom(qcB);
        q.alias(subQ, "c");
        q.addFrom(subQ);

        Constraint c1 = new SimpleConstraint(new QueryField(qcA, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(5)));
        Constraint c2 = new ContainsConstraint(new QueryCollectionReference(qcA, "departments"), ConstraintOp.CONTAINS, qcB);
        Constraint c3 = new SimpleConstraint(new QueryField(subQ, subQC, "name"), ConstraintOp.EQUALS, new QueryField(qcA, "name"));
        ConstraintSet c4 = new ConstraintSet(ConstraintOp.OR);
        c4.addConstraint(new SimpleConstraint(new QueryField(qcA, "vatNumber"), ConstraintOp.EQUALS, new QueryField(subQ, subQC, "vatNumber")));
        c4.addConstraint(new SimpleConstraint(new QueryField(subQ, subQC, "vatNumber"), ConstraintOp.EQUALS, new QueryValue(new Integer(3))));
        Constraint c5 = new SimpleConstraint(new QueryField(qcB, "name"), ConstraintOp.EQUALS, new QueryValue("hello"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(c1);
        c.addConstraint(c2);
        c.addConstraint(c3);
        c.addConstraint(c4);
        c.addConstraint(c5);
        q.setConstraint(c);

        assertTrue(ConstraintHelper.isAssociatedWith(c1, qcA));
        assertTrue(ConstraintHelper.isAssociatedWith(c2, qcA));
        assertFalse(ConstraintHelper.isAssociatedWith(c2, qcB));

        assertFalse(ConstraintHelper.isAssociatedWith(c3, qcA));
        assertFalse(ConstraintHelper.isAssociatedWith(c3, subQC));


    }


    public void testIsAssociatedWithNothing() throws Exception {
        QueryValue qv1 = new QueryValue("test");
        SimpleConstraint sc1 = new SimpleConstraint(qv1, ConstraintOp.IS_NULL);
        assertTrue(ConstraintHelper.isAssociatedWithNothing(sc1));

        // cross-references not associated with anything
        assertTrue(ConstraintHelper.isAssociatedWithNothing(simpleConstraint2));
        assertTrue(ConstraintHelper.isAssociatedWithNothing(simpleConstraint3));

        // everthing else is associated with something
        assertFalse(ConstraintHelper.isAssociatedWithNothing(classConstraint1));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(classConstraint1));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(classConstraint2));

        assertFalse(ConstraintHelper.isAssociatedWithNothing(containsConstraint1));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(containsConstraint1));

        assertFalse(ConstraintHelper.isAssociatedWithNothing(subqueryConstraint1));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(subqueryConstraint2));

        assertFalse(ConstraintHelper.isAssociatedWithNothing(simpleConstraint1));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(simpleConstraint4));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(simpleConstraint5));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(simpleConstraint6));
        assertFalse(ConstraintHelper.isAssociatedWithNothing(simpleConstraint7));
    }



    public void testIsCrossReference() throws Exception {
        qc1 = new QueryClass(Company.class);
        qc2 = new QueryClass(CEO.class);
        QueryField qf1 = new QueryField(qc1, "vatNumber");
        QueryField qf2 = new QueryField(qc2, "salary");
        QueryExpression expr1 = new QueryExpression(qf1, QueryExpression.ADD, qf2);
        QueryFunction func1 = new QueryFunction(qf1, QueryFunction.SUM);
        QueryValue qv1 = new QueryValue(new Integer(100));

        assertFalse(ConstraintHelper.isCrossReference(simpleConstraint1));
        assertTrue(ConstraintHelper.isCrossReference(simpleConstraint2));
        assertFalse(ConstraintHelper.isCrossReference(simpleConstraint7));

        SimpleConstraint sc1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, expr1);
        assertTrue(ConstraintHelper.isCrossReference(sc1));
        SimpleConstraint sc2 = new SimpleConstraint(qv1, ConstraintOp.EQUALS, expr1);
        assertTrue(ConstraintHelper.isCrossReference(sc2));
        SimpleConstraint sc3 = new SimpleConstraint(qv1, ConstraintOp.EQUALS, func1);
        assertFalse(ConstraintHelper.isCrossReference(sc3));
        SimpleConstraint sc4 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, func1);
        assertFalse(ConstraintHelper.isCrossReference(sc4));
        SimpleConstraint sc5 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, func1);
        assertTrue(ConstraintHelper.isCrossReference(sc5));
        SimpleConstraint sc6 = new SimpleConstraint(new QueryField(subquery1, subQc1, "name"), ConstraintOp.EQUALS,
                                                    new QueryField(qc1, "name"));
        assertTrue(ConstraintHelper.isCrossReference(sc6));

    }


    public void testGetQueryFields() throws Exception {
        qc1 = new QueryClass(Company.class);
        QueryField qf1 = new QueryField(qc1, "vatNumber");
        QueryField qf2 = new QueryField(qc1, "vatNumber");
        QueryField qf3 = new QueryField(qc1, "vatNumber");
        QueryValue qv1 = new QueryValue(new Integer(100));

        QueryExpression expr1 = new QueryExpression(qf1, QueryExpression.ADD, qf2);
        QueryExpression expr2 = new QueryExpression(expr1, QueryExpression.ADD, qf3);
        QueryFunction func1 = new QueryFunction(qf1, QueryFunction.SUM);
        QueryFunction func2 = new QueryFunction(expr1, QueryFunction.SUM);
        QueryExpression expr3 = new QueryExpression(func2, QueryExpression.ADD, qv1);

        Set expected = new HashSet();
        assertEquals(expected, ConstraintHelper.getQueryFields(qv1));
        expected = new HashSet(Arrays.asList(new Object[] {qf1}));
        assertEquals(expected, ConstraintHelper.getQueryFields(qf1));
        expected = new HashSet(Arrays.asList(new Object[] {qf1, qf2}));
        assertEquals(expected, ConstraintHelper.getQueryFields(expr1));
        expected = new HashSet(Arrays.asList(new Object[] {qf1, qf2, qf3}));
        assertEquals(expected, ConstraintHelper.getQueryFields(expr2));
        expected = new HashSet(Arrays.asList(new Object[] {qf1}));
        assertEquals(expected, ConstraintHelper.getQueryFields(func1));
        expected = new HashSet(Arrays.asList(new Object[] {qf1, qf2}));
        assertEquals(expected, ConstraintHelper.getQueryFields(func2));
        expected = new HashSet(Arrays.asList(new Object[] {qf1, qf2}));
        assertEquals(expected, ConstraintHelper.getQueryFields(expr3));

        expected = new HashSet();
        assertEquals(expected, ConstraintHelper.getQueryFields(null));
    }


    public void testGetLeftArgument() {
        assertTrue(ConstraintHelper.getLeftArgument(simpleConstraint1) instanceof QueryField);
        assertEquals("name", ((QueryField) ConstraintHelper.getLeftArgument(simpleConstraint1)).getFieldName());
        assertTrue(ConstraintHelper.getLeftArgument(simpleConstraint4) instanceof QueryValue);
        assertEquals("Company1", ((QueryValue) ConstraintHelper.getLeftArgument(simpleConstraint4)).getValue());
        assertTrue(ConstraintHelper.getLeftArgument(simpleConstraint3) instanceof QueryExpression);

        assertTrue(ConstraintHelper.getLeftArgument(classConstraint1) instanceof QueryClass);
        assertEquals(Department.class, ((QueryClass) ConstraintHelper.getLeftArgument(classConstraint1)).getType());

        assertTrue(ConstraintHelper.getLeftArgument(containsConstraint1) instanceof QueryReference);
        assertEquals(qc1, ((QueryReference) ConstraintHelper.getLeftArgument(containsConstraint1)).getQueryClass());
        assertEquals("departments", ((QueryReference) ConstraintHelper.getLeftArgument(containsConstraint1)).getFieldName());

        assertTrue(ConstraintHelper.getLeftArgument(bagConstraint1) instanceof QueryNode);
        assertEquals(qc1, ((QueryField) ConstraintHelper.getLeftArgument(bagConstraint1)).getFromElement());
        assertEquals("name", ((QueryField) ConstraintHelper.getLeftArgument(bagConstraint1)).getFieldName());

        assertTrue(ConstraintHelper.getLeftArgument(subqueryConstraint1) instanceof QueryClass);
        assertEquals(Department.class, ((QueryClass) ConstraintHelper.getLeftArgument(subqueryConstraint1)).getType());
        assertTrue(ConstraintHelper.getLeftArgument(subqueryConstraint2) instanceof QueryField);
        assertEquals("name", ((QueryField) ConstraintHelper.getLeftArgument(subqueryConstraint2)).getFieldName());

        assertNull(ConstraintHelper.getLeftArgument(cs1));
    }

    public void testGetRightArgument() {
        assertTrue(ConstraintHelper.getRightArgument(simpleConstraint1) instanceof QueryValue);
        assertEquals("Company1", ((QueryValue) ConstraintHelper.getRightArgument(simpleConstraint1)).getValue());
        assertTrue(ConstraintHelper.getRightArgument(simpleConstraint2) instanceof QueryField);
        assertEquals("name", ((QueryField) ConstraintHelper.getRightArgument(simpleConstraint2)).getFieldName());
        assertTrue(ConstraintHelper.getRightArgument(simpleConstraint3) instanceof QueryExpression);

        assertTrue(ConstraintHelper.getRightArgument(classConstraint1) instanceof QueryClass);
        assertEquals(Department.class, ((QueryClass) ConstraintHelper.getRightArgument(classConstraint1)).getType());
        assertTrue(ConstraintHelper.getRightArgument(classConstraint2) instanceof Department);

        assertTrue(ConstraintHelper.getRightArgument(containsConstraint1) instanceof QueryClass);
        assertEquals(Department.class, ((QueryClass) ConstraintHelper.getRightArgument(containsConstraint1)).getType());

        assertTrue(ConstraintHelper.getRightArgument(subqueryConstraint1) instanceof Query);
        assertTrue(ConstraintHelper.getRightArgument(bagConstraint1) instanceof Collection);

        assertNull(ConstraintHelper.getRightArgument(cs1));
    }

}

