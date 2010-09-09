package org.intermine.api.util;

import java.util.Collections;

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
}
