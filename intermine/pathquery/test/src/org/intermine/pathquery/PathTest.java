package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import junit.framework.TestCase;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.DynamicUtil;

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

    public void testNotValidWithClassConstraintMap() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department.manager", "CEO");
        constraintMap.put("Department.manager.company.departments.employees", "Manager");

        String stringPath = "Department.manager[CEO].company.departments.employees[Manager].seniority";

        try {
            new Path(model, stringPath, constraintMap);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected

        }
    }
    public void testNotValidConstraintMapColon() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department:manager", "CEO");

        String stringPath = "Department.manager.name";

        try {
            new Path(model, stringPath, constraintMap);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private void checkConstrainedPath(Path path) {
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.Department"), path.getStartClassDescriptor());
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.Department"), path.getElementClassDescriptors().get(0));
        assertEquals("manager", path.getElements().get(0));
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.CEO"), path.getElementClassDescriptors().get(1));
        assertEquals("company", path.getElements().get(1));
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.Company"), path.getElementClassDescriptors().get(2));
        assertEquals("departments", path.getElements().get(2));
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.Department"), path.getElementClassDescriptors().get(3));
        assertEquals("employees", path.getElements().get(3));
        assertEquals(model.getClassDescriptorByName("org.intermine.model.testmodel.Manager"), path.getElementClassDescriptors().get(4));
        assertEquals("seniority", path.getElements().get(4));
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

    public void testGetPrefix() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department.manager", "CEO");
        constraintMap.put("Department.manager.company.departments.employees", "Manager");

        String stringPath = "Department.manager.company.departments.employees.seniority";
        Path path = new Path(model, stringPath, constraintMap);

        Path prefix = path.getPrefix();
        assertEquals("Department.manager[CEO].company.departments.employees[Manager]",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department.manager[CEO].company.departments",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department.manager[CEO].company",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department.manager[CEO]",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department",
                     prefix.toString());
        try {
            prefix = prefix.getPrefix();
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }

    }

    public void testGetPrefixOuterJoin() {
        Map constraintMap = new HashMap();
        constraintMap.put("Department.manager", "CEO");
        constraintMap.put("Department.manager.company.departments.employees", "Manager");

        String stringPath = "Department:manager.company:departments.employees.seniority";
        Path path = new Path(model, stringPath, constraintMap);

        Path prefix = path.getPrefix();
        assertEquals("Department:manager[CEO].company:departments.employees[Manager]",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department:manager[CEO].company:departments",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department:manager[CEO].company",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department:manager[CEO]",
                     prefix.toString());
        prefix = prefix.getPrefix();
        assertEquals("Department",
                     prefix.toString());
        try {
            prefix = prefix.getPrefix();
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }

    }

    public void testContainsCollections() {
        String stringPath = "Department.company";
        Path path = new Path(model, stringPath);
        assertFalse(path.containsCollections());
        stringPath = "Department.employees.name";
        path = new Path(model, stringPath);
        assertTrue(path.containsCollections());
    }

    public void testContainsReferences() {
        String stringPath = "Department.company";
        Path path = new Path(model, stringPath);
        assertTrue(path.containsReferences());
        stringPath = "Department.employees.name";
        path = new Path(model, stringPath);
        assertFalse(path.containsReferences());
    }

    public void testIsOnlyAttribute() {
        String stringPath = "Department.company";
        Path path = new Path(model, stringPath);
        assertFalse(path.isOnlyAttribute());
        stringPath = "Department.employees.name";
        path = new Path(model, stringPath);
        assertFalse(path.isOnlyAttribute());
        stringPath = "Department.name";
        path = new Path(model, stringPath);
        assertTrue(path.isOnlyAttribute());
    }

    public void testAppend() {
        Path expDepNamePath = new Path(model, "Department.name");
        Path path = new Path(model, "Department");
        Path depNamePath = path.append("name");

        assertEquals(expDepNamePath, depNamePath);
    }

    public void testAppend2() {
        Path expDeptPath = new Path(model, "Department.manager[CEO].company.departments");
        Path expEmpPath = new Path(model, "Department.manager[CEO].company.departments.employees");

        String testPathStr = "Department.manager[CEO].company";
        Path path = new Path(model, testPathStr);

        Path depPath = path.append("departments");

        assertEquals(expDeptPath.toString(), depPath.toString());

        Path empPath = depPath.append("employees");

        assertEquals(expEmpPath.toString(), empPath.toString());
    }

}
