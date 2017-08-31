package org.intermine.api.util;

import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;

public class PathUtilTest extends TestCase {

    private Model model;

    protected void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public void testResolveShort() throws Exception {
        Path path = new Path(model, "Department.name");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department.setName("department name");
        assertEquals("department name", PathUtil.resolvePath(path, department));
    }


    public void testResolve() throws Exception {
        Path path = new Path(model, "Department.company.name");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        department.setName("department name");
        company.setName("company name");
        department.setCompany(company);
        assertEquals("company name", PathUtil.resolvePath(path, department));
    }

    public void testResolveLong() throws Exception {
        Path path = new Path(model, "Department.company.CEO.name");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        CEO ceo =
            (CEO) DynamicUtil.createObject(Collections.singleton(CEO.class));
        department.setName("department name");
        company.setName("company name");
        ceo.setName("ceo name");
        department.setCompany(company);
        company.setcEO(ceo);
        assertEquals("ceo name", PathUtil.resolvePath(path, department));
    }

    public void testResolveReference() throws Exception {
        Path path = new Path(model, "Department.company.CEO");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        CEO ceo =
            (CEO) DynamicUtil.createObject(Collections.singleton(CEO.class));
        department.setName("department name");
        company.setName("company name");
        ceo.setName("ceo name");
        department.setCompany(company);
        company.setcEO(ceo);
        CEO resCEO = (CEO) PathUtil.resolvePath(path, department);
        assertEquals("ceo name", resCEO.getName());
    }

    public void testResolveReferenceWithClassConstraint() throws Exception {
        Path path = new Path(model, "Department.manager[CEO].company");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        CEO ceo =
            (CEO) DynamicUtil.createObject(Collections.singleton(CEO.class));
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        department.setName("department name");
        ceo.setName("ceo name");
        company.setName("company name");
        department.setManager(ceo);
        ceo.setCompany(company);
        Company resCompany = (Company) PathUtil.resolvePath(path, department);
        assertEquals("company name", resCompany.getName());
    }

    public void testResolvePathOneElement() throws Exception {
        Path path = new Path(model, "Department");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department.setId(10);
        assertEquals(department.getId(), ((Department) PathUtil.resolvePath(path, department)).getId());
    }

    public void testNotSuperclass() throws Exception {
        Path path = new Path(model, "Department.name");
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        try {
            PathUtil.resolvePath(path, c);
            fail("Expected exception");
        } catch (PathException e) {
            // Fine
        }
    }

    public void testNullRef() throws Exception {
        Path path = new Path(model, "Department.company.name");
        Department d = new Department();
        assertNull(PathUtil.resolvePath(path, d));
    }

    public void testCollections() throws Exception {
        Path path = new Path(model, "Company.departments.name");
        Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        try {
            PathUtil.resolvePath(path, c);
            fail("Expected exception");
        } catch (RuntimeException e) {
            // Fine
        }
    }

    public void testCanAssignObjectToType() throws Exception {
        Employee e = (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        assertTrue(PathUtil.canAssignObjectToType(Employee.class, e));

        Manager m = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        assertTrue(PathUtil.canAssignObjectToType(Employee.class, m));

        assertFalse(PathUtil.canAssignObjectToType(Manager.class, e));
        assertFalse(PathUtil.canAssignObjectToType(Department.class, e));
    }

    public void testResolveCollections() throws Exception {
        Path path = new Path(model, "Company.departments.name");
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setName("company name");

        Department department1 =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department1.setName("department name 1");
        Department department2 =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department2.setName("department name 2");

        company.addDepartments(department1);
        company.addDepartments(department2);

        // PathUtil return set of objects, assert with a set of objects
        HashSet<String> departmentsSet = new HashSet<String>();
        departmentsSet.add("department name 1");
        departmentsSet.add("department name 2");
        assertEquals(departmentsSet, PathUtil.resolveCollectionPath(path, company));
    }

    /**
     * This test contains a collection inside a collection, expect one level deep Set
     * @throws Exception
     */
    public void testResolveCollectionOfCollections() throws Exception {
        Path path = new Path(model, "Company.departments.employees.name");
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setName("Initech");

        Department department1 =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department1.setName("Office Space");
        department1.setCompany(company);

        Department department2 =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department2.setName("Storage Room B");
        department2.setCompany(company);

        Employee employee1 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee1.setName("Peter Gibbons");
        employee1.setDepartment(department1);

        Employee employee2 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee2.setName("Michael Bolton");
        employee2.setDepartment(department1);

        Employee employee3 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee3.setName("Samir Nagheenanajar");
        employee3.setDepartment(department2);

        Employee employee4 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee4.setName("Tom Smykowski");
        employee4.setDepartment(department2);

        department1.addEmployees(employee1);
        department1.addEmployees(employee2);

        department2.addEmployees(employee3);
        department2.addEmployees(employee4);

        company.addDepartments(department1);
        company.addDepartments(department2);

        // PathUtil return set of objects, assert with a set of objects
        HashSet<String> employeesSet = new HashSet<String>();
        employeesSet.add("Peter Gibbons");
        employeesSet.add("Michael Bolton");
        employeesSet.add("Samir Nagheenanajar");
        employeesSet.add("Tom Smykowski");
        assertEquals(employeesSet, PathUtil.resolveCollectionPath(path, company));
    }

    /**
     * This test makes use of collections (departments, employees) and reverse references (department, company)
     * @throws Exception
     */
    public void testResolveCollectionOfCollectionsWithReverseReferences() throws Exception {
        Path path = new Path(model, "Company.departments.employees.department.company.departments.employees.name");

        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company.setName("Initech");

        Department department1 =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department1.setName("Office Space");
        department1.setCompany(company);

        Department department2 =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department2.setName("Storage Room B");
        department2.setCompany(company);

        Employee employee1 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee1.setName("Peter Gibbons");
        employee1.setDepartment(department1);

        Employee employee2 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee2.setName("Michael Bolton");
        employee2.setDepartment(department1);

        Employee employee3 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee3.setName("Samir Nagheenanajar");
        employee3.setDepartment(department2);

        Employee employee4 =
            (Employee) DynamicUtil.createObject(Collections.singleton(Employee.class));
        employee4.setName("Tom Smykowski");
        employee4.setDepartment(department2);

        department1.addEmployees(employee1);
        department1.addEmployees(employee2);

        department2.addEmployees(employee3);
        department2.addEmployees(employee4);

        company.addDepartments(department1);
        company.addDepartments(department2);

        // PathUtil return set of objects, assert with a set of objects
        HashSet<String> employeesSet = new HashSet<String>();
        employeesSet.add("Peter Gibbons");
        employeesSet.add("Michael Bolton");
        employeesSet.add("Samir Nagheenanajar");
        employeesSet.add("Tom Smykowski");
        assertEquals(employeesSet, PathUtil.resolveCollectionPath(path, company));
    }

}
