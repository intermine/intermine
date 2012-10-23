package org.intermine.api.query;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employable;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraintMultitype;
import org.intermine.pathquery.PathQuery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiTypeIntegrationTest {

    private static ObjectStoreWriter osw;
    private static final int SET_SIZE = 10;
    private static int made = 0;
    
    private static Department tempDepartment;
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        } catch (Exception e) {
            System.err.println("Error connecting to DB");
            System.err.println(e);
            return;
        }
        made = 0;
        try {
            osw.beginTransaction();
            Department d = new Department();
            d.setName("temp-department");
            osw.store(d);
            tempDepartment = d;
            
            for (int i = 0; i < SET_SIZE; i++) {
                Employee e = new Employee();
                e.setName(String.format("temp-employee-%03d", i));
                e.setAge(i);
                e.setDepartment(d);
                osw.store(e);
                made++;
            }
            for (int i = 0; i < SET_SIZE; i++) {
                Manager e = new Manager();
                e.setName(String.format("temp-employee-%03d", i));
                e.setAge(i);
                e.setDepartment(d);
                osw.store(e);
                made++;
            }
            for (int i = 0; i < SET_SIZE; i++) {
                Employee e = new CEO();
                e.setName(String.format("temp-employee-%03d", i));
                e.setAge(i);
                e.setDepartment(d);
                osw.store(e);
                made++;
            }
            for (int i = 0; i < SET_SIZE; i++) {
                Contractor e = new Contractor();
                e.setName(String.format("temp-employee-%03d", i));
                osw.store(e);
                made++;
            }
            osw.commitTransaction();
            System.out.printf("Made %d employables\n", made);
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
                osw.delete(tempDepartment);
                PathQuery pq = new PathQuery(osw.getModel());
                pq.addView("Employable.id");
                pq.addConstraint(Constraints.eq("Employable.name", "temp*"));

                Query q = MainHelper.makeQuery(
                    pq, new HashMap(), new HashMap(), null, new HashMap());

                Results res = osw.execute(q, 50000, true, false, true);
                for (Object row: res) {
                    Employable emp = (Employable) ((List) row).get(0);
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
    }
    
    @After
    public void teardown() throws ObjectStoreException {
        if (osw != null) {
            osw.close();
        }
    }

    private int runQuery(ConstraintOp op, Collection<String> types) throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Employable.name");
        pq.addConstraint(new PathConstraintMultitype("Employable", op, types));
        
        Query q = MainHelper.makeQuery(
                pq, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        return res.size();
    }

    @Test
    public void isa() throws ObjectStoreException {
        Collection<String> types = Arrays.asList("CEO", "Contractor");
        assertEquals(runQuery(ConstraintOp.ISA, types), types.size() * SET_SIZE);
    }

    @Test
    public void isnt() throws ObjectStoreException {
        Collection<String> types = Arrays.asList("CEO", "Contractor", "Manager");
        assertEquals(runQuery(ConstraintOp.ISNT, types), made - (types.size() * SET_SIZE));
    }
    
    @Test
    public void isntOnRef() throws ObjectStoreException {
        Collection<String> types = Arrays.asList("CEO", "Contractor");
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addView("Department.employees.name");
        pq.addConstraint(Constraints.eq("Department.name", tempDepartment.getName()));
        PathQuery all = pq.clone();
        pq.addConstraint(new PathConstraintMultitype("Department.employees", ConstraintOp.ISNT, Arrays.asList("Manager", "CEO")));

        Query q = MainHelper.makeQuery(
                pq, new HashMap(), new HashMap(), null, new HashMap());
        Query qall = MainHelper.makeQuery(
                all, new HashMap(), new HashMap(), null, new HashMap());

        Results res = osw.execute(q, 50000, true, false, true);
        Results resall = osw.execute(qall, 50000, true, false, true);
        
        assertEquals(res.size(), resall.size() - (types.size() * SET_SIZE));
    }

}
