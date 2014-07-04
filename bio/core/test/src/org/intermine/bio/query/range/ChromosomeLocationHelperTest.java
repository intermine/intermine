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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.bio.query.range.ChromosomeLocationHelper.GenomicInterval;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
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
    int startCoordinate = 31222839;
    int endCoordinate = 31224287;
    List<String> ranges = Arrays.asList("1:31222839..31224287");

    public void testCreateConstraint() {
        QueryClass qc = new QueryClass(Location.class);
        PathConstraintRange pcr = new PathConstraintRange("Location", ConstraintOp.OVERLAPS, ranges);
        ChromosomeLocationHelper clh = new ChromosomeLocationHelper();
        Constraint rangeConstraint = clh.createConstraint(null, qc, pcr);

        // constraint created
        Set<Constraint> constraints = ((ConstraintSet) rangeConstraint).getConstraints();
        assertEquals(1, constraints.size());

        // break into three bits
        Set<Constraint>  mainConstraints = ((ConstraintSet) constraints.iterator().next()).getConstraints();
        assertEquals(3, mainConstraints.size());

        // Chromosome.primaryidentifier = 1
        String simpleConstraint = "SimpleConstraint(QueryField(org.intermine.model.bio.Chromosome, primaryIdentifier) = java.lang.String: \"1\")";
        String containsConstraint = "org.intermine.model.bio.Location.locatedOn CONTAINS org.intermine.model.bio.Chromosome";
        // Location.start OVERLAPS Location.end
        String overlapConstraint = "start=QueryField(org.intermine.model.bio.Location, start), end=QueryField(org.intermine.model.bio.Location, end) OVERLAPS start=java.lang.Integer: \"31222839\", end=java.lang.Integer: \"31224287\"";


        for (Constraint c : mainConstraints) {
            if (c.toString().contains("OVERLAPS")) {
                assertEquals(overlapConstraint, c.toString());
            } else if (c.toString().contains("CONTAINS")) {
                assertEquals(containsConstraint, c.toString());
            } else {
                assertEquals(simpleConstraint, c.toString());
            }
        }
    }

    public void testGenomicInterval() {
        GenomicInterval g = new GenomicInterval("1:31222839..31224287");
        assertTrue(g.getStart() == 31222839);
        assertTrue(g.getEnd() == 31224287);
        assertEquals(g.getChr(), "1");
    }
}

