package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.ArrayList;
import java.util.Collections;
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
            new DatabaseSchema(model, truncated, false, Collections.EMPTY_SET);
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
        ClassDescriptor importantPerson = model.getClassDescriptorByName("org.intermine.model.testmodel.ImportantPerson");
        truncated.add(manager);
        truncated.add(employee);
        DatabaseSchema schema = new DatabaseSchema(model, truncated, false, Collections.EMPTY_SET);

        assertTrue(schema.isTruncated(manager));
        assertTrue(schema.isTruncated(employee));
        assertFalse(schema.isTruncated(ceo));
        assertFalse(schema.isTruncated(company));
        assertFalse(schema.isTruncated(importantPerson));

        assertEquals(employee, schema.getTableMaster(employee));
        assertEquals(manager, schema.getTableMaster(manager));
        assertEquals(manager, schema.getTableMaster(ceo));
        assertEquals(company, schema.getTableMaster(company));
        assertEquals(importantPerson, schema.getTableMaster(importantPerson));

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

        Set importantPersonAttributes = new HashSet();
        Set importantPersonReferences = new HashSet();
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("id"));
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("seniority"));
        got = schema.getTableFields(importantPerson);
        assertEquals(importantPersonAttributes, got.getAttributes());
        assertEquals(importantPersonReferences, got.getReferences());
 
        truncated = new ArrayList();
        truncated.add(employee);
        truncated.add(importantPerson);
        schema = new DatabaseSchema(model, truncated, false, Collections.EMPTY_SET);

        assertTrue(schema.isTruncated(employee));
        assertTrue(schema.isTruncated(importantPerson));
        assertFalse(schema.isTruncated(manager));
        assertFalse(schema.isTruncated(ceo));
        assertFalse(schema.isTruncated(company));

        assertEquals(employee, schema.getTableMaster(employee));
        assertEquals(employee, schema.getTableMaster(manager));
        assertEquals(employee, schema.getTableMaster(ceo));
        assertEquals(company, schema.getTableMaster(company));
        assertEquals(importantPerson, schema.getTableMaster(importantPerson));

        employeeAttributes = new HashSet();
        employeeReferences = new HashSet();
        employeeAttributes.add(employee.getFieldDescriptorByName("id"));
        employeeAttributes.add(employee.getFieldDescriptorByName("fullTime"));
        employeeAttributes.add(employee.getFieldDescriptorByName("age"));
        employeeAttributes.add(employee.getFieldDescriptorByName("end"));
        employeeReferences.add(employee.getFieldDescriptorByName("department"));
        employeeReferences.add(employee.getFieldDescriptorByName("departmentThatRejectedMe"));
        employeeReferences.add(employee.getFieldDescriptorByName("address"));
        employeeAttributes.add(employee.getFieldDescriptorByName("name"));
        employeeAttributes.add(manager.getFieldDescriptorByName("title"));
        employeeAttributes.add(importantPerson.getFieldDescriptorByName("seniority"));
        employeeAttributes.add(ceo.getFieldDescriptorByName("salary"));
        employeeReferences.add(ceo.getFieldDescriptorByName("company"));
        got = schema.getTableFields(employee);
        assertEquals(employeeAttributes, got.getAttributes());
        assertEquals(employeeReferences, got.getReferences());

        companyAttributes = new HashSet();
        companyReferences = new HashSet();
        companyAttributes.add(company.getFieldDescriptorByName("id"));
        companyAttributes.add(company.getFieldDescriptorByName("name"));
        companyAttributes.add(company.getFieldDescriptorByName("vatNumber"));
        companyReferences.add(company.getFieldDescriptorByName("CEO"));
        companyReferences.add(company.getFieldDescriptorByName("address"));
        got = schema.getTableFields(company);
        assertEquals(companyAttributes, got.getAttributes());
        assertEquals(companyReferences, got.getReferences());

        importantPersonAttributes = new HashSet();
        importantPersonReferences = new HashSet();
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("id"));
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("seniority"));
        //importantPersonAttributes.add(manager.getFieldDescriptorByName("fullTime"));
        //importantPersonAttributes.add(manager.getFieldDescriptorByName("age"));
        //importantPersonAttributes.add(manager.getFieldDescriptorByName("end"));
        //importantPersonReferences.add(manager.getFieldDescriptorByName("department"));
        //importantPersonReferences.add(manager.getFieldDescriptorByName("departmentThatRejectedMe"));
        //importantPersonReferences.add(manager.getFieldDescriptorByName("address"));
        //importantPersonAttributes.add(manager.getFieldDescriptorByName("title"));
        //importantPersonAttributes.add(manager.getFieldDescriptorByName("name"));
        //importantPersonAttributes.add(ceo.getFieldDescriptorByName("salary"));
        //importantPersonReferences.add(ceo.getFieldDescriptorByName("company"));
        ClassDescriptor employable = model.getClassDescriptorByName("org.intermine.model.testmodel.Employable");
        ClassDescriptor contractor = model.getClassDescriptorByName("org.intermine.model.testmodel.Contractor");
        importantPersonAttributes.add(employable.getFieldDescriptorByName("name"));
        importantPersonReferences.add(contractor.getFieldDescriptorByName("personalAddress"));
        importantPersonReferences.add(contractor.getFieldDescriptorByName("businessAddress"));
        got = schema.getTableFields(importantPerson);
        assertEquals(importantPersonAttributes, got.getAttributes());
        assertEquals(importantPersonReferences, got.getReferences());
    }

    public void testValidTruncatedListFlatMode() throws Exception {
        List truncated = new ArrayList();
        ClassDescriptor ceo = model.getClassDescriptorByName("org.intermine.model.testmodel.CEO");
        ClassDescriptor manager = model.getClassDescriptorByName("org.intermine.model.testmodel.Manager");
        ClassDescriptor employee = model.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        ClassDescriptor company = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        ClassDescriptor importantPerson = model.getClassDescriptorByName("org.intermine.model.testmodel.ImportantPerson");
        truncated.add(manager);
        truncated.add(employee);
        DatabaseSchema schema = new DatabaseSchema(model, truncated, true, Collections.singleton("intermineobject"));

        assertTrue(schema.isTruncated(manager));
        assertTrue(schema.isTruncated(employee));
        assertFalse(schema.isTruncated(ceo));
        assertFalse(schema.isTruncated(company));
        assertFalse(schema.isTruncated(importantPerson));

        assertEquals(employee, schema.getTableMaster(employee));
        assertEquals(manager, schema.getTableMaster(manager));
        assertEquals(manager, schema.getTableMaster(ceo));
        assertEquals(company, schema.getTableMaster(company));
        assertEquals(importantPerson, schema.getTableMaster(importantPerson));

        Set employeeAttributes = new HashSet();
        Set employeeReferences = new HashSet();
        employeeAttributes.add(employee.getFieldDescriptorByName("id"));
        employeeAttributes.add(employee.getFieldDescriptorByName("fullTime"));
        employeeAttributes.add(employee.getFieldDescriptorByName("age"));
        employeeAttributes.add(employee.getFieldDescriptorByName("end"));
        employeeReferences.add(employee.getFieldDescriptorByName("department"));
        employeeReferences.add(employee.getFieldDescriptorByName("departmentThatRejectedMe"));
        employeeReferences.add(employee.getFieldDescriptorByName("address"));
        employeeAttributes.add(manager.getFieldDescriptorByName("title"));
        employeeAttributes.add(employee.getFieldDescriptorByName("name"));
        employeeAttributes.add(manager.getFieldDescriptorByName("seniority"));
        employeeAttributes.add(ceo.getFieldDescriptorByName("salary"));
        employeeReferences.add(ceo.getFieldDescriptorByName("company"));
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

        Set importantPersonAttributes = new HashSet();
        Set importantPersonReferences = new HashSet();
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("id"));
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("seniority"));
        got = schema.getTableFields(importantPerson);
        assertEquals(importantPersonAttributes, got.getAttributes());
        assertEquals(importantPersonReferences, got.getReferences());
 
        truncated = new ArrayList();
        truncated.add(employee);
        truncated.add(importantPerson);
        schema = new DatabaseSchema(model, truncated, true, Collections.singleton("intermineobject"));

        assertTrue(schema.isTruncated(employee));
        assertTrue(schema.isTruncated(importantPerson));
        assertFalse(schema.isTruncated(manager));
        assertFalse(schema.isTruncated(ceo));
        assertFalse(schema.isTruncated(company));

        assertEquals(employee, schema.getTableMaster(employee));
        assertEquals(employee, schema.getTableMaster(manager));
        assertEquals(employee, schema.getTableMaster(ceo));
        assertEquals(company, schema.getTableMaster(company));
        assertEquals(importantPerson, schema.getTableMaster(importantPerson));

        employeeAttributes = new HashSet();
        employeeReferences = new HashSet();
        employeeAttributes.add(employee.getFieldDescriptorByName("id"));
        employeeAttributes.add(employee.getFieldDescriptorByName("fullTime"));
        employeeAttributes.add(employee.getFieldDescriptorByName("age"));
        employeeAttributes.add(employee.getFieldDescriptorByName("end"));
        employeeReferences.add(employee.getFieldDescriptorByName("department"));
        employeeReferences.add(employee.getFieldDescriptorByName("departmentThatRejectedMe"));
        employeeReferences.add(employee.getFieldDescriptorByName("address"));
        employeeAttributes.add(employee.getFieldDescriptorByName("name"));
        employeeAttributes.add(manager.getFieldDescriptorByName("title"));
        employeeAttributes.add(importantPerson.getFieldDescriptorByName("seniority"));
        employeeAttributes.add(ceo.getFieldDescriptorByName("salary"));
        employeeReferences.add(ceo.getFieldDescriptorByName("company"));
        got = schema.getTableFields(employee);
        assertEquals(employeeAttributes, got.getAttributes());
        assertEquals(employeeReferences, got.getReferences());

        companyAttributes = new HashSet();
        companyReferences = new HashSet();
        companyAttributes.add(company.getFieldDescriptorByName("id"));
        companyAttributes.add(company.getFieldDescriptorByName("name"));
        companyAttributes.add(company.getFieldDescriptorByName("vatNumber"));
        companyReferences.add(company.getFieldDescriptorByName("CEO"));
        companyReferences.add(company.getFieldDescriptorByName("address"));
        got = schema.getTableFields(company);
        assertEquals(companyAttributes, got.getAttributes());
        assertEquals(companyReferences, got.getReferences());

        importantPersonAttributes = new HashSet();
        importantPersonReferences = new HashSet();
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("id"));
        importantPersonAttributes.add(importantPerson.getFieldDescriptorByName("seniority"));
        importantPersonAttributes.add(manager.getFieldDescriptorByName("fullTime"));
        importantPersonAttributes.add(manager.getFieldDescriptorByName("age"));
        importantPersonAttributes.add(manager.getFieldDescriptorByName("end"));
        importantPersonReferences.add(manager.getFieldDescriptorByName("department"));
        importantPersonReferences.add(manager.getFieldDescriptorByName("departmentThatRejectedMe"));
        importantPersonReferences.add(manager.getFieldDescriptorByName("address"));
        importantPersonAttributes.add(manager.getFieldDescriptorByName("title"));
        importantPersonAttributes.add(manager.getFieldDescriptorByName("name"));
        importantPersonAttributes.add(ceo.getFieldDescriptorByName("salary"));
        importantPersonReferences.add(ceo.getFieldDescriptorByName("company"));
        ClassDescriptor employable = model.getClassDescriptorByName("org.intermine.model.testmodel.Employable");
        ClassDescriptor contractor = model.getClassDescriptorByName("org.intermine.model.testmodel.Contractor");
        importantPersonAttributes.add(employable.getFieldDescriptorByName("name"));
        importantPersonReferences.add(contractor.getFieldDescriptorByName("personalAddress"));
        importantPersonReferences.add(contractor.getFieldDescriptorByName("businessAddress"));
        got = schema.getTableFields(importantPerson);
        assertEquals(importantPersonAttributes, got.getAttributes());
        assertEquals(importantPersonReferences, got.getReferences());
    }
}
