package org.intermine.sql.query;

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

public class DummyExplainResultTest extends TestCase
{

    private Query q1, q2;

    public DummyExplainResultTest(String arg1) {
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
        Table t2 = new Table("mytable2");
        c = new Constant("1");
        Field f1 = new Field("a", t1);
        Field f2 = new Field("b", t2);
        sv = new SelectValue(f, null);
        q2.addFrom(t1);
        q2.addFrom(t2);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c));
        q2.addWhere(new Constraint(f2, Constraint.EQ, c));
    }

    public void testTime() throws Exception {
        ExplainResult d1 = ExplainResult.getInstance(q1, null);
        ExplainResult d2 = ExplainResult.getInstance(q2, null);
        assertEquals(600, d1.getTime());
        assertEquals(1200, d2.getTime());
    }

}
