package org.intermine.webservice.query.result;

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
import java.util.List;


import junit.framework.TestCase;

import org.intermine.webservice.TestUtil;



/**
 * Tests query result web service.  
 * Tests urls like: http://localhost:8080/service/query/results?query=...
 * @author Jakub Kulaviak
 **/
public class QueryResultTest extends TestCase
{
    
    private String serviceUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.serviceUrl = TestUtil.getServiceBaseURL() + "/query/results?";
    }

    /**
     * Tests tab output.
     * @throws Exception if some error occurs
     */
    public void testEmployeeTabOutput() throws Exception {
        String tabResult = getResultForQueryString("query=" + getQuery());
        List<List<String>> results = TestUtil.parseTabResult(tabResult);
        checkEmployees(results);
    }
     

    /**
     * Tests xml output that is formed information about employees.
     * @throws Exception if some error occurs
     */
    public void testEmployeeXMLOutput() throws Exception {
        String xmlResult = getResultForQueryString("format=xml&query=" + getQuery());
        List<List<String>> results = TestUtil.parseXMLResult(xmlResult);
        checkEmployees(results);
    }
    
    /**
     * Tests that error message appear when query xml is not well formatted.
     * @throws Exception when an error occurs
     */
    public void testErrorXMLQuery() throws Exception {
        String result = getResultForQueryString("query=a" + getQuery()).trim();
        assertTrue(result.startsWith("<error>"));
        assertTrue(result.contains("<message>"));
        assertTrue(result.contains("</message>"));
        assertTrue(result.endsWith("</error>"));        
    }

    /**
     * Tests that when parameter 'onlyTotalCount' is set, then only total count of results is returned.
     * @throws Exception when an error occurs 
     */
//    public void testOnlyTotalCount() throws Exception {
//        String result = getResult("onlyTotalCount=yes&query=" + getQuery()).trim();
//        assertEquals("6", result);
//    }
    
    /**
     * Tests functionality of counting of all results and count of results returned 
     * @throws Exception
     */
//    public void testXMLResultAttributes() throws Exception {
//        String xmlResult = getResult("totalCount=yes&format=xml&start=5&query=" + getQuery()).trim();
//        InputSource is = new  InputSource(new StringReader(xmlResult));
//        SAXParserFactory factory = SAXParserFactory.newInstance();
//        factory.setValidating(true);
//        XMLResultHandler handler = new XMLResultHandler();
//        factory.newSAXParser().parse(is, handler);
//        Attributes atts = handler.getRootAttributes();
//        for (int i=0; i<atts.getLength(); i++) {
//            if (atts.getLocalName(i).equals("firstResultPosition")) {
//                assertEquals("5", atts.getValue(i).trim());        
//            }
//            if (atts.getLocalName(i).equals("totalResultsReturned")) {
//                assertEquals("2", atts.getValue(i).trim());        
//            }
//            if (atts.getLocalName(i).equals("totalResultsAvailable")) {
//                assertEquals("6", atts.getValue(i).trim());        
//            }
//        }
//    }

    public String getServiceUrl() {
        return serviceUrl;
    }
    
    private void checkEmployees(List<List<String>> results) {
        TestUtil.checkEmployee(results.get(0), "EmployeeA1", "10", "1", "true");
        TestUtil.checkEmployee(results.get(1), "EmployeeA2", "20", "2", "true");
        TestUtil.checkEmployee(results.get(2), "EmployeeA3", "30", "3", "false");
        TestUtil.checkEmployee(results.get(3), "EmployeeB1", "40", "4", "true");
        TestUtil.checkEmployee(results.get(4), "EmployeeB2", "50", "5", "true");
        TestUtil.checkEmployee(results.get(5), "EmployeeB3", "60", "6", "true");
    }

    private String getResultForQueryString(String parameterString) throws Exception {
        String requestString = getServiceUrl() + parameterString;
        return TestUtil.getResult(requestString);
    }

    private String getQuery() throws IOException {
        //BufferedReader br = new BufferedReader(new FileReader("/home/jakub/svn/dev/testmodel/webapp/test/resources/ServiceServletTest1.xml"));
        InputStream is = getClass().getClassLoader().getResourceAsStream("ServiceServletTest.xml");
        if (is == null) {
            throw new FileNotFoundException("ServiceServletTest.xml wasn't found.");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ret = "";
        String l;
        while ((l = br.readLine()) != null) {
            ret = ret + l;
        }
        return ret;
    }
}
