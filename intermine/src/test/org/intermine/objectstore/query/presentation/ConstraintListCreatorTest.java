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

import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.fql.FqlQueryParser;

public class ConstraintListCreatorTest extends TestCase
{
    public ConstraintListCreatorTest(String arg) {
        super(arg);
    }

    public void test1() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }

    public void test2() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a where a.vatNumber = 5", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        expected.add(new AssociatedConstraint(q, q.getConstraint()));
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }

    public void test3() throws Exception {
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

    public void test4() throws Exception {
        FqlQuery fq = new FqlQuery("select a from Company as a where a.vatNumber = 5 or a.name = 'hello'", "org.flymine.model.testmodel");
        Query q = FqlQueryParser.parse(fq);
        List expected = new ArrayList();
        expected.add(new PrintableConstraint(q, (ConstraintSet) q.getConstraint()));
        List got = ConstraintListCreator.createList(q);

        assertEquals(expected, got);
    }
}


