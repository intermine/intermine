package org.intermine.api.query;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.intermine.model.testmodel.EmploymentPeriod;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.PathConstraintRange;
import org.junit.Before;
import org.junit.Test;

import static org.intermine.objectstore.query.ConstraintOp.*;

/**
 * Tests for RangeHelpers constructed using the AbstractHelper pattern.
 * @author Alex Kalderimis.
 *
 */
public class AbstractHelperTest {

    private final QueryClass empPeriod = new QueryClass(EmploymentPeriod.class);
    private final QueryField start = new QueryField(empPeriod, "start");
    private final QueryField end = new QueryField(empPeriod, "end");
    
    private final QueryValue startOf17May2008 = new QueryValue(new Date(1226880000000L));
    private final QueryValue endOf17May2008 = new QueryValue(new Date(1226966400000L));
    private final QueryValue startOfJan2012 = new QueryValue(new Date(1325376000000L));
    private final QueryValue endOfJan2012 = new QueryValue(new Date(1328054400000L));
    
    private final List<String> ranges = Arrays.asList("2008-11-17", "2012-01-01 .. 2012-01-31");
    
    private Date dateInQuestion = new Date(1226881234565L);
    
    @Before
    public void setup() {
        MainHelper.RangeConfig.rangeHelpers.put(EmploymentPeriod.class, new EmploymentPeriodHelper());
    }

    @Test
    public void within() throws Exception {

        ConstraintSet withinExp = new ConstraintSet(OR);
        
        ConstraintSet innerExp1 = new ConstraintSet(AND);
        innerExp1.addConstraint(new SimpleConstraint(start, GREATER_THAN_EQUALS, startOf17May2008));
        innerExp1.addConstraint(new SimpleConstraint(end, LESS_THAN, endOf17May2008));
        withinExp.addConstraint(innerExp1);
        
        ConstraintSet innerExp2 = new ConstraintSet(AND);
        innerExp2.addConstraint(new SimpleConstraint(start, GREATER_THAN_EQUALS, startOfJan2012));
        innerExp2.addConstraint(new SimpleConstraint(end, LESS_THAN, endOfJan2012));
        withinExp.addConstraint(innerExp2);
        
        PathConstraintRange con = new PathConstraintRange("EmploymentPeriod", WITHIN, ranges);
        org.intermine.objectstore.query.Constraint got = MainHelper.makeRangeConstraint(empPeriod, con);
        
        assertEquals(withinExp, got);
    }
    
    @Test
    public void outside() throws Exception {

        ConstraintSet outsideExp = new ConstraintSet(AND);
        
        ConstraintSet outsideInner1 = new ConstraintSet(OR);
        outsideInner1.addConstraint(new SimpleConstraint(start, ConstraintOp.LESS_THAN, startOf17May2008));
        outsideInner1.addConstraint(new SimpleConstraint(end, ConstraintOp.GREATER_THAN_EQUALS, endOf17May2008));
        outsideExp.addConstraint(outsideInner1);
        
        ConstraintSet outsideInner2 = new ConstraintSet(OR);
        outsideInner2.addConstraint(new SimpleConstraint(start, LESS_THAN, startOfJan2012));
        outsideInner2.addConstraint(new SimpleConstraint(end, GREATER_THAN_EQUALS, endOfJan2012));
        outsideExp.addConstraint(outsideInner2);
        
        Constraint got = MainHelper.makeRangeConstraint(empPeriod, new PathConstraintRange("EmploymentPeriod", OUTSIDE, ranges));
        assertEquals(outsideExp, got);
    }
    
    @Test
    public void overlaps() throws Exception {

        ConstraintSet exp = new ConstraintSet(OR);

        ConstraintSet inner1 = new ConstraintSet(AND);
        inner1.addConstraint(new SimpleConstraint(end, GREATER_THAN_EQUALS, startOf17May2008));
        inner1.addConstraint(new SimpleConstraint(start, LESS_THAN, endOf17May2008));
        exp.addConstraint(inner1);

        ConstraintSet inner2 = new ConstraintSet(AND);
        inner2.addConstraint(new SimpleConstraint(end, GREATER_THAN_EQUALS, startOfJan2012));
        inner2.addConstraint(new SimpleConstraint(start, LESS_THAN, endOfJan2012));
        exp.addConstraint(inner2);

        Constraint got = MainHelper.makeRangeConstraint(empPeriod, new PathConstraintRange("EmploymentPeriod", OVERLAPS, ranges));
        assertEquals(exp, got);
    }

    @Test
    public void doesntOverlap() throws Exception {

        ConstraintSet exp = new ConstraintSet(AND);

        ConstraintSet inner1 = new ConstraintSet(OR);
        inner1.addConstraint(new SimpleConstraint(end, LESS_THAN, startOf17May2008));
        inner1.addConstraint(new SimpleConstraint(start, GREATER_THAN_EQUALS, endOf17May2008));
        exp.addConstraint(inner1);

        ConstraintSet inner2 = new ConstraintSet(OR);
        inner2.addConstraint(new SimpleConstraint(end, LESS_THAN, startOfJan2012));
        inner2.addConstraint(new SimpleConstraint(start, GREATER_THAN_EQUALS, endOfJan2012));
        exp.addConstraint(inner2);

        Constraint got = MainHelper.makeRangeConstraint(empPeriod, new PathConstraintRange("EmploymentPeriod", DOES_NOT_OVERLAP, ranges));
        assertEquals(exp, got);
    }

    @Test
    public void contains() throws Exception {

        ConstraintSet exp = new ConstraintSet(OR);

        ConstraintSet inner1 = new ConstraintSet(AND);
        inner1.addConstraint(new SimpleConstraint(start, LESS_THAN, startOf17May2008));
        inner1.addConstraint(new SimpleConstraint(end, GREATER_THAN_EQUALS, endOf17May2008));
        exp.addConstraint(inner1);

        ConstraintSet inner2 = new ConstraintSet(AND);
        inner2.addConstraint(new SimpleConstraint(start, LESS_THAN, startOfJan2012));
        inner2.addConstraint(new SimpleConstraint(end, GREATER_THAN_EQUALS, endOfJan2012));
        exp.addConstraint(inner2);

        Constraint got = MainHelper.makeRangeConstraint(empPeriod, new PathConstraintRange("EmploymentPeriod", CONTAINS, ranges));
        assertEquals(exp, got);
    }
    
    @Test
    public void doesntContain() throws Exception {

        ConstraintSet exp = new ConstraintSet(AND);

        ConstraintSet inner1 = new ConstraintSet(OR);
        inner1.addConstraint(new SimpleConstraint(start, GREATER_THAN_EQUALS, startOf17May2008));
        inner1.addConstraint(new SimpleConstraint(end, LESS_THAN, endOf17May2008));
        exp.addConstraint(inner1);

        ConstraintSet inner2 = new ConstraintSet(OR);
        inner2.addConstraint(new SimpleConstraint(start, GREATER_THAN_EQUALS, startOfJan2012));
        inner2.addConstraint(new SimpleConstraint(end, LESS_THAN, endOfJan2012));
        exp.addConstraint(inner2);

        Constraint got = MainHelper.makeRangeConstraint(empPeriod, new PathConstraintRange("EmploymentPeriod", DOES_NOT_CONTAIN, ranges));
        assertEquals(exp, got);
    }
}
