package org.intermine.api.query;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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
import org.intermine.metadata.ConstraintOp;
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
    protected static final int EMP_COUNT = 1000;
    
    protected Collection<String> ranges = Arrays.asList("2010-07-30 .. 2010-09-06");
    
    protected Results runQuery(String path, ConstraintOp op, Collection<String> ranges) throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews("Employee.name");
        pq.addConstraint(Constraints.eq("Employee.name", "temp*"));
        pq.addConstraint(new PathConstraintRange(path, op, ranges));
        Query q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        return res;
    }
    
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
        
        int col0 = (int) 'A';
        int col1 = (int) 'a';
        int col2 = (int) 'a';
        
        try {
            osw.beginTransaction();
            for (int i = 0; i < EMP_COUNT; i++) {
                
                Employee e = new Employee();
                e.setName(String.format("temp-employee-%03d", i));
                e.setAge(made + 20);
                e.setFullTime(i % 2 == 0);

                e.setEnd(String.format("%s%s%s",
                    Character.toString((char) col0),
                    Character.toString((char) col1),
                    Character.toString((char) col2)));
                col2++;
                if (col2 > (int) 'l') {
                    col2 = (int) 'a';
                    col1++;
                }
                if (col1 > (int) 'l') {
                    col1 = (int) 'a';
                    col0++;
                }

                EmploymentPeriod ep = new EmploymentPeriod();
                ep.setStartDate(when.getTime());
                ep.setEndDate(then.getTime());
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
    
    protected void showRow(Object o) {
        List row = (List) o;
        Employee emp = (Employee) row.get(0);
        showEmployee(emp);
        
    }

    protected void showEmployee(Employee e) {
        EmploymentPeriod ep = e.getEmploymentPeriod();
        System.out.printf("%s (%s .. %s)\n", e.getName(), ep.getStartDate(), ep.getEndDate());
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
            showRow(row);
        }
    }

    
    @Test
    public void testWithin() throws ObjectStoreException {
        Results res = runQuery("Employee.employmentPeriod", ConstraintOp.WITHIN, ranges);
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
        Results res = runQuery("Employee.employmentPeriod", ConstraintOp.OUTSIDE, ranges);

        for (Object row: res) {
            showRow(row);
            Employee emp = (Employee) ((List) row).get(0);
            assertTrue(
                String.format("%s does not start with temp-employee-91", emp.getName()),
                !emp.getName().startsWith("temp-employee-91")
            );
        }
        assertEquals(EMP_COUNT - 10, res.size());
    }
    

    @Test
    public void testOverlaps() throws ObjectStoreException {
        Results res = runQuery("Employee.employmentPeriod", ConstraintOp.OVERLAPS, ranges);
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStartDate(), emp.getEmploymentPeriod().getEndDate());
            assertTrue(emp.getAge() >= 901);
            assertTrue(emp.getAge() <= 968);
        }
        assertEquals(68, res.size());
    }
    

    @Test
    public void testDoesntOverlap() throws ObjectStoreException {
        Results res = runQuery("Employee.employmentPeriod", ConstraintOp.DOES_NOT_OVERLAP, ranges);
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStartDate(), emp.getEmploymentPeriod().getEndDate());
            assertTrue(emp.getAge() < 901 || emp.getAge() > 968);
        }
        assertEquals(EMP_COUNT - 68, res.size());
    }
    
    @Test
    public void testContains() throws ObjectStoreException {
        List<String> ranges = Arrays.asList("2010-07-30");
        Results res = runQuery("Employee.employmentPeriod", ConstraintOp.CONTAINS, ranges);
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStartDate(), emp.getEmploymentPeriod().getEndDate());
            //assertTrue(emp.getAge() < 901 || emp.getAge() > 968);
        }
        assertEquals(28, res.size());
    }

    @Test
    public void testDoesntContain() throws ObjectStoreException {
        List<String> ranges = Arrays.asList("2010-07-30");
        Results res = runQuery("Employee.employmentPeriod", ConstraintOp.DOES_NOT_CONTAIN, ranges);
        for (Object row: res) {
            List<Object> l = (List<Object>) row;
            Employee emp = (Employee) l.get(0);
            System.out.printf("%s (%s .. %s)\n", emp.getName(), emp.getEmploymentPeriod().getStartDate(), emp.getEmploymentPeriod().getEndDate());
            //assertTrue(emp.getAge() < 901 || emp.getAge() > 968);
        }
        assertEquals(EMP_COUNT - 28, res.size());
    }

}
