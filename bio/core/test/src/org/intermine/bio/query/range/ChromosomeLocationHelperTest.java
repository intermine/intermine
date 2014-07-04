package org.intermine.bio.query.range;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.metadata.ConstraintOp.AND;
import static org.intermine.metadata.ConstraintOp.GREATER_THAN_EQUALS;
import static org.intermine.metadata.ConstraintOp.LESS_THAN;
import static org.intermine.metadata.ConstraintOp.OR;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.Location;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;

/**
 * Tests for the ChromosomeLocationHelper class.
 *
 * @author Julie
 */

public class ChromosomeLocationHelperTest extends TestCase
{

    private final QueryValue startOfRange = new QueryValue(1);
    private final QueryValue endOfRange = new QueryValue(99);
    private final QueryValue bigStartOfRange = new QueryValue(10000000);
    private final QueryValue bigEndOfRange = new QueryValue(990000000);


    public ChromosomeLocationHelperTest (String arg) {
        super(arg);
    }

    public void testCreateConstraint() {

        QueryClass location = new QueryClass(Location.class);
        QueryField start = new QueryField(location, "start");
        QueryField end = new QueryField(location, "end");

        ConstraintSet cs = new ConstraintSet(AND);

        ConstraintSet cs1 = new ConstraintSet(OR);
        cs1.addConstraint(new SimpleConstraint(start, ConstraintOp.LESS_THAN, startOfRange));
        cs1.addConstraint(new SimpleConstraint(end, ConstraintOp.GREATER_THAN_EQUALS, endOfRange));
        cs.addConstraint(cs1);

        ConstraintSet cs2 = new ConstraintSet(OR);
        cs2.addConstraint(new SimpleConstraint(start, LESS_THAN, bigStartOfRange));
        cs2.addConstraint(new SimpleConstraint(end, GREATER_THAN_EQUALS, bigEndOfRange));
        cs.addConstraint(cs2);


        QueryClass qc = new QueryClass(Location.class);
        List<String> ranges = Arrays.asList("50", "100");
        PathConstraintRange constraint = new PathConstraintRange("Location", ConstraintOp.WITHIN, ranges);


        ChromosomeLocationHelper clh = new ChromosomeLocationHelper();
        Constraint rangeConstraint = clh.createConstraint(null, qc, constraint);
        assertEquals("constraints should be the same", cs, rangeConstraint);
    }

    public void testGenomicInterval() {


    }

}
