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
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.ConstraintOp;

import junit.framework.TestCase;

public class QueryBuildHelperTest extends TestCase
{
    public QueryBuildHelperTest (String testName) {
        super(testName);
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
    }

    public void testToStrings() throws Exception {
    }

    public void testGetAllFieldNames() throws Exception {
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
    }

    public void testToDisplayable() throws Exception {
    }
}
