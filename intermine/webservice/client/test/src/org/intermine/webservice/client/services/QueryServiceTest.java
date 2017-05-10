package org.intermine.webservice.client.services;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.util.TestUtil;

/*
 * Copyright (C) 2002-2016 FlyMine
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
    private static final String baseUrl = "http://localhost:8080/intermine-test/service";
    private static final String resourcePath = "/query/results";

    public void testCreatePathQuery() throws IOException {
        ModelServiceTest mst = new ModelServiceTest();
        Model model = mst.getModelService().getModel();
        PathQuery query = new PathQuery(model);
        query.addViews("Employee.name", "Employee.age", "Employee.end", "Employee.fullTime");
        query.addConstraint(Constraints.like("Employee.name","EmployeeA*"));
        query.addConstraint(Constraints.greaterThanEqualTo("Employee.age", "10"));
        query.addConstraint(Constraints.lessThan("Employee.age", "60"));
        query.addConstraint(Constraints.eq("Employee.fullTime", Boolean.TRUE.toString()));
        DummyQueryService queryService = TestUtil.getQueryService();

        queryService.setFakeResponse("<ResultSet><Result><i>EmployeeA1</i><i>10</i><i>1.1</i><i>true</i></Result><Result><i>EmployeeA2</i><i>20</i><i>2.2</i><i>false</i></Result></ResultSet>");
        queryService.setExpectedRequest(baseUrl + resourcePath + "?" + "start=0" + "&" + "query=" + query.toXml() + "&" + "format=xml");

        List<List<String>> result = queryService.getAllResults(query);
        TestUtil.checkRow(result.get(0), "EmployeeA1", "10", "1.1", "true");
        TestUtil.checkRow(result.get(1), "EmployeeA2", "20", "2.2", "false");

        queryService.setExpectedRequest(baseUrl + resourcePath + "?"
                + "start=100&size=200"
                + "&query=" + query.toXml()
                + "&format=xml");
        result = queryService.getResults(query, new Page(100, 200));
    }

    public void testGetResultPathQuery() throws IOException {
        DummyQueryService queryService = TestUtil.getQueryService();
        PathQuery query = queryService.createPathQuery(getSimpleXml());

        queryService.setFakeResponse("<ResultSet><Result><i>EmployeeA1</i><i>10</i><i>1.1</i><i>true</i></Result><Result><i>EmployeeA2</i><i>20</i><i>2.2</i><i>false</i></Result></ResultSet>");
        queryService.setExpectedRequest(baseUrl + resourcePath + "?" + "start=0" + "&" + "query=" + query.toXml() + "&" + "format=xml");

        queryService.getAllResults(query);
    }

    private String getSimpleXml() {
        return "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.department.name "
                + "Employee.department.company.name Employee.fullTime Employee.address.address\" sortOrder=\"Employee.name ASC\">"
                + "<constraint path=\"Employee.address.address\" op=\"CONTAINS\" value=\"AVille\" code=\"A\"/>"
                + "</query>";
    }

    public void testGetResultStringXmlQuery() throws IOException {
        DummyQueryService service = TestUtil.getQueryService();
        service.setFakeResponse("<ResultSet><Result><i>EmployeeA1</i><i>10</i><i>1.1</i><i>true</i></Result><Result><i>EmployeeA2</i><i>20</i><i>2.2</i><i>false</i></Result></ResultSet>");
        service.setExpectedRequest(baseUrl + resourcePath + "?" + "start=0" + "&" + "query=" + getSimpleXml() + "&" + "format=xml");
        List<List<String>> result = service.getAllResults(getSimpleXml());
        TestUtil.checkRow(result.get(0), "EmployeeA1", "10", "1.1", "true");
    }

    public void testParseProblemValues() throws IOException {
        DummyQueryService service = TestUtil.getQueryService();
        service.setFakeResponse("<ResultSet><Result><i>&lt;</i><i>&quot;</i><i>&gt;</i><i>&amp;</i><i>,</i><i>&apos;</i><i>\t</i><i>\n</i></Result></ResultSet>");
        service.setExpectedRequest(baseUrl + resourcePath + "?" + "start=0" + "&" + "query=" + getSimpleXml() + "&" + "format=xml");
        List<List<String>> result = service.getAllResults(getSimpleXml());
        TestUtil.checkRow(result.get(0), "<", "\"", ">", "&", ",", "'", "\t", "\n");
    }

}
