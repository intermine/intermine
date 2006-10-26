package org.intermine.path;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
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
}

