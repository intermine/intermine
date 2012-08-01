package org.intermine.api.query;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.EmploymentPeriod;
import org.intermine.model.testmodel.Secretary;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathQuery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EmploymentPeriodHelperIntegrationTest {

    private static ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(EmploymentPeriodHelperIntegrationTest.class);
    private static final int EMP_COUNT = 1000;

    @BeforeClass
    public static void loadData() throws ObjectStoreException {
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        } catch (Exception e) {
            System.err.println("Error connecting to DB");
            System.err.println(e);
            return;
        }
        int made = 0;
        Calendar when = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        when.set(2008, 1, 1, 0, 0, 0);
        when.set(Calendar.HOUR_OF_DAY, 0);
        when.set(Calendar.MINUTE, 0);
        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);
        Calendar then = (Calendar) when.clone();
        then.add(Calendar.MONTH, 1);
        
        try {
            osw.beginTransaction();
            for (int i = 0; i < EMP_COUNT; i++) {
                
                Employee e = new Employee();
                e.setName(String.format("temp-employee-%03d", i));
                e.setAge(made + 20);
                e.setFullTime(i % 2 == 0);
                
                EmploymentPeriod ep = new EmploymentPeriod();
                ep.setStart(when.getTime());
                ep.setEnd(then.getTime());
                osw.store(ep);
                
                e.setEmploymentPeriod(ep);

                // Advance the dates.
                when.add(Calendar.DATE, 1);
                then.add(Calendar.DATE, 1);

                osw.store(e);
                made++;
            }
            osw.commitTransaction();
            System.out.printf("[START UP] Made %d employees\n", made);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (osw != null) {
                if (osw.isInTransaction()) {
                    osw.abortTransaction();
                }
                osw.close();
            }
        }
    }
    
    @After
    public void teardown() throws ObjectStoreException {
        if (osw != null) {
            osw.close();
        }
    }
    
    @AfterClass
    public static void shutdown() {
        int deleted = 0;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        } catch (Exception e) {
            System.err.println("Error connecting to DB");
            System.err.println(e);
        }
        if (osw != null) {
            try {
                osw.beginTransaction();
                PathQuery pq = new PathQuery(osw.getModel());
                pq.addView("Employee.id");
                pq.addConstraint(Constraints.eq("Employee.name", "temp*"));

                Query q = MainHelper.makeQuery(
                    pq, new HashMap(), new HashMap(), null, new HashMap());

                Results res = osw.execute(q, 50000, true, false, true);
                for (Object row: res) {
                    Employee emp = (Employee) ((List) row).get(0);
                    if (emp.getEmploymentPeriod() != null) {
                        osw.delete(emp.getEmploymentPeriod());
                        deleted++;
                    }
                    osw.delete(emp);
                    deleted++;
                }
                osw.commitTransaction();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }finally {
                try {
                    osw.close();
                } catch (Exception e) {
                    System.err.print(e);
                }
            }
        }
        System.out.printf("\n[CLEAN UP] Deleted %d things\n", deleted);
    }
    
    @Before
    public void setup() throws ObjectStoreException {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        MainHelper.RangeConfig.rangeHelpers.put(EmploymentPeriod.class, new EmploymentPeriodHelper());
    }
    
    @Test
    public void testSanity() throws ObjectStoreException {
        
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.id", "Employee.employmentPeriod.id");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        
        Query q = MainHelper.makeQuery(
                pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        assertEquals(res.size(), EMP_COUNT);
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            EmploymentPeriod ep = (EmploymentPeriod) l.get(1);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), ep.getStart(), ep.getEnd());
        }
    }
    
    @Test
    public void testWithin() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        List<String> ranges = Arrays.asList("2010-07-30 .. 2010-09-06");
        pq.addConstraint(
            new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.WITHIN, ranges));
        
        Query q = MainHelper.makeQuery(
                pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        assertEquals(10, res.size());
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            assertTrue(
                String.format("%s starts with temp-employee-91", emp.getName()),
                emp.getName().startsWith("temp-employee-91")
            );
        }
    }

    @Test
    public void testOutside() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        List<String> ranges = Arrays.asList("2010-07-30 .. 2010-09-06");
        pq.addConstraint(
            new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.OUTSIDE, ranges));

        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStart(), emp.getEmploymentPeriod().getEnd());
            assertTrue(
                String.format("%s does not start with temp-employee-91", emp.getName()),
                !emp.getName().startsWith("temp-employee-91")
            );
        }
        assertEquals(EMP_COUNT - 10, res.size());
    }
    

    @Test
    public void testOverlaps() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        List<String> ranges = Arrays.asList("2010-07-30 .. 2010-09-06");
        pq.addConstraint(
            new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.OVERLAPS, ranges));

        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStart(), emp.getEmploymentPeriod().getEnd());
            assertTrue(emp.getAge() >= 901);
            assertTrue(emp.getAge() <= 968);
        }
        assertEquals(68, res.size());
    }
    

    @Test
    public void testDoesntOverlap() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        List<String> ranges = Arrays.asList("2010-07-30 .. 2010-09-06");
        pq.addConstraint(
            new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.DOES_NOT_OVERLAP, ranges));

        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStart(), emp.getEmploymentPeriod().getEnd());
            assertTrue(emp.getAge() < 901 || emp.getAge() > 968);
        }
        assertEquals(EMP_COUNT - 68, res.size());
    }
    
    @Test
    public void testContains() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        List<String> ranges = Arrays.asList("2010-07-30");
        pq.addConstraint(
            new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.CONTAINS, ranges));

        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStart(), emp.getEmploymentPeriod().getEnd());
            //assertTrue(emp.getAge() < 901 || emp.getAge() > 968);
        }
        assertEquals(28, res.size());
    }

    @Test
    public void testDoesntContain() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        List<String> ranges = Arrays.asList("2010-07-30");
        pq.addConstraint(
            new PathConstraintRange("Employee.employmentPeriod", ConstraintOp.DOES_NOT_CONTAIN, ranges));

        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStart(), emp.getEmploymentPeriod().getEnd());
            //assertTrue(emp.getAge() < 901 || emp.getAge() > 968);
        }
        assertEquals(EMP_COUNT - 28, res.size());
    }

}
