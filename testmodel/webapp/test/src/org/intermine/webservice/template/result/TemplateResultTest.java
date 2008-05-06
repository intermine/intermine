package org.intermine.webservice.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


import junit.framework.TestCase;

import org.intermine.web.task.PrecomputeTemplatesTask;
import org.intermine.webservice.TestUtil;
import org.intermine.webservice.WebServiceConstants;



/**
 * Tests query result web service.  
 * Tests urls like: http://localhost:8080/service/query/results?query=...
 * @author Jakub Kulaviak
 **/
public class TemplateResultTest extends TestCase
{
    
    private String serviceUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.serviceUrl = TestUtil.getServiceBaseURL() + "/template/results?";
    }
    
    /**
     * Default url (it is url with default values in it) for this template is
     * http://localhost:8080/intermine-test/service/template/results?name=employeesOfACertainAge&op1=gt&value1=10&op2=ne&value2=10&size=10&format=tab 
     * This test checks, that values in template are replaced with actual parameters in request.
     * @throws Exception if some error occurs
     */
    public void testForNonDefaultParameterValues() throws Exception {
        String tabResult = getResultForQueryString("name=employeesOfACertainAge&op1=gt" +
        		"&value1=20&op2=ne&value2=40&size=10&format=tab").trim();
        List<List<String>> results = TestUtil.parseTabResult(tabResult);
        
        System.out.println("result: " + tabResult);
        
        assertEquals("EmployeeA3", results.get(0).get(0));
        assertEquals("30", results.get(0).get(1));

        assertEquals("EmployeeB2", results.get(1).get(0));
        assertEquals("50", results.get(1).get(1));

        assertEquals("EmployeeB3", results.get(2).get(0));
        assertEquals("60", results.get(2).get(1));
    }

    /**
     * Test template with 4 constraints.
     * @throws Exception
     */
    public void testFourConstraints() throws Exception {
        String tabResult = getResultForQueryString("name=fourConstraints&op1=CONTAINS&value1=Employee&op2=lt&value2=20&op3=gt&value3=20&op4=eq&value4=false&size=10&format=tab").trim();
        List<List<String>> results = TestUtil.parseTabResult(tabResult);

        assertEquals("EmployeeA3", results.get(0).get(0));
        assertEquals("30", results.get(0).get(1));
        assertEquals("3", results.get(0).get(2));
        assertEquals("false", results.get(0).get(3));
    }
         
    /**
     * Tests that error message appear when public template with this name doesn't exist.
     * @throws Exception when an error occurs
     */
    public void testErrorXMLQuery() throws Exception {
        String result = getResultForQueryString("name=unknown").trim();
        assertTrue(result.startsWith("<error>"));
        assertTrue(result.contains("<message>"));
        assertTrue(result.contains("exist"));
        assertTrue(result.contains("</message>"));
        assertTrue(result.endsWith("</error>"));        
    }

    public String getServiceUrl() {
        return serviceUrl;
    }
    

    private String getResultForQueryString(String parameterString) throws Exception {
        String requestString = getServiceUrl() + parameterString;
        return TestUtil.getResult(requestString);
    }
}
