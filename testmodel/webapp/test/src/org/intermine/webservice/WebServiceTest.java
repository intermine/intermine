package org.intermine.webservice;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.intermine.web.task.PrecomputeTemplatesTask;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

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
public class WebServiceTest extends TestCase
{
    
    private String serviceUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        InputStream webProps = PrecomputeTemplatesTask.class
            .getClassLoader().getResourceAsStream("WEB-INF/web.properties");
        ResourceBundle rb = new PropertyResourceBundle(webProps);
        String context = rb.getString("webapp.path").trim();
        String webAppUrl = rb.getString("webapp.deploy.url").trim();
        this.serviceUrl = webAppUrl + "/" +  context + "/data/query/results?"; 
    }
    
    /**
     * Tests tab separated output that is formed information about employees.
     * @throws Exception if some error occurs
     */
    public void testEmployeeTabOutput() throws Exception {
        String tabResult = getResult("query=" + getQuery());
        List<List<String>> results = parseTabResult(tabResult);
        checkEmployees(results);
    }
    

    /**
     * Tests xml output that is formed information about employees.
     * @throws Exception if some error occurs
     */
    public void testEmployeeXMLOutput() throws Exception {
        String xmlResult = getResult("output=xml&query=" + getQuery());
        List<List<String>> results = parseXMLResult(xmlResult);
        checkEmployees(results);
    }

    /**
     * Tests that error message appear when query xml is not well formatted.
     * @throws Exception when an error occurs
     */
    public void testErrorXMLQuery() throws Exception {
        String result = getResult("query=a" + getQuery()).trim();
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
        assertEquals(6, results.size());
        checkEmployee(results.get(0), "EmployeeA1", "10", "1", "true");
        checkEmployee(results.get(1), "EmployeeA2", "20", "2", "true");
        checkEmployee(results.get(2), "EmployeeA3", "30", "3", "false");
        checkEmployee(results.get(3), "EmployeeB1", "40", "4", "true");
        checkEmployee(results.get(4), "EmployeeB2", "50", "5", "true");
        checkEmployee(results.get(5), "EmployeeB3", "60", "6", "true");
    }

    private void checkEmployee(List<String> employee, String name,
            String age, String end, String fullTime) {
        assertEquals(4, employee.size());
        assertEquals(employee.get(0), name);
        assertEquals(employee.get(1), age);
        assertEquals(employee.get(2), end);
        assertEquals(employee.get(3), fullTime);
    }

    private List<List<String>> parseTabResult(String tabResult) {
        List<List<String>> ret = new ArrayList<List<String>>();
        String[] rows = tabResult.split("\n");
        for (String row : rows) {
            String[] values = row.split("\t");
            List<String> retRow = new ArrayList<String>();
            for (String value : values) {
                retRow.add(value.trim());
            }
            ret.add(retRow);
        }
        return ret;
    }

    private List<List<String>> parseXMLResult(String xmlResult) throws Exception {
        InputSource is = new  InputSource(new StringReader(xmlResult));
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        XMLResultHandler handler = new XMLResultHandler();
        factory.newSAXParser().parse(is, handler);
        return handler.getResults();
    }
    
    private String getResult(String parameterString) throws Exception {
        String requestString = getServiceUrl() + parameterString;
        WebConversation wc = new WebConversation();
        WebRequest     req = new GetMethodWebRequest( requestString);
        return wc.getResponse(req).getText();
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
