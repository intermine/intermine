package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

public class DatabaseSchemaTest extends TestCase
{
    private Model model;

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testInvalidTruncatedList() throws Exception {
        List truncated = new ArrayList();
        truncated.add(model.getClassDescriptorByName("org.intermine.model.testmodel.Employee"));
        truncated.add(model.getClassDescriptorByName("org.intermine.model.testmodel.Manager"));
        try {
            new DatabaseSchema(model, truncated);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidTruncatedList() throws Exception {
        List truncated = new ArrayList();
        ClassDescriptor ceo = model.getClassDescriptorByName("org.intermine.model.testmodel.CEO");
        ClassDescriptor manager = model.getClassDescriptorByName("org.intermine.model.testmodel.Manager");
        ClassDescriptor employee = model.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        ClassDescriptor company = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        truncated.add(manager);
        truncated.add(employee);
        DatabaseSchema schema = new DatabaseSchema(model, truncated);

        assertTrue(schema.isTruncated(manager));
        assertTrue(schema.isTruncated(employee));
        assertFalse(schema.isTruncated(ceo));
        assertFalse(schema.isTruncated(company));

        assertEquals(employee, schema.getTableMaster(employee));
        assertEquals(manager, schema.getTableMaster(manager));
        assertEquals(manager, schema.getTableMaster(ceo));
        assertEquals(company, schema.getTableMaster(company));

        Set employeeAttributes = new HashSet();
        Set employeeReferences = new HashSet();
        employeeAttributes.add(employee.getFieldDescriptorByName("id"));
        employeeAttributes.add(employee.getFieldDescriptorByName("fullTime"));
        employeeAttributes.add(employee.getFieldDescriptorByName("age"));
        employeeAttributes.add(employee.getFieldDescriptorByName("end"));
        employeeReferences.add(employee.getFieldDescriptorByName("department"));
        employeeReferences.add(employee.getFieldDescriptorByName("departmentThatRejectedMe"));
        employeeReferences.add(employee.getFieldDescriptorByName("address"));
        employeeAttributes.add(employee.getFieldDescriptorByName("name"));
        DatabaseSchema.Fields got = schema.getTableFields(employee);
        assertEquals(employeeAttributes, got.getAttributes());
        assertEquals(employeeReferences, got.getReferences());

        Set managerAttributes = new HashSet();
        Set managerReferences = new HashSet();
        managerAttributes.add(manager.getFieldDescriptorByName("id"));
        managerAttributes.add(manager.getFieldDescriptorByName("fullTime"));
        managerAttributes.add(manager.getFieldDescriptorByName("age"));
        managerAttributes.add(manager.getFieldDescriptorByName("end"));
        managerReferences.add(manager.getFieldDescriptorByName("department"));
        managerReferences.add(manager.getFieldDescriptorByName("departmentThatRejectedMe"));
        managerReferences.add(manager.getFieldDescriptorByName("address"));
        managerAttributes.add(manager.getFieldDescriptorByName("title"));
        managerAttributes.add(manager.getFieldDescriptorByName("name"));
        managerAttributes.add(manager.getFieldDescriptorByName("seniority"));
        managerAttributes.add(ceo.getFieldDescriptorByName("salary"));
        managerReferences.add(ceo.getFieldDescriptorByName("company"));
        got = schema.getTableFields(manager);
        assertEquals(managerAttributes, got.getAttributes());
        assertEquals(managerReferences, got.getReferences());

        Set companyAttributes = new HashSet();
        Set companyReferences = new HashSet();
        companyAttributes.add(company.getFieldDescriptorByName("id"));
        companyAttributes.add(company.getFieldDescriptorByName("name"));
        companyAttributes.add(company.getFieldDescriptorByName("vatNumber"));
        companyReferences.add(company.getFieldDescriptorByName("CEO"));
        companyReferences.add(company.getFieldDescriptorByName("address"));
        got = schema.getTableFields(company);
        assertEquals(companyAttributes, got.getAttributes());
        assertEquals(companyReferences, got.getReferences());
    }
}
