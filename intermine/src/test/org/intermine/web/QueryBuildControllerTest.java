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

import servletunit.struts.MockStrutsTestCase;
//import org.apache.struts.tiles.ComponentContext;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
//import javax.servlet.http.HttpSession;

import org.flymine.objectstore.query.*;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Employee;
import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Manager;
// import org.flymine.model.testmodel.Department;

public class QueryBuildControllerTest extends MockStrutsTestCase
{
    public QueryBuildControllerTest(String arg1) {
        super(arg1);
    }

    public void testMapOps() throws Exception {
        List ops = Arrays.asList(new Object[] { ConstraintOp.EQUALS, ConstraintOp.CONTAINS });
        Map result = QueryBuildController.mapOps(ops);
    
        for (Iterator i = ops.iterator(); i.hasNext();) {
            ConstraintOp op = (ConstraintOp) i.next();
            assertEquals(op.toString(), result.get(op.getIndex()));
        }
    }

    public void testGetOpsNoBagsPresent() throws Exception {
        ClassDescriptor cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Department");      
        Map result = QueryBuildController.getValidOps(cld, false);
        
        assertEquals(QueryBuildController.mapOps(SimpleConstraint.validOps(String.class)), result.get("name"));
        assertEquals(QueryBuildController.mapOps(ContainsConstraint.VALID_OPS), result.get("employees"));
        assertEquals(QueryBuildController.mapOps(ContainsConstraint.VALID_OPS), result.get("manager"));
        assertEquals(QueryBuildController.mapOps(ContainsConstraint.VALID_OPS), result.get("rejectedEmployee"));
        assertEquals(QueryBuildController.mapOps(ContainsConstraint.VALID_OPS), result.get("company"));
    }
    
    public void testGetOpsBagsPresent() throws Exception {
        ClassDescriptor cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Department");      
        QueryBuildController qbc = new QueryBuildController();
        Map result = qbc.getValidOps(cld, true);
        
        List nameValidOps = new ArrayList(SimpleConstraint.validOps(String.class));
        nameValidOps.addAll(BagConstraint.VALID_OPS);
        assertEquals(qbc.mapOps(nameValidOps), result.get("name"));
        assertEquals(qbc.mapOps(ContainsConstraint.VALID_OPS), result.get("employees"));
        assertEquals(qbc.mapOps(ContainsConstraint.VALID_OPS), result.get("manager"));
        assertEquals(qbc.mapOps(ContainsConstraint.VALID_OPS), result.get("rejectedEmployee"));
        assertEquals(qbc.mapOps(ContainsConstraint.VALID_OPS), result.get("company"));
    }

//     public void testGetAliases() throws Exception {
//         Query q = new Query();
//         QueryClass qc1 = new QueryClass(Company.class);
//         QueryClass qc2 = new QueryClass(Company.class);
//         QueryClass qc3 = new QueryClass(Employee.class);
//         q.addFrom(qc1);
//         q.addFrom(qc2);
//         q.addFrom(qc3);
        
//         ClassDescriptor cld = Model.getInstanceByName("testmodel").getClassDescriptorByName("org.flymine.model.testmodel.Department");
//         Map aliases = q.getAliases();
//         Map result = new QueryBuildController().getAliases(cld, q);
//         assertEquals(Arrays.asList(new Object[] {aliases.get(qc1), aliases.get(qc2)}), result.get("company"));
//         assertEquals(Arrays.asList(new Object[] {aliases.get(qc3)}), result.get("employees"));
//     }

//     public void testPopulateForm() throws Exception {
//         QueryBuildForm form = new QueryBuildForm();
//         Query q = new Query();
//         QueryClass qc1 = new QueryClass(Department.class);
//         QueryClass qc2 = new QueryClass(Manager.class);
//         QueryClass qc3 = new QueryClass(Employee.class);
//         SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.EQUALS, new QueryValue("Frank"));
//         SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc1, "name"), ConstraintOp.MATCHES, new QueryValue("Barry"));
//         ContainsConstraint cc1 = new ContainsConstraint(new QueryObjectReference(qc1, "manager"), ConstraintOp.CONTAINS, qc2);
//         ContainsConstraint cc2 = new ContainsConstraint(new QueryCollectionReference(qc1, "employees"), ConstraintOp.CONTAINS, qc3);
//         q.addFrom(qc1);
//         q.addFrom(qc2);
//         q.addFrom(qc3);
//         ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
//         cs.addConstraint(sc1);
//         cs.addConstraint(sc2);
//         cs.addConstraint(cc1);
//         cs.addConstraint(cc2);
//         q.setConstraint(cs);
        
//         QueryBuildController.populateForm(form, ConstraintHelper.createList(q, qc1), q.getAliases(), null);
//         assertEquals(ConstraintOp.EQUALS.getIndex(), form.getFieldOp("name_0"));
//         assertEquals("Frank", form.getFieldValue("name_0"));
//         assertEquals(ConstraintOp.MATCHES.getIndex(), form.getFieldOp("name_1"));
//         assertEquals("Barry", form.getFieldValue("name_1"));
//         assertEquals(ConstraintOp.CONTAINS.getIndex(), form.getFieldOp("manager_0"));
//         assertEquals(q.getAliases().get(qc2), form.getFieldValue("manager_0"));
//         assertEquals(ConstraintOp.CONTAINS.getIndex(), form.getFieldOp("employees_0"));
//         assertEquals(q.getAliases().get(qc3), form.getFieldValue("employees_0"));
//    }

//     public void testExecute() throws Exception {
//         setRequestPathInfo("/initQueryBuild");
//         QueryClass qc = new QueryClass(Department.class);
//         getSession().setAttribute("queryClass", qc);
//         Model model = Model.getInstanceByName("testmodel");
//         getSession().setAttribute("model", new DisplayModel(model));
//         Query q = new Query();
//         SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.EQUALS, new QueryValue("Kenneth"));
//         q.setConstraint(sc);
//         q.addFrom(qc);
//         getSession().setAttribute("query", q);

//         actionPerform();
//         verifyNoActionErrors();

//         Map expected1 = new HashMap();
//         expected1.put("name_0", "name");
//         Map expected2 = new HashMap();
//         expected2.put("employees", new ArrayList());
//         expected2.put("rejectedEmployee", new ArrayList());
//         expected2.put("manager", new ArrayList());
//         expected2.put("company", new ArrayList());
//         Map expected3 = new HashMap();
//         expected3.put("name", QueryBuildController.mapOps(SimpleConstraint.validOps(String.class)));
//         expected3.put("id", QueryBuildController.mapOps(SimpleConstraint.validOps(Integer.class)));
//         expected3.put("employees", QueryBuildController.mapOps(ContainsConstraint.VALID_OPS));
//         expected3.put("rejectedEmployee", QueryBuildController.mapOps(ContainsConstraint.VALID_OPS));
//         expected3.put("manager", QueryBuildController.mapOps(ContainsConstraint.VALID_OPS));
//         expected3.put("company", QueryBuildController.mapOps(ContainsConstraint.VALID_OPS));

//         assertEquals(q.getAliases().get(qc), getRequest().getAttribute("aliasStr"));
//         assertEquals(expected1, getSession().getAttribute("constraints"));
//         assertEquals(expected2, getRequest().getAttribute("aliases"));
//         assertEquals(expected3, getRequest().getAttribute("ops"));
//         assertEquals(model.getClassDescriptorByName("org.flymine.model.testmodel.Department"), getRequest().getAttribute("cld"));
//     }

    public void testPopulateQueryBuildForm() throws Exception {
        assertTrue(true);
//         QueryBuildForm form = new QueryBuildForm();
//         QueryClass qc1 = new QueryClass(Company.class);
//         QueryClass qc2 = new QueryClass(Department.class);

//         SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
//                                                   ConstraintOp.NOT_EQUALS,
//                                                   new QueryValue("name1"));
//         SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc1, "name"),
//                                                   ConstraintOp.EQUALS,
//                                                   new QueryValue("name2"));
//         ContainsConstraint cc1 = new ContainsConstraint(new QueryCollectionReference(qc1, "departments"),
//                                                         ConstraintOp.DOES_NOT_CONTAIN,
//                                                         qc2);

//         List constraints = Arrays.asList(new Object[] {sc1, sc2, cc1});
//         ClassDescriptor cld = Model.getInstanceByName("testmodel")
//             .getClassDescriptorByName("org.flymine.model.testmodel.Company");
//         HashMap aliasMap = new HashMap();
//         aliasMap.put(qc2, "qc2");

//         QueryBuildController qbc = new QueryBuildController();
//         Map constraints = qbc.buildConstraintMap(form, constraints, aliasMap, new HashMap());
//         assertEquals("name1", (String) form.getFieldValues().get("name#0"));
//         assertEquals(3, form.getFieldValues().size());
//         assertEquals(3, form.getFieldOps().size());
//         assertEquals(sc1.getOp().getIndex(), (Integer) form.getFieldOps().get("name#0"));
//         assertEquals("name2", (String) form.getFieldValues().get("name#1"));
//         assertEquals(sc2.getOp().getIndex(), (Integer) form.getFieldOps().get("name#1"));
//         assertEquals("qc2", (String) form.getFieldValues().get("departments#0"));
//         assertEquals(cc1.getOp().getIndex(), (Integer) form.getFieldOps().get("departments#0"));
    }
}
