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

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;

public class PrintableConstraintTest extends TestCase
{
    private PrintableConstraint pc1, pc2, pc3, pc4;

    public PrintableConstraintTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Query q1 = new Query();
        Query q2 = new Query();
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.OR);

        pc1 = new PrintableConstraint(q1, cs1);
        pc2 = new PrintableConstraint(q1, cs1);
        pc3 = new PrintableConstraint(q2, cs1);
        pc4 = new PrintableConstraint(q2, cs2);
    }

    public void testEquals() {
        assertEquals(pc1, pc1);
        assertEquals(pc1, pc2);
        assertFalse(pc1.equals(pc3));
        assertFalse(pc1.equals(pc4));
        assertFalse(pc3.equals(pc4));
    }

    public void testHashCode() {
        assertEquals(pc1.hashCode(), pc1.hashCode());
        assertEquals(pc1.hashCode(), pc2.hashCode());
        // Not testing this
        //assertFalse(pc1.hashCode() == pc3.hashCode());
        assertFalse(pc1.hashCode() == pc4.hashCode());
        assertFalse(pc3.hashCode() == pc4.hashCode());
    }


}
