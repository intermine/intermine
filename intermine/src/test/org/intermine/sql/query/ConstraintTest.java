package org.flymine.sql.query;

import junit.framework.*;

public class ConstraintTest extends TestCase
{
    private AbstractValue v1, v2, v3, v4, a;
    private Constraint c1, c2, c3, c4, c5, c6, c7, c8;

    public ConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        v1 = new Constant("1");
        v2 = new Constant("2");
        v3 = new Constant("'Flibble'");
        v4 = new Constant("'Flobble'");
        a = new Constant("a");
        c1 = new Constraint(a, Constraint.EQ, v1);
        c2 = new Constraint(a, Constraint.EQ, v2);
        c3 = new Constraint(a, Constraint.LT, v1);
        c4 = new Constraint(a, Constraint.LT, v2);
        c5 = new Constraint(v1, Constraint.LT, a);
        c6 = new Constraint(v3, Constraint.EQ, v4);
	c7 = new Constraint(a, Constraint.LIKE, v3);
	c8 = new Constraint(a, Constraint.LIKE, v4);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("a = 1", c1.getSQLString());
        assertEquals("a = 2", c2.getSQLString());
        assertEquals("a < 1", c3.getSQLString());
        assertEquals("a < 2", c4.getSQLString());
        assertEquals("1 < a", c5.getSQLString());
        assertEquals("'Flibble' = 'Flobble'", c6.getSQLString());
	assertEquals("a LIKE 'Flibble'", c7.getSQLString());
    }

    public void testCompare() throws Exception {
        assertEquals(AbstractConstraint.EQUAL, c1.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(c3));
        assertEquals(AbstractConstraint.IMPLIES, c1.compare(c4));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c1.compare(c7));

        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(c1));
        assertEquals(AbstractConstraint.EQUAL, c2.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(c3));
        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(c4));
        assertEquals(AbstractConstraint.IMPLIES, c2.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c2.compare(c7));

        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(c2));
        assertEquals(AbstractConstraint.EQUAL, c3.compare(c3));
        assertEquals(AbstractConstraint.IMPLIES, c3.compare(c4));
        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c3.compare(c7));

        assertEquals(AbstractConstraint.IMPLIED_BY, c4.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, c4.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, c4.compare(c3));
        assertEquals(AbstractConstraint.EQUAL, c4.compare(c4));
        assertEquals(AbstractConstraint.OR, c4.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c4.compare(c7));

        assertEquals(AbstractConstraint.EXCLUDES, c5.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, c5.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, c5.compare(c3));
        assertEquals(AbstractConstraint.OR, c5.compare(c4));
        assertEquals(AbstractConstraint.EQUAL, c5.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c5.compare(c7));


        assertEquals(AbstractConstraint.INDEPENDENT, c6.compare(c1));

        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c1));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c2));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c3));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c4));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c5));
        assertEquals(AbstractConstraint.EQUAL, c7.compare(c7));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c8));


    }
}
