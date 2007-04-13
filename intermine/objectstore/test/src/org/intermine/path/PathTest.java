package org.intermine.path;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.util.DynamicUtil;

import junit.framework.TestCase;

/**
 * Tests for the Path class.
 *
 * @author Kim Rutherford
 */

public class PathTest extends TestCase
{
    private Model model;

    public PathTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public void testValid() {
        String stringPath = "Department.company";
        Path path = new Path(model, stringPath);
        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Department");
        assertEquals(cld, path.getStartClassDescriptor());
        FieldDescriptor fld = cld.getFieldDescriptorByName("company");
        assertEquals(fld, path.getEndFieldDescriptor());
        ClassDescriptor compDesc =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        assertEquals(compDesc, path.getEndClassDescriptor());
        assertFalse(path.containsCollections());
        assertEquals(stringPath, path.toString());
    }

    public void testValid2() {
        String stringPath = "Department.company.name";
        Path path = new Path(model, stringPath);
        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Department");
        assertEquals(String.class, path.getEndType());
        assertEquals(cld, path.getStartClassDescriptor());
    }

    public void testValidWithClassConstraint() {
        String stringPath = "Department.manager[CEO].company.departments.employees[Manager].seniority";
        Path path = new Path(model, stringPath);

        checkConstrainedPath(path);
    }

    public void testValidWithClassConstraintMap() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department.manager", "CEO");
        constraintMap.put("Department.manager.company.departments.employees", "Manager");

        String stringPath = "Department.manager.company.departments.employees.seniority";
        Path path = new Path(model, stringPath, constraintMap);

        checkConstrainedPath(path);
    }

    private void checkConstrainedPath(Path path) {
        ClassDescriptor ceoCld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.CEO");
        assertEquals(ceoCld, ((ReferenceDescriptor) path.getElements().get(1)).getClassDescriptor());
        ClassDescriptor manCld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.ImportantPerson");
        assertEquals(manCld, ((FieldDescriptor) path.getElements().get(4)).getClassDescriptor());
        assertEquals(String.class, path.getEndType());
        ClassDescriptor deptCld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Department");
        assertEquals(deptCld, path.getStartClassDescriptor());
        assertEquals(String.class, path.getEndType());
    }

    public void testNullPath() {
        try {
            Path path = new Path(model, null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            Path path = new Path(model, "");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testShortPath() {
        String stringPath = "Department";
        Path path = new Path(model, stringPath);
        ClassDescriptor cld =
            model.getClassDescriptorByName("org.intermine.model.testmodel.Department");
        assertEquals(cld, path.getStartClassDescriptor());
        assertEquals(cld, path.getEndClassDescriptor());
        assertNull(path.getEndFieldDescriptor());
        assertNull(path.getEndType());
    }

    public void testResolveShort() {
        Path path = new Path(model, "Department.name");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        department.setName("department name");
        assertEquals("department name", path.resolve(department));
    }

    public void testResolve() {
        Path path = new Path(model, "Department.company.name");
        Department department =
            (Department) DynamicUtil.createObject(Collections.singleton(Department.class));
        Company company =
            (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        department.setName("department name");
        company.setName("company name");
        department.setCompany(company);
        assertEquals("company name", path.resolve(department));
    }

    public void testResolveLong() {
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
        assertEquals("ceo name", path.resolve(department));
    }

    public void testResolveReference() {
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
        CEO resCEO = (CEO) path.resolve(department);
        assertEquals("ceo name", resCEO.getName());
    }

    public void testResolveReferenceWithClassConstraint() {
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
        Company resCompany = (Company) path.resolve(department);
        assertEquals("company name", resCompany.getName());
    }

    public void testToString() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department.manager", "CEO");
        constraintMap.put("Department.manager.company.departments.employees", "Manager");

        String stringPath = "Department.manager.company.departments.employees.seniority";
        Path path = new Path(model, stringPath, constraintMap);

        assertEquals("Department.manager[CEO].company.departments.employees[Manager].seniority",
                     path.toString());
    }

    public void testToStringNoConstraints() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department.manager", "CEO");
        constraintMap.put("Department.manager.company.departments.employees", "Manager");

        String stringPath = "Department.manager.company.departments.employees.seniority";
        Path path = new Path(model, stringPath, constraintMap);

        assertEquals("Department.manager.company.departments.employees.seniority",
                     path.toStringNoConstraints());
    }

    public void testGetLastClassDescriptor() {
        Path path = new Path(model, "Department.manager.name");
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.Manager"), path.getLastClassDescriptor());
    }

    public void testEquals() {
        Path path1 = new Path(model, "Department.manager.name");
        Path path2 = new Path(model, "Department.manager.name");
        assertEquals(path1, path2);
    }
}
