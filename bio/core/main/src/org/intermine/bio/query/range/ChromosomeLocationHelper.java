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

import java.util.regex.Pattern;

import org.intermine.api.query.RangeHelper;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.HasFromList;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Queryable;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;

/**
 * Method to help querying for ranges
 *
 * @author Alex Kalderimis
 */
public class ChromosomeLocationHelper implements RangeHelper
{
    private final QueryClass chromosome, organism;
    private final QueryField chrIdField, taxonIdField;
    private final boolean taxonIdsAreStrings;

    /**
     * Method to set up chromosome fields for helping
     */
    public ChromosomeLocationHelper() {
        Model model = Model.getInstanceByName("genomic");
        if (model == null) {
            throw new RuntimeException("No genomic model is available");
        }
        ClassDescriptor chr = model.getClassDescriptorByName("Chromosome");
        if (chr == null) {
            throw new RuntimeException("This genomic model does not contain Chromosomes");
        }
        ClassDescriptor org = model.getClassDescriptorByName("Organism");
        if (org == null) {
            throw new RuntimeException("This genomic model does not contain Organisms");
        }
        AttributeDescriptor taxonDesc = org.getAttributeDescriptorByName("taxonId");
        taxonIdsAreStrings = "java.lang.String".equals(taxonDesc.getType());

        chromosome = new QueryClass(chr.getType());
        organism = new QueryClass(org.getType());
        chrIdField = new QueryField(chromosome, "primaryIdentifier");
        taxonIdField = new QueryField(organism, "taxonId");
    }

    private class ConstraintSetFactory
    {
        private final Queryable q;
        private final QueryObjectReference qor, chrOR;
        private final OverlapRange left;
        private final ConstraintOp rangeOp;

        ConstraintSetFactory(Queryable q, QueryNode n,
            QueryObjectReference qor, QueryObjectReference chrOR,
            OverlapRange left, ConstraintOp rangeOp) {
            this.q = q;
            this.qor = qor;
            this.chrOR = chrOR;
            this.left = left;
            this.rangeOp = rangeOp;
        }

        ConstraintSet process(final GenomicInterval interval) {
            ConstraintSet rangeSet = new ConstraintSet(ConstraintOp.AND);
            String chrId = interval.getChr();
            String taxonId = interval.getTaxonId();
            // Specify the organism if provided.
            if (taxonId != null) {
                specifyOrganism(rangeSet, taxonId);
            }

            rangeSet.addConstraint(
                    new ContainsConstraint(chrOR, ConstraintOp.CONTAINS, chromosome));
            rangeSet.addConstraint(
                    new SimpleConstraint(chrIdField, ConstraintOp.EQUALS, new QueryValue(chrId)));

            if (interval.getEnd() != null) {
                OverlapRange right = new OverlapRange(
                        new QueryValue(interval.getStart()),
                        new QueryValue(interval.getEnd()), chrOR);
                rangeSet.addConstraint(new OverlapConstraint(left, rangeOp, right));
            } else if (interval.getStart() != null) {
                OverlapRange right = new OverlapRange(new QueryValue(interval.getStart()),
                        new QueryValue(interval.getStart()), chrOR);
                rangeSet.addConstraint(new OverlapConstraint(left, rangeOp, right));
            } else {
                // Chromosome only - no action needed.
            }
            return rangeSet;
        }

        private void specifyOrganism(ConstraintSet cs, String taxonId) {
            QueryValue taxon;
            if (taxonIdsAreStrings) {
                taxon = new QueryValue(taxonId);
            } else {
                taxon = new QueryValue(Integer.valueOf(taxonId));
            }
            QueryObjectReference orgref = new QueryObjectReference(chromosome, "organism");
            addFrom(q, organism);
            cs.addConstraint(new ContainsConstraint(orgref, ConstraintOp.CONTAINS, organism));
            cs.addConstraint(new SimpleConstraint(taxonIdField, ConstraintOp.EQUALS, taxon));
        }
    }

    @Override
    public Constraint createConstraint(Queryable q, QueryNode n, PathConstraintRange pcr) {

        addFrom(q, chromosome);

        QueryField leftA = new QueryField((QueryClass) n, "start");
        QueryField leftB = new QueryField((QueryClass) n, "end");
        QueryObjectReference qor = new QueryObjectReference((QueryClass) n, "feature");
        QueryObjectReference chrOR = new QueryObjectReference((QueryClass) n, "locatedOn");
        OverlapRange left = new OverlapRange(leftA, leftB, chrOR);
        ConstraintOp op = pcr.getOp();
        ConstraintOp rangeOp = op;
        if (op == ConstraintOp.WITHIN) {
            rangeOp = ConstraintOp.IN;
        } else if (op == ConstraintOp.OUTSIDE) {
            rangeOp = ConstraintOp.NOT_IN;
        }

        ConstraintOp mainOp = (op == ConstraintOp.WITHIN
                || op == ConstraintOp.CONTAINS
                || op == ConstraintOp.OVERLAPS)
                ? ConstraintOp.OR : ConstraintOp.AND;
        ConstraintSet mainSet = new ConstraintSet(mainOp);

        ConstraintSetFactory factory = new ConstraintSetFactory(q, n, qor, chrOR, left, rangeOp);
        for (String range: pcr.getValues()) {
            GenomicInterval interval = new GenomicInterval(range);
            ConstraintSet rangeSet = factory.process(interval);
            mainSet.addConstraint(rangeSet);
        }
        return mainSet;
    }

    private void addFrom(Queryable q, QueryClass qc) {
        if (q instanceof HasFromList) {
            ((HasFromList) q).addFrom(qc);
        }
    }

    /**
     * Represents a genomic interval
     */
    static class GenomicInterval
    {

        private final Integer start, end;
        private final String chr, taxonId;
        private final String parsedAs;

        private static final Pattern GFF3 = Pattern.compile(
                "^[^\\t]+\\t[^\\t]+\\t[^\\t]+\\d+\\t\\d+");
        private static final Pattern BED = Pattern.compile("^[^\\t]+\\t\\d+\\t\\d+");
        private static final Pattern COLON_DASH = Pattern.compile("^[^:]+:\\d+-\\d+");
        private static final Pattern COLON_DOTS = Pattern.compile("^[^:]+:\\d+\\.\\.\\.?\\d+");
        private static final Pattern COLON_START = Pattern.compile("^[^:]+:\\d+$");
        private static final Pattern CHR_ONLY = Pattern.compile("^[^:]+$");
        private static final Pattern COLON_DASH_WITH_TAXON
            = Pattern.compile("^\\d+:[^:]+:\\d+-\\d+");
        private static final Pattern COLON_DOTS_WITH_TAXON
            = Pattern.compile("^\\d+:[^:]+:\\d+\\.\\.\\.?\\d+");

        /**
         * @param range string to be parsed for coordinates
         */
        GenomicInterval(String range) {
            if (range == null) {
                throw new NullPointerException("range may not be null");
            }
            if (GFF3.matcher(range).find()) {
                String[] parts = range.split("\\t");
                chr = parts[0].trim();
                start = Integer.valueOf(parts[3].trim());
                end = Integer.valueOf(parts[4].trim());
                taxonId = null;
                parsedAs = "GFF3";
            } else if (BED.matcher(range).find()) {
                String[] parts = range.split("\\t");
                chr = parts[0].trim();
                start = Integer.valueOf(parts[1].trim());
                end = Integer.valueOf(parts[2].trim());
                taxonId = null;
                parsedAs = "BED";
            } else if (COLON_DASH.matcher(range).matches()) {
                String[] partsA = range.split(":");
                chr = partsA[0];
                String[] partsB = partsA[1].split("-");
                start = Integer.valueOf(partsB[0].trim());
                end = Integer.valueOf(partsB[1].trim());
                taxonId = null;
                parsedAs = "COLON_DASH";
            } else if (COLON_DOTS.matcher(range).matches()) {
                String[] partsA = range.split(":");
                chr = partsA[0];
                String[] partsB = partsA[1].split("\\.\\.");
                start = Integer.valueOf(partsB[0].trim());
                String rawEnd = partsB[1].trim();
                if (rawEnd.startsWith(".")) {
                    rawEnd = rawEnd.substring(1);
                }
                end = Integer.valueOf(rawEnd);
                taxonId = null;
                parsedAs = "COLON_DOTS";
            } else if (COLON_DOTS_WITH_TAXON.matcher(range).matches()) {
                String[] partsA = range.split(":");
                taxonId = partsA[0];
                chr = partsA[1];
                String[] partsB = partsA[2].split("\\.\\.");
                start = Integer.valueOf(partsB[0].trim());
                String rawEnd = partsB[1].trim();
                if (rawEnd.startsWith(".")) {
                    rawEnd = rawEnd.substring(1);
                }
                end = Integer.valueOf(rawEnd);
                parsedAs = "COLON_DOTS_WITH_TAXON";
            } else if (COLON_DASH_WITH_TAXON.matcher(range).matches()) {
                String[] partsA = range.split(":");
                taxonId = partsA[0];
                chr = partsA[1];
                String[] partsB = partsA[2].split("-");
                start = Integer.valueOf(partsB[0].trim());
                end = Integer.valueOf(partsB[1].trim());
                parsedAs = "COLON_DASH_WITH_TAXON";
            } else if (COLON_START.matcher(range).matches()) {
                String[] partsA = range.split(":");
                chr = partsA[0];
                start = Integer.valueOf(partsA[1].trim());
                end = start;
                taxonId = null;
                parsedAs = "POINT";
            } else if (CHR_ONLY.matcher(range).matches()) {
                chr = range;
                start = null;
                end = null;
                taxonId = null;
                parsedAs = "CHR";
            } else {
                throw new IllegalArgumentException("Illegal range: " + range);
            }
            if (start != null && end != null && start > end) {
                throw new IllegalArgumentException(
                        "Illegal range - start is greater than end: " + range);
            }
        }

        /**
         * @return start of range
         */
        public Integer getStart() {
            return start;
        }

        /**
         * @return end of range
         */
        public Integer getEnd() {
            return end;
        }

        /**
         * @return identifier of chromosome
         */
        public String getChr() {
            return chr;
        }

        /** @return the taxon ID for this region, if there is one. **/
        public String getTaxonId() {
            return taxonId;
        }

        /** @return what we interpreted this range as **/
        public String getParsedAs() {
            return parsedAs;
        }
    }
}
