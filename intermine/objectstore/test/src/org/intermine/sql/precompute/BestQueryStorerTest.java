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

public class BestQueryStorerTest extends TestCase
{
    private Query q1, q2;

    public BestQueryStorerTest(String arg1) {
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

    public void testNoQueriesForNoneAdded() throws Exception {
        BestQueryStorer bq = new BestQueryStorer();
        assertEquals(0, bq.getQueries().size());
    }

    /*
    public void testAddNull() throws Exception {
        BestQueryStorer bq = new BestQueryStorer();
        try {
            bq.add(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }*/

    public void testReturnFirst() throws Exception {
        BestQueryStorer bq = new BestQueryStorer();
        bq.add(q1);
        assertTrue(bq.getQueries().contains(q1));
    }

    public void testReturnBest() throws Exception {
        BestQueryStorer bq = new BestQueryStorer();
        bq.add(q1);
        bq.add(q2);
        assertEquals(2, bq.getQueries().size());
        assertTrue(bq.getQueries().contains(q1));
        assertTrue(bq.getQueries().contains(q2));

        bq = new BestQueryStorer();
        bq.add(q2);
        bq.add(q1);
        assertEquals(2, bq.getQueries().size());
        assertTrue(bq.getQueries().contains(q1));
        assertTrue(bq.getQueries().contains(q2));
    }

}
