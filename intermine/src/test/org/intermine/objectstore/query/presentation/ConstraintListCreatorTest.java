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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

import org.flymine.objectstore.query.*;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.fql.FqlQueryParser;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Department;


public class ConstraintListCreatorTest extends TestCase
{
    public ConstraintListCreatorTest(String arg) {
        super(arg);
    }

    public void testCreateListNoConstraints() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }

    public void testCreateListSingleConstraint() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a where a.vatNumber = 5", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        expected.add(new AssociatedConstraint(q, q.getConstraint()));
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }

    public void testCreateListAnd() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a where a.vatNumber = 5 and a.name = 'hello'", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        Iterator conIter = ((ConstraintSet) q.getConstraint()).getConstraints().iterator();
        while (conIter.hasNext()) {
            expected.add(new AssociatedConstraint(q, (Constraint) conIter.next()));
        }
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }

    public void testCreateListOr() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a where a.vatNumber = 5 or a.name = 'hello'", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        expected.add(new PrintableConstraint(q, (ConstraintSet) q.getConstraint()));
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }

    public void testFilterQueryClass() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    SimpleConstraint.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    SimpleConstraint.EQUALS,
                                                    new QueryValue("department1"));
        ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                        ContainsConstraint.CONTAINS,
                                                        qc2);
        ConstraintSet c = new ConstraintSet(ConstraintSet.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        c.addConstraint(cc1);
        q.setConstraint(c);

        AssociatedConstraint ac1 = new AssociatedConstraint(q, sc1);
        AssociatedConstraint ac2 = new AssociatedConstraint(q, sc2);
        AssociatedConstraint ac3 = new AssociatedConstraint(q, cc1);
        List expectedAll = new ArrayList(Arrays.asList(new Object[] {ac1, ac2, ac3}));
        List expected1 = new ArrayList(Arrays.asList(new Object[] {ac1, ac3}));
        List expected2 = new ArrayList(Arrays.asList(new Object[] {ac2}));
        List got = ConstraintListCreator.createList(q);


        assertEquals(expectedAll, ConstraintListCreator.createList(q));
        assertEquals(expected1, ConstraintListCreator.filter(got, qc1));
        assertEquals(expected2, ConstraintListCreator.filter(got, qc2));
    }


    public void testCreateListQueryClass() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    SimpleConstraint.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    SimpleConstraint.EQUALS,
                                                    new QueryValue("department1"));
        ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
                                                        ContainsConstraint.CONTAINS,
                                                        qc2);
        ConstraintSet c = new ConstraintSet(ConstraintSet.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        c.addConstraint(cc1);
        q.setConstraint(c);

        AssociatedConstraint ac1 = new AssociatedConstraint(q, sc1);
        AssociatedConstraint ac2 = new AssociatedConstraint(q, sc2);
        AssociatedConstraint ac3 = new AssociatedConstraint(q, cc1);
        List expected1 = new ArrayList(Arrays.asList(new Object[] {ac1, ac3}));
        List expected2 = new ArrayList(Arrays.asList(new Object[] {ac2}));

        assertEquals(expected1, ConstraintListCreator.createList(q, qc1));
        assertEquals(expected2, ConstraintListCreator.createList(q, qc2));
    }


}


