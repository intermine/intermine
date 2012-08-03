package org.intermine.bio.query.range;

import java.util.regex.Pattern;

import org.intermine.api.query.RangeHelper;
import org.intermine.model.bio.Chromosome;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.OverlapConstraint;
import org.intermine.objectstore.query.OverlapRange;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionPathExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Queryable;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;

public class ChromosomeLocationHelper implements RangeHelper
{
    private QueryClass chromosome = new QueryClass(Chromosome.class);
    private QueryField chrIdField = new QueryField(chromosome, "primaryIdentifier");
    
    @Override
    public Constraint createConstraint(Queryable q, QueryNode n, PathConstraintRange pcr) {
        
        if (q instanceof Query) {
            ((Query) q).addFrom(chromosome);
        } else if (q instanceof QueryCollectionPathExpression) {
            ((QueryCollectionPathExpression) q).addFrom(chromosome);
        }
        
        QueryField leftA = new QueryField((QueryClass) n, "start");
        QueryField leftB = new QueryField((QueryClass) n, "start");
        QueryObjectReference qor = new QueryObjectReference((QueryClass) n, "feature");
        QueryObjectReference chrOR = new QueryObjectReference((QueryClass) n, "locatedOn");
        OverlapRange left = new OverlapRange(leftA, leftB, qor);
        ConstraintOp op = pcr.getOp();
        ConstraintOp rangeOp = op;
        if (op == ConstraintOp.WITHIN) {
            rangeOp = ConstraintOp.IN;
        } else if (op == ConstraintOp.OUTSIDE) {
            rangeOp = ConstraintOp.NOT_IN;
        }
        
        ConstraintOp mainOp = (op == ConstraintOp.WITHIN || op == ConstraintOp.CONTAINS || op == ConstraintOp.OVERLAPS)
                ? ConstraintOp.OR : ConstraintOp.AND;
        ConstraintSet mainSet = new ConstraintSet(mainOp);
        for (String range: pcr.getValues()) {
            GenomicInterval interval = new GenomicInterval(range);
            String chrId = interval.getChr();
            
            ConstraintSet rangeSet = new ConstraintSet(ConstraintOp.AND);
            
            rangeSet.addConstraint(new ContainsConstraint(chrOR, ConstraintOp.CONTAINS, chromosome));
            rangeSet.addConstraint(new SimpleConstraint(chrIdField, ConstraintOp.EQUALS, new QueryValue(chrId)));
            
            if (interval.getEnd() != null) {
                OverlapRange right = new OverlapRange(new QueryValue(interval.getStart()), new QueryValue(interval.getEnd()), qor);
                rangeSet.addConstraint(new OverlapConstraint(left, rangeOp, right));
            } else if (interval.getStart() != null) {
                OverlapRange right = new OverlapRange(new QueryValue(interval.getStart()), new QueryValue(interval.getStart()), qor);
                rangeSet.addConstraint(new OverlapConstraint(left, rangeOp, right));
            } else {
                // Chromosome only - no action needed.
            }
            mainSet.addConstraint(rangeSet);
        }
        return mainSet;
    }

    static class GenomicInterval {

        private final Integer start, end;
        
        private final String chr;
      
        private static final Pattern GFF3 = Pattern.compile("^[^\\t]+\\t[^\\t]+\\t[^\\t]+\\d+\\t\\d+");
        private static final Pattern BED = Pattern.compile("^[^\\t]+\\t\\d+\\t\\d+");
        private static final Pattern COLON_DASH = Pattern.compile("^[^:]+:\\d+-\\d+");
        private static final Pattern COLON_DOTS = Pattern.compile("^[^:]+:\\d+\\.\\.\\d+");
        private static final Pattern COLON_START = Pattern.compile("^[^:]+:\\d+$");
        private static final Pattern CHR_ONLY = Pattern.compile("^[^:]+$");

        GenomicInterval(String range) {
            if (range == null) {
                throw new NullPointerException("range may not be null");
            }
            if (GFF3.matcher(range).matches()) {
                String[] parts = range.split("\\t");
                chr = parts[0].trim();
                start = Integer.valueOf(parts[3].trim());
                end = Integer.valueOf(parts[4].trim());
            } else if (BED.matcher(range).matches()) {
                String[] parts = range.split("\\t");
                chr = parts[0].trim();
                start = Integer.valueOf(parts[1].trim());
                end = Integer.valueOf(parts[2].trim());
            } else if (COLON_DASH.matcher(range).matches()) {
                String[] partsA = range.split(":");
                chr = partsA[0];
                String[] partsB = partsA[1].split("-");
                start = Integer.valueOf(partsB[0].trim());
                end = Integer.valueOf(partsB[1].trim());
            } else if (COLON_DOTS.matcher(range).matches()) {
                String[] partsA = range.split(":");
                chr = partsA[0];
                String[] partsB = partsA[1].split("\\.\\.");
                start = Integer.valueOf(partsB[0].trim());
                end = Integer.valueOf(partsB[1].trim());
            } else if (COLON_START.matcher(range).matches()) {
                String[] partsA = range.split(":");
                chr = partsA[0];
                start = Integer.valueOf(partsA[1].trim());
                end = start;
            } else if (CHR_ONLY.matcher(range).matches()) {
                chr = range;
                start = null;
                end = null;
            } else {
                throw new IllegalArgumentException("Illegal range: " + range);
            }
            if (start != null && end != null && start > end) {
                throw new IllegalArgumentException("Illegal range - start is greater than end: " + range);
            }
        }

        public Integer getStart() {
            return start;
        }

        public Integer getEnd() {
            return end;
        }
        
        public String getChr() {
            return chr;
        }
    }
}
