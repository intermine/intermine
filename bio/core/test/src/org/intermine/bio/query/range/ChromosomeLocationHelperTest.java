package org.intermine.bio.query.range;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.intermine.bio.query.range.ChromosomeLocationHelper.GenomicInterval;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.Location;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.pathquery.PathConstraintRange;

import junit.framework.TestCase;

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

    public void testCreateConstraintWithTaxon() {
        QueryClass qc = new QueryClass(Location.class);
        PathConstraintRange pcr = new PathConstraintRange("Location",
                ConstraintOp.OVERLAPS,
                Arrays.asList("7227:X:123..456", "9606:X:123..456"));
        ChromosomeLocationHelper clh = new ChromosomeLocationHelper();
        Constraint rangeConstraint = clh.createConstraint(null, qc, pcr);
        Set<Constraint> constraints = ((ConstraintSet) rangeConstraint).getConstraints();
        assertEquals(2, constraints.size());
        assertEquals(ConstraintOp.OR, ((ConstraintSet) rangeConstraint).getOp());
        ConstraintSet firstRangeConstraint = (ConstraintSet) constraints.iterator().next();
        assertEquals(ConstraintOp.AND, firstRangeConstraint.getOp());
        Set<Constraint>  mainConstraints = firstRangeConstraint.getConstraints();
        assertEquals(5, mainConstraints.size());


        List<String> constraintStrings = new ArrayList<String>();
        for (Constraint c : mainConstraints) {
            constraintStrings.add(c.toString());
        }
        Collections.sort(constraintStrings);

        List<String> expected = Arrays.asList(
            "SimpleConstraint(QueryField(org.intermine.model.bio.Chromosome, primaryIdentifier) = java.lang.String: \"X\")",
            "SimpleConstraint(QueryField(org.intermine.model.bio.Organism, taxonId) = java.lang.Integer: \"7227\")",
            "org.intermine.model.bio.Chromosome.organism CONTAINS org.intermine.model.bio.Organism",
            "org.intermine.model.bio.Location.locatedOn CONTAINS org.intermine.model.bio.Chromosome",
            "start=QueryField(org.intermine.model.bio.Location, start), end=QueryField(org.intermine.model.bio.Location, end) OVERLAPS start=java.lang.Integer: \"123\", end=java.lang.Integer: \"456\""
        );

        for (int i = 0; i < 5; i++) {
            assertEquals("Constraint " + i + " does not match", expected.get(i), constraintStrings.get(i));
        }
    }

    public void testGenomicInterval() {
        GenomicInterval g = new GenomicInterval("1:31222839..31224287");
        assertTrue(g.getStart() == 31222839);
        assertTrue(g.getEnd() == 31224287);
        assertEquals(g.getChr(), "1");
    }

    public void testGenomicIntervalThreeDots() {
        GenomicInterval g = new GenomicInterval("1:31222839...31224287");
        assertTrue(g.getStart() == 31222839);
        assertTrue(g.getEnd() == 31224287);
        assertEquals(g.getChr(), "1");
    }

    public void testGenomicIntervalDash() {
        GenomicInterval g = new GenomicInterval("1:31222839-31224287");
        assertTrue(g.getStart() == 31222839);
        assertTrue(g.getEnd() == 31224287);
        assertEquals(g.getChr(), "1");
    }

    public void testGenomicIntervalTabSeparated() {
        GenomicInterval g = new GenomicInterval("1\t31222839\t31224287");
        assertTrue(g.getStart() == 31222839);
        assertTrue(g.getEnd() == 31224287);
        assertEquals(g.getChr(), "1");
    }

    public void testGenomicIntervalTabSeparatedWithOtherFields() {
        GenomicInterval g = new GenomicInterval("1\t31222839\t31224287\tFOO\tBAR");
        assertTrue(g.getStart() == 31222839);
        assertTrue(g.getEnd() == 31224287);
        assertEquals(g.getChr(), "1");
    }

    public void testGff3Interval() {
        GenomicInterval g = new GenomicInterval("2L\tGenbank\tSO:00001\t123\t456\tx\ty\tz");
        assertEquals("GFF3", g.getParsedAs());
        assertTrue(g.getStart() == 123);
        assertTrue(g.getEnd() == 456);
        assertEquals(g.getChr(), "2L");
    }

    public void testGenomicIntervalPoint() {
        GenomicInterval g = new GenomicInterval("2L:123");
        assertTrue(g.getStart() == 123);
        assertEquals(g.getChr(), "2L");
        assertEquals(g.getEnd(), Integer.valueOf(123));
    }

    public void testGenomicIntervalWithTaxon() {
        GenomicInterval g = new GenomicInterval("7227:1:123..456");
        assertEquals(g.getStart(), Integer.valueOf(123));
        assertEquals(g.getEnd(), Integer.valueOf(456));
        assertEquals(g.getChr(), "1");
        assertEquals(g.getTaxonId(), "7227");
    }

    public void testGenomicIntervalWithTaxonDash() {
        GenomicInterval g = new GenomicInterval("7227:1:123-456");
        assertEquals(g.getStart(), Integer.valueOf(123));
        assertEquals(g.getEnd(), Integer.valueOf(456));
        assertEquals(g.getChr(), "1");
        assertEquals(g.getTaxonId(), "7227");
    }
}

