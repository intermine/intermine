package org.intermine.model.testmodel.query.range;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.query.RangeHelper;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Book;
import org.intermine.model.testmodel.Section;
import org.intermine.objectstore.query.Constraint;
import org.intermine.metadata.ConstraintOp;
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

/**
 * A range helper that will interpret text locations.
 * @author Alex Kalderimis
 *
 */
public class TextLocationHelper implements RangeHelper {

    private final static QueryClass book = new QueryClass(Book.class);
    private final static QueryClass section = new QueryClass(Section.class);
    private final static QueryField ident = new QueryField(book, "identifier");

    /**
     * Constructor.
     */
    public TextLocationHelper() {
        Model model = Model.getInstanceByName("testmodel");
        if (model == null) {
            throw new RuntimeException("No testmodel is available");
        }
    }

    @Override
    public Constraint createConstraint(Queryable q, QueryNode node, PathConstraintRange con) {

        if (q instanceof Query) {
            ((Query) q).addFrom(book);
            ((Query) q).addFrom(section);
        } else if (q instanceof QueryCollectionPathExpression) {
            ((QueryCollectionPathExpression) q).addFrom(book);
            ((QueryCollectionPathExpression) q).addFrom(section);
        }

        QueryField leftA = new QueryField((QueryClass) node, "start");
        QueryField leftB = new QueryField((QueryClass) node, "end");
        QueryObjectReference sectionRef = new QueryObjectReference((QueryClass) node, "foundIn");

        OverlapRange left = new OverlapRange(leftA, leftB, sectionRef);
        ConstraintOp op = con.getOp();
        ConstraintOp rangeOp = op;
        if (op == ConstraintOp.WITHIN) {
            rangeOp = ConstraintOp.IN;
        } else if (op == ConstraintOp.OUTSIDE) {
            rangeOp = ConstraintOp.NOT_IN;
        }
        ConstraintOp mainOp = (op == ConstraintOp.WITHIN || op == ConstraintOp.CONTAINS || op == ConstraintOp.OVERLAPS)
                ? ConstraintOp.OR : ConstraintOp.AND;
        ConstraintSet mainSet = new ConstraintSet(mainOp);

        for (String value: con.getValues()) {
            TextRange range = makeTextRange(value);

            ConstraintSet rangeSet = new ConstraintSet(ConstraintOp.AND);
            rangeSet.addConstraint(new ContainsConstraint(sectionRef, ConstraintOp.CONTAINS, section));
            QueryObjectReference bref = new QueryObjectReference(section, "book");
            rangeSet.addConstraint(new ContainsConstraint(bref, ConstraintOp.CONTAINS, book));
            rangeSet.addConstraint(new SimpleConstraint(ident, ConstraintOp.EQUALS, new QueryValue(range.getIdent())));
            OverlapRange right = range.getOverlapRange(sectionRef);
            if (right != null) {
                rangeSet.addConstraint(new OverlapConstraint(left, rangeOp, right));
            }
            mainSet.addConstraint(rangeSet);
        }
        return mainSet;
    }

    private static final Pattern COLON_DASH = Pattern.compile("^[^:]+:\\d+-\\d+$");
    private static final Pattern COLON_DOTS = Pattern.compile("^[^:]+:\\d+\\.\\.\\d+$");
    private static final Pattern COLON_START = Pattern.compile("^[^:]+:\\d+$");
    private static final Pattern WHOLE_REF = Pattern.compile("^[^:]+$");

    private TextRange makeTextRange(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("value must not be blank.");
        }
        if (COLON_DASH.matcher(value).matches()) {
            String[] parts = value.split(":");
            String[] positions = parts[1].split("-");
            return new TextRange(parts[0], Integer.valueOf(positions[0]), Integer.valueOf(positions[1]));
        } else if (COLON_DOTS.matcher(value).matches()) {
            String[] parts = value.split(":");
            String[] positions = parts[1].split("\\.\\.");
            return new TextRange(parts[0], Integer.valueOf(positions[0]), Integer.valueOf(positions[1]));
        } else if (COLON_START.matcher(value).matches()) {
            String[] parts = value.split(":");
            return new TextRange(parts[0], Integer.valueOf(parts[1]));
        } else if (WHOLE_REF.matcher(value).matches()) {
            return new TextRange(value);
        } else {
            throw new IllegalArgumentException("value is not is a valid text range format.");
        }
    }

    static final class TextRange {

        private final Integer start, end;
        
        public Integer getStart() {
            return start;
        }

        public Integer getEnd() {
            return end;
        }

        public String getIdent() {
            return ident;
        }

        private final String ident;

        TextRange(String ident, Integer start, Integer end) {
            this.ident = ident;
            this.start = start;
            this.end = end;
        }

        TextRange(String ident, Integer point) {
            this.ident = ident;
            this.start = point;
            this.end = point;
        }

        TextRange(String ident) {
            this.ident = ident;
            this.start = null;
            this.end = null;
        }

        @Override
        public String toString() {
            return "TextRange [start=" + start + ", end=" + end + ", ident="
                    + ident + "]";
        }

        OverlapRange getOverlapRange(QueryObjectReference section) {
            if (start != null && end != null) {
                return new OverlapRange(new QueryValue(start), new QueryValue(end), section);
            } else {
                return null;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((ident == null) ? 0 : ident.hashCode());
            result = prime * result + ((end == null) ? 0 : end.hashCode());
            result = prime * result + ((start == null) ? 0 : start.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TextRange other = (TextRange) obj;
            if (ident == null) {
                if (other.ident != null)
                    return false;
            } else if (!ident.equals(other.ident))
                return false;
            if (end == null) {
                if (other.end != null)
                    return false;
            } else if (!end.equals(other.end))
                return false;
            if (start == null) {
                if (other.start != null)
                    return false;
            } else if (!start.equals(other.start))
                return false;
            return true;
        }

    }

}
