package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.Model;
import org.flymine.objectstore.query.*;
import org.flymine.testing.OneTimeTestCase;

import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Employee;

import junit.framework.TestCase;
import junit.framework.Test;

public class QueryBuildHelperTest extends QueryTestCase
{
    Model model;

    public QueryBuildHelperTest (String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return OneTimeTestCase.buildSuite(QueryBuildHelperTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }
    
    public void testAliasClass() throws Exception {
        Collection existingAliases = new HashSet();
        existingAliases.add("Type_0");
        existingAliases.add("Type_1");
        existingAliases.add("Type_2");
        existingAliases.add("OtherType_0");
        existingAliases.add("OtherType_1");

        String newAlias = QueryBuildHelper.aliasClass(existingAliases, "OtherType");

        assertEquals("OtherType_2", newAlias);
    }

    public void testAddClass() throws Exception {
        Map queryClasses = new HashMap();

        QueryBuildHelper.addClass(queryClasses, "org.flymine.model.testmodel.Employee");
        assertEquals(1, queryClasses.size());

        QueryBuildHelper.addClass(queryClasses, "org.flymine.model.testmodel.Employee");
        assertEquals(2, queryClasses.size());

        Set expected = new HashSet();
        expected.add("Employee_0");
        expected.add("Employee_1");
        
        assertEquals(expected, queryClasses.keySet());
    }

    public void testCreateQuery() throws Exception{
        Class cls = Department.class;
        
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(cls.getName());
        d.getConstraintNames().add("name_0");
        d.getFieldNames().put("name_0", "name");
        d.getFieldOps().put("name_0", ConstraintOp.NOT_EQUALS);
        d.getFieldValues().put("name_0", "Frank");
        
        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.alias(qc, "Department_0");
        q.addFrom(qc);
        q.addToSelect(qc);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.NOT_EQUALS, new QueryValue("Frank"));
        q.setConstraint(sc);

        Map queryClasses = new HashMap();
        queryClasses.put("Department_0", d);
        
        assertEquals(q, QueryBuildHelper.createQuery(queryClasses, model, new HashMap()));
    }

    public void testCreateQueryWithBag() throws Exception{
        Class cls = Employee.class;
        
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(cls.getName());
        d.getConstraintNames().add("age_0");
        d.getFieldNames().put("age_0", "age");
        d.getFieldOps().put("age_0", ConstraintOp.IN);

        Map savedBags = new HashMap();
        List myBag = new ArrayList();
        myBag.add(new Integer(20));
        myBag.add(new Integer(30));

        savedBags.put("my_saved_bag", myBag);
        
        d.getFieldValues().put("age_0", "my_saved_bag");
        
        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.alias(qc, "Employee_0");
        q.addFrom(qc);
        q.addToSelect(qc);
        BagConstraint sc =
            new BagConstraint(new QueryField(qc, "age"), ConstraintOp.IN, myBag);
        q.setConstraint(sc);

        Map queryClasses = new HashMap();
        queryClasses.put("Employee_0", d);
        
        assertEquals(q, QueryBuildHelper.createQuery(queryClasses, model, savedBags));
    }

    public void testGetAllFieldNames() throws Exception {
        ClassDescriptor cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Department");
        
        List expected = Arrays.asList(new Object[] {
            "rejectedEmployee", "employees", "manager", "name", "company"
        });
        
        assertEquals(expected, QueryBuildHelper.getAllFieldNames(cld));
    }

    public void testGetValidOpsNoBagsPresent() throws Exception {
        ClassDescriptor cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Department");
        Map result = QueryBuildHelper.getValidOps(cld, false);
        
        assertEquals(QueryBuildHelper.mapOps(SimpleConstraint.validOps(String.class)), result.get("name"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("employees"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("manager"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("rejectedEmployee"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("company"));
    }

    public void testGetValidOpsBagsPresent() throws Exception {
        ClassDescriptor cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Department");      
        Map result = QueryBuildHelper.getValidOps(cld, true);
        
        List nameValidOps = new ArrayList(SimpleConstraint.validOps(String.class));
        nameValidOps.addAll(BagConstraint.VALID_OPS);
        assertEquals(QueryBuildHelper.mapOps(nameValidOps), result.get("name"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("employees"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("manager"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("rejectedEmployee"));
        assertEquals(QueryBuildHelper.mapOps(ContainsConstraint.VALID_OPS), result.get("company"));
    }

    public void testMapOps() throws Exception {
        List ops = Arrays.asList(new Object[] { ConstraintOp.EQUALS, ConstraintOp.CONTAINS });

        Map expected = new HashMap();
        expected.put(ConstraintOp.EQUALS.getIndex(), ConstraintOp.EQUALS.toString());
        expected.put(ConstraintOp.CONTAINS.getIndex(), ConstraintOp.CONTAINS.toString());

        assertEquals(expected, QueryBuildHelper.mapOps(ops));
    }

    public void testGetQueryClasses() throws Exception {
        Class cls = Department.class;
        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.alias(qc, "Department_0");
        q.addFrom(qc);
        q.addToSelect(qc);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.NOT_EQUALS, new QueryValue("Frank"));
        q.setConstraint(sc);

        
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(cls.getName());
        d.getConstraintNames().add("name_0");
        d.getFieldNames().put("name_0", "name");
        d.getFieldOps().put("name_0", ConstraintOp.NOT_EQUALS);
        d.getFieldValues().put("name_0", "Frank");

        Map queryClasses = new HashMap();
        queryClasses.put("Department_0", d);

        assertEquals(queryClasses, QueryBuildHelper.getQueryClasses(q, new HashMap()));
    }

    public void testToDisplayable() throws Exception {
        Class cls = Department.class;
        
        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.alias(qc, "Department_0");
        q.addFrom(qc);
        q.addToSelect(qc);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.NOT_EQUALS, new QueryValue("Frank"));
        q.setConstraint(sc);

        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(cls.getName());
        d.getConstraintNames().add("name_0");
        d.getFieldNames().put("name_0", "name");
        d.getFieldOps().put("name_0", ConstraintOp.NOT_EQUALS);
        d.getFieldValues().put("name_0", "Frank");

        assertEquals(d, QueryBuildHelper.toDisplayable(qc, q, new HashMap()));
    }
}
