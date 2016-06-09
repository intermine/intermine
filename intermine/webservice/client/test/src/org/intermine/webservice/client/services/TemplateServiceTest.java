package org.intermine.webservice.client.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.webservice.client.template.TemplateParameter;
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
 * Tests functionality of TemplateService - client class, implementing easy
 * access to InterMine web service. That's why it tests the web service itself
 * as well.
 *
 * @author Jakub Kulaviak
 **/
public class TemplateServiceTest extends TestCase
{

    private final static String HOST = "http://localhost:8080";
    private final static String PATH = "/intermine-test/service/template/results";
    /**
     * Checks Java client and that default parameters of template are replaced with
     * parameters provided by client.
     */
    public void testNonDefaultParameters() {
        DummyTemplateService service = TestUtil.getTemplateService();
        service.setFakeResponse("<ResultSet><Result><i>EmployeeA1</i><i>10</i><i>1.1</i><i>true</i></Result><Result><i>EmployeeA2</i><i>20</i><i>2.2</i><i>false</i></Result></ResultSet>");
        service.setExpectedRequest(HOST + PATH + "?"
                + "name=fourConstraints"
                + "&constraint1=Employee.name&op1=contains&value1=EmployeeA"
                + "&constraint2=Employee.age&op2=gt&value2=10&code2=B"
                + "&constraint3=Employee.age&op3=lt&value3=60&code3=C"
                + "&constraint4=Employee.fullTime&op4=eq&value4=true"
                + "&start=0&format=xml");
        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        parameters.add(new TemplateParameter("Employee.name", "contains", "EmployeeA", null));

        TemplateParameter par1 = new TemplateParameter("Employee.age", "gt", "10", "B");
        parameters.add(par1);

        TemplateParameter par2 = new TemplateParameter("Employee.age", "lt", "60", "C");
        parameters.add(par2);

        parameters.add(new TemplateParameter("Employee.fullTime", "eq", "true", null));
        List<List<String>> results = service.getAllResults("fourConstraints", parameters);
        assertEquals(2, results.size());
        // returns 2 results, notice that the logic for constraints B and C is OR -> returns Employee of age 10
        TestUtil.checkRow(results.get(0), "EmployeeA1", "10", "1.1", "true");
        TestUtil.checkRow(results.get(1), "EmployeeA2", "20", "2.2", "false");
    }
}
