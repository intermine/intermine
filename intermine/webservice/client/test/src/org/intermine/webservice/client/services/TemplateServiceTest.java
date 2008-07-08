package org.intermine.webservice.client.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.webservice.client.template.TemplateParameter;
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
public class TemplateServiceTest extends TestCase
{

    //http://localhost:8080/intermine-test/service/template/results?name=employeesOfACertainAge&op1=gt&value1=10&op2=ne&value2=10&size=10&format=tab
    public void testGetCount() {
        TemplateService service = TestUtil.getTemplateService();
        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        parameters.add(new TemplateParameter("gt", "20"));
        parameters.add(new TemplateParameter("ne", "50"));
        int actual = service.getCount("employeesOfACertainAge", parameters);
        assertEquals(3, actual);
    }

    //http://localhost:8080/intermine-test/service/template/results?name=fourConstraints&op1=CONTAINS&value1=Employee&op2=lt
    //&value2=10&op3=gt&value3=30&op4=eq&value4=true&size=10&format=tab
    public void testGetResult() throws IOException {
        TemplateService service = TestUtil.getTemplateService();
        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        parameters.add(new TemplateParameter("contains", "Employee"));
        parameters.add(new TemplateParameter("lt", "10"));
        parameters.add(new TemplateParameter("gt", "30"));
        parameters.add(new TemplateParameter("eq", "true"));
        List<List<String>> results = service.getResult("fourConstraints", parameters, 1, 10).getData();
        assertEquals(3, results.size());
        TestUtil.checkRow(results.get(0), "EmployeeB1", "40", "4", "true");
        TestUtil.checkRow(results.get(1), "EmployeeB2", "50", "5", "true");
        TestUtil.checkRow(results.get(2), "EmployeeB3", "60", "6", "true");
    }
}
