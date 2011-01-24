package org.intermine.webservice.client.services;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.util.TestUtil;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests functionality of QueryService - client class
 * 
 * @author Jakub Kulaviak
 **/
public class QueryServiceTest extends TestCase
{
    
    public void testCreatePathQuery() throws IOException {
        ServiceFactory factory = new ServiceFactory(TestUtil.getRootUrl(), "");
        ModelService modelService = factory.getModelService();
        Model model = modelService.getModel();
        PathQuery query = new PathQuery(model);
        query.addViews("Employee.name", "Employee.age", "Employee.end", "Employee.fullTime");
        query.addConstraint(Constraints.like("Employee.name","EmployeeA*"));
        query.addConstraint(Constraints.greaterThanEqualTo("Employee.age", "10"));
        query.addConstraint(Constraints.lessThan("Employee.age", "60"));
        query.addConstraint(Constraints.eq("Employee.fullTime", Boolean.TRUE.toString()));
        DummyQueryService queryService = TestUtil.getQueryService();
        queryService.setFakeResponse("EmployeeA1\t10\t1\ttrue\nEmployeeA2\t20\t2\ttrue");
        queryService.setExpectedRequest("http://localhost:8080/intermine-test/service/query/results?query=%3Cquery+name%3D%22query%22+model%3D%22testmodel%22+view%3D%22Employee.name+Employee.age+Employee.end+Employee.fullTime%22+constraintLogic%3D%22A+and+B+and+C+and+D%22%3E%3Cconstraint+path%3D%22Employee.name%22+code%3D%22A%22+op%3D%22LIKE%22+value%3D%22EmployeeA*%22%2F%3E%3Cconstraint+path%3D%22Employee.age%22+code%3D%22B%22+op%3D%22%26gt%3B%3D%22+value%3D%2210%22%2F%3E%3Cconstraint+path%3D%22Employee.age%22+code%3D%22C%22+op%3D%22%26lt%3B%22+value%3D%2260%22%2F%3E%3Cconstraint+path%3D%22Employee.fullTime%22+code%3D%22D%22+op%3D%22%3D%22+value%3D%22true%22%2F%3E%3C%2Fquery%3E&size=100");
        List<List<String>> result = queryService.getResult(query, 100);
        TestUtil.checkRow(result.get(0), "EmployeeA1", "10", "1", "true");
        TestUtil.checkRow(result.get(1), "EmployeeA2", "20", "2", "true");
    }

    public void testGetResultPathQuery() throws IOException {
        DummyQueryService queryService = TestUtil.getQueryService();
        queryService.setFakeResponse("EmployeeA1\tDepartmentA1\tCompanyA\ttrue\tEmployee Street, AVille\nEmployeeA2\tDepartmentA1\tCompanyA\ttrue\tEmployee Street, AVille\nEmployeeA3\tDepartmentA1\tCompanyA\tfalse\tEmployee Street, AVille");
        queryService.setExpectedRequest("http://localhost:8080/intermine-test/service/query/results?query=%3Cquery+name%3D%22query%22+model%3D%22testmodel%22+view%3D%22Employee.name+Employee.department.name+Employee.department.company.name+Employee.fullTime+Employee.address.address%22+sortOrder%3D%22Employee.name+asc%22%3E%3Cconstraint+path%3D%22Employee.address.address%22+op%3D%22LIKE%22+value%3D%22AVille%22%2F%3E%3C%2Fquery%3E&size=10");
        PathQuery query = queryService.createPathQuery(getSimpleXml());
        checkResult(queryService.getResult(query, 10));
    }

    private String getSimpleXml() {
        return "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.department.name " 
        		+ "Employee.department.company.name Employee.fullTime Employee.address.address\" sortOrder=\"Employee.name ASC\">" 
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
        DummyQueryService service = TestUtil.getQueryService();
        service.setFakeResponse("EmployeeA1\tDepartmentA1\tCompanyA\ttrue\tEmployee Street, AVille\nEmployeeA2\tDepartmentA1\tCompanyA\ttrue\tEmployee Street, AVille\nEmployeeA3\tDepartmentA1\tCompanyA\tfalse\tEmployee Street, AVille");
        service.setExpectedRequest("http://localhost:8080/intermine-test/service/query/results?query=%3Cquery+name%3D%22%22+model%3D%22testmodel%22+view%3D%22Employee.name+Employee.department.name+Employee.department.company.name+Employee.fullTime+Employee.address.address%22+sortOrder%3D%22Employee.name+ASC%22%3E%3Cnode+path%3D%22Employee%22+type%3D%22Employee%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Employee.address%22+type%3D%22Address%22%3E%3C%2Fnode%3E%3Cnode+path%3D%22Employee.address.address%22+type%3D%22String%22%3E%3Cconstraint+op%3D%22CONTAINS%22+value%3D%22AVille%22+description%3D%22%22+identifier%3D%22%22+code%3D%22A%22%3E%3C%2Fconstraint%3E%3C%2Fnode%3E%3C%2Fquery%3E&size=10");
        List<List<String>> result = service.getResult(getSimpleXml(), 10);
        checkResult(result);
    }

    private void checkResult(List<List<String>> result) {
        assertEquals(3, result.size());
        TestUtil.checkRow(result.get(0), "EmployeeA1", "DepartmentA1", "CompanyA", "true", "Employee Street, AVille");
        TestUtil.checkRow(result.get(1), "EmployeeA2", "DepartmentA1", "CompanyA", "true", "Employee Street, AVille");
        TestUtil.checkRow(result.get(2), "EmployeeA3", "DepartmentA1", "CompanyA", "false", "Employee Street, AVille");
    }
}
