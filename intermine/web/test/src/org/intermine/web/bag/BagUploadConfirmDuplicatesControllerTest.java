package org.intermine.web.bag;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;

import junit.framework.TestCase;

/**
 * Tests for BagUploadConfirmDuplicatesController
 * @author Kim Rutherford
 */
public class BagUploadConfirmDuplicatesControllerTest extends TestCase
{
    private Model model;
    
    protected void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }
    
    public void testMakeClassDuplicatesMap() {
        Map duplicates = new HashMap();
        List testObjList1= new ArrayList();
        Employee emp1 = new Employee();
        emp1.setName("emp1");
        testObjList1.add(emp1);
        Employee emp2 = new Employee();
        emp2.setName("emp1");
        testObjList1.add(emp2);
        duplicates.put("emp1", testObjList1);
        List testObjList2 = new ArrayList();
        Employee emp3 = new Employee();
        emp1.setName("foo");
        testObjList2.add(emp3);
        Department dep1 = new Department();
        dep1.setName("foo");
        testObjList2.add(dep1);
        Department dep2 = new Department();
        dep2.setName("foo");
        testObjList2.add(dep2);
        duplicates.put("foo", testObjList2);
        
        Map res = BagUploadConfirmDuplicatesController.makeClassDuplicatesMap(duplicates);
        assertEquals(2, res.size());
        Map empRes = (Map) res.get("Employee");
        assertEquals(2, empRes.size());
        List emp1Objs = (List) empRes.get("emp1");
        assertEquals(2, emp1Objs.size());
        List empFooObjs = (List) empRes.get("foo");
        assertEquals(1, empFooObjs.size());
        Map depRes = (Map) res.get("Department");
        List depFooObjs = (List) depRes.get("foo");
        assertEquals(2, depFooObjs.size());
    }
}
