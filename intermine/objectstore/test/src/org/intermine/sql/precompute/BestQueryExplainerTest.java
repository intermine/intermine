package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;
import org.intermine.sql.query.*;

public class BestQueryExplainerTest extends TestCase
{
    private Query q1, q2;

    public BestQueryExplainerTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        q1 = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q1.addFrom(t);
        q1.addSelect(sv);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));

        q2 = new Query();
        Table t1 = new Table("mytable");
        Table t2 = new Table("mytable");
        Field f1 = new Field("a", t1);
        Field f2 = new Field("b", t2);
        sv = new SelectValue(f1, null);
        q2.addFrom(t1);
        q2.addFrom(t2);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c));
        q2.addWhere(new Constraint(f2, Constraint.EQ, c));
    }

    public void testNullConstructor() throws Exception {
        BestQueryExplainer bq;
        try {
            bq = new BestQueryExplainer(null, -1);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullBestQueryForNoneAdded() throws Exception {
        BestQueryExplainer bq = new BestQueryExplainer();
        assertNull(bq.getBestQuery());
        assertNull(bq.getBestQueryString());
        assertNull(bq.getBestExplainResult());
    }

    /*
    public void testAddNull() throws Exception {
        BestQueryExplainer bq = new BestQueryExplainer();
        try {
            bq.add(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }*/

    public void testReturnFirst() throws Exception {
        System.gc();
        BestQueryExplainer bq = new BestQueryExplainer();
        bq.add(q1);
        assertEquals(q1, bq.getBestQuery());
    }

    public void testReturnBest() throws Exception {
        System.gc();
        BestQueryExplainer bq = new BestQueryExplainer();
        bq.add(q1);
        bq.add(q2);
        assertEquals(q1, bq.getBestQuery());

        bq = new BestQueryExplainer();
        bq.add(q2);
        bq.add(q1);
        assertEquals(q1, bq.getBestQuery());
    }

    public void testStopsWhenQueryBetterThanElapsed() throws Exception {
        System.gc();
        BestQueryExplainer bq = new BestQueryExplainer();
        bq.add(q1); // Takes 600 milliseconds
        Thread.sleep(700);
        try {
            bq.add(q2);
            fail("Expected: BestQueryException");
        }
        catch (BestQueryException e) {
        }
    }

}
