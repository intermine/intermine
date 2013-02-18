package org.intermine.api.bag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.InterMineBag;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Employable;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;

public class UnionOperationTest extends InterMineAPITestCase {

    public UnionOperationTest(String arg) {
        super(arg);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        insertData();
        setUpBags();
    }

    Set<Integer> employees1, employees2, managers, ceos, contractors, employables;
    private InterMineBag bagA;
    private InterMineBag bagB;
    private InterMineBag bagC;
    private InterMineBag bagD;
    private InterMineBag bagE;
    private InterMineBag bagF;
    
    private void insertData() throws Exception {
        System.out.println("Storing test data");
        Long start = System.currentTimeMillis();
        ObjectStoreWriter osw = os.getNewWriter();

        employees1 = new HashSet<Integer>();
        employees2 = new HashSet<Integer>();
        employables = new HashSet<Integer>();
        managers = new HashSet<Integer>();
        ceos = new HashSet<Integer>();
        for (int i = 65; i < 75; i++) {
            Employee emp = new Employee();
            emp.setName("Employee" + Character.toString((char) i));
            emp.setAge(i - 40);
            osw.store(emp);
            if (i % 2 == 0) {
                employees1.add(emp.getId()); // 5
            } else {
                employees2.add(emp.getId()); // 5
            }
            if (i % 3 == 0) {
                employables.add(emp.getId()); // 3
            }
        }
        for (int i = 65; i < 75; i++) {
            Employee emp = new Manager();
            emp.setName("Manager" + Character.toString((char) i));
            emp.setAge(i - 40);
            osw.store(emp);
            managers.add(emp.getId()); // 10
            if (i % 2 == 0) employees1.add(emp.getId()); // 5
            if (i % 3 == 0) employables.add(emp.getId()); // 3
        }
        for (int i = 65; i < 75; i++) {
            Employee emp = new CEO();
            emp.setName("CEO" + Character.toString((char) i));
            emp.setAge(i - 40);
            osw.store(emp);
            ceos.add(emp.getId()); // 10
            if (i % 2 == 0) managers.add(emp.getId()); // 5
            if (i % 3 == 0) employees1.add(emp.getId()); // 3
            if (i % 4 == 0) employables.add(emp.getId()); // 2
        }
        contractors = new HashSet<Integer>();
        for (int i = 65; i < 75; i++) {
            Contractor emp = new Contractor();
            emp.setName("Contractor" + Character.toString((char) i));
            osw.store(emp);
            contractors.add(emp.getId()); // 10
            if (i % 2 == 0) employables.add(emp.getId()); // 5
        }
        Set types = new HashSet(Arrays.asList(Employable.class));
        for (int i = 65; i < 75; i++) {
            Employable x = (Employable) DynamicUtil.createObject(types);
            x.setName("Employable" + Character.toString((char) i));
            osw.store(x);
            employables.add(x.getId()); // 10
        }

        osw.close();
        System.out.printf("Finished storing test data. Took %d ms.\n", System.currentTimeMillis() - start);
    }
    
    private void setUpBags() throws Exception {
        bagA = testUser.createBag("emp1", "Employee", "bag of emps", im.getClassKeys());
        bagB = testUser.createBag("emp2", "Employee", "bag of emps", im.getClassKeys());
        bagC = testUser.createBag("mans", "Manager", "bag of mans", im.getClassKeys());
        bagD = testUser.createBag("ceos", "CEO", "bag of ceos", im.getClassKeys());
        bagE = testUser.createBag("cons", "Contractor", "bag of things", im.getClassKeys());
        bagF = testUser.createBag("empabls", "Employable", "bag of things", im.getClassKeys());

        bagA.addIdsToBag(employees1, "Employee");
        bagB.addIdsToBag(employees2, "Employee");
        bagC.addIdsToBag(managers, "Manager");
        bagD.addIdsToBag(ceos, "CEO");
        bagE.addIdsToBag(contractors, "Contractor");
        bagF.addIdsToBag(employables, "Employable");
    }

    public void testTwoOfSameType_NoOverlap() throws Exception {
        BagOperation operation = new UnionOperation(os.getModel(), Arrays.asList(bagA, bagB), testUser);
        InterMineBag union = operation.operate();
        assertEquals("Employee", union.getType());
        assertEquals(18, union.getSize());
    }

    public void testUniversalUnion() throws Exception {
        BagOperation operation = new UnionOperation(os.getModel(), Arrays.asList(bagA, bagB, bagC, bagD, bagE, bagF), testUser);
        InterMineBag union = operation.operate();
        assertEquals("Employable", union.getType());
        assertEquals(50, union.getSize());
    }

    public void testDisjointUnion() throws Exception {
        BagOperation operation = new UnionOperation(os.getModel(), Arrays.asList(bagD, bagE), testUser);
        InterMineBag union = operation.operate();
        assertEquals("Employable", union.getType());
        assertEquals(20, union.getSize());
    }

    public void testLineage() throws Exception {
        BagOperation operation = new UnionOperation(os.getModel(), Arrays.asList(bagB, bagC, bagD), testUser);
        InterMineBag union = operation.operate();
        assertEquals("Employee", union.getType());
        assertEquals(25, union.getSize());
    }

    public void testSingleIsCopy() throws Exception {
        BagOperation operation = new UnionOperation(os.getModel(), Arrays.asList(bagE), testUser);
        InterMineBag union = operation.operate();
        assertEquals("Contractor", union.getType());
        assertEquals(10, union.getSize());
    }
}
