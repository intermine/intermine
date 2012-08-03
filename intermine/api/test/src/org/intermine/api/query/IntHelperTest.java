package org.intermine.api.query;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Results;
import org.junit.Before;

public class IntHelperTest extends EmploymentPeriodHelperIntegrationTest {

    protected String path;
    protected int withinExp;

    @Before
    public void setRange() {
        this.ranges = Arrays.asList("500 .. 509", "800 .. 800");
    }
    
    @Before
    public void setPath() {
        this.path = "Employee.age";
    }
    
    @Before
    public void setExpectations() {
        this.withinExp = 11;
    }

    @Override
    public void testWithin() throws ObjectStoreException {
        testWithin(ConstraintOp.WITHIN);
    }

    @Override
    public void testOverlaps() throws ObjectStoreException {
        testWithin(ConstraintOp.OVERLAPS);
    }
    
    @Override
    protected void showEmployee(Employee e) {
        System.out.printf("%-20s is %d\n", e.getName(), e.getAge());
    }

    protected void testWithin(ConstraintOp op) throws ObjectStoreException {
        Results res = runQuery(path, op, ranges);
        for (Object o: res) {
            showRow(o);
        }
        assertEquals(withinExp, res.size());
    }

    @Override
    public void testOutside() throws ObjectStoreException {
        testOutside(ConstraintOp.OUTSIDE);
    }

    @Override
    public void testDoesntOverlap()  throws ObjectStoreException {
        testOutside(ConstraintOp.DOES_NOT_OVERLAP);
    }

    private void testOutside(ConstraintOp op) throws ObjectStoreException {
        Results res = runQuery(path, op, ranges);
        Set<Integer> ages = new TreeSet<Integer>();
        for (int i = 20; i < 1000; i++) {
            ages.add(Integer.valueOf(i));
        }
        for (Object o: res) {
            List row = (List) o;
            Employee e = (Employee) row.get(0);
            ages.remove(e.getAge());
        }
        System.out.printf("Excluded ages: %s\n", ages);
        assertEquals(EMP_COUNT - withinExp, res.size());
    }

    @Override
    public void testContains() throws ObjectStoreException {
        checkUnimplemented(ConstraintOp.CONTAINS);
    }
    
    private void checkUnimplemented(ConstraintOp op) throws ObjectStoreException {
        try {
            runQuery("Employee.age", op, ranges);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().startsWith("Unimplemented behaviour"));
        }
    }
    
    @Override
    public void testDoesntContain() throws ObjectStoreException {
        checkUnimplemented(ConstraintOp.DOES_NOT_CONTAIN);
    }

}
