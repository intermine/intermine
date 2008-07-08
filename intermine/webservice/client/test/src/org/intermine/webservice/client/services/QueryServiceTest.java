package org.intermine.webservice.client.services;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.util.TestUtil;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class QueryServiceTest extends TestCase
{

    public void testCreatePathQuery() {
        QueryService queryService = TestUtil.getQueryService();
        PathQuery query = queryService.createPathQuery(getSimpleXml());
        assertNotNull(query);
        query.getViewStrings();
    }
    
//    public void testExecuteClientPathQuery() throws IOException {
//        ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl(), "");
//        ModelService modelService = factory.getModelService();
//        Model model = modelService.getModel();
//        PathQuery query = new PathQuery(model);
//        query.addView("Employee.name");
//        query.addConstraint("Employee.name", new Constraint(ConstraintOp.CONTAINS, "Employee"));
//        query.addConstraint("Employee.age", new Constraint(ConstraintOp.LESS_THAN, new Integer(10)));
//        query.addConstraint("Employee.age", new Constraint(ConstraintOp.GREATER_THAN, new Integer(30)));
//        query.addConstraint("Employee.fullTime", new Constraint(ConstraintOp.EQUALS, true));
//        System.out.println("query: ");
//        System.out.println(query.toXml());
//        QueryService queryService = factory.getQueryService();
//        List<List<String>> results = queryService.getResult(query, 1, 100).getData();
//        printResults(results);
//    }

//    private void printResults(List<List<String>> results) {
//        for (List<String> row : results) {
//            for (String cell : row) {
//                System.out.print(cell);
//                System.out.print(" ");
//            }
//            System.out.println();
//        }
//    }

    public void testGetCount() {
        QueryService service = TestUtil.getQueryService();
        assertEquals(3, service.getCount(getSimpleXml()));
    }

    public void testGetResultPathQuery() throws IOException {
        QueryService queryService = TestUtil.getQueryService();
        PathQuery query = queryService.createPathQuery(getSimpleXml());
        checkResult(queryService.getResult(query, 1, 10).getData());
    }

    private String getSimpleXml() {
        return "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.department.name " 
        		+ "Employee.department.company.name Employee.fullTime Employee.address.address\" sortOrder=\"Employee.name\">" 
        		+ "<node path=\"Employee\" type=\"Employee\">"
        		+ "</node>"
                + "<node path=\"Employee.address\" type=\"Address\">"
                + "</node>"
                + "<node path=\"Employee.address.address\" type=\"String\">"
                + "<constraint op=\"CONTAINS\" value=\"AVille\" description=\"\" identifier=\"\" code=\"A\">"
                + "</constraint>"
                + "</node>"
                + "</query>";
    }
    
    public void testGetResultStringXmlQuery() throws IOException {
        QueryService service = TestUtil.getQueryService();
        List<List<String>> result = service.getResult(getSimpleXml(), 1, 10).getData();
        checkResult(result);
    }

    private void checkResult(List<List<String>> result) {
        assertEquals(3, result.size());
        TestUtil.checkRow(result.get(0), "EmployeeA1", "DepartmentA1", "CompanyA", "true", "Employee Street, AVille");
        TestUtil.checkRow(result.get(1), "EmployeeA2", "DepartmentA1", "CompanyA", "true", "Employee Street, AVille");
        TestUtil.checkRow(result.get(2), "EmployeeA3", "DepartmentA1", "CompanyA", "false", "Employee Street, AVille");
    }
}
