package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.intermine.web.task.PrecomputeTemplatesTask;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;


/**
 * @author Jakub Kulaviak
 **/
public class TestUtil
{

    public static List<List<String>> parseTabResult(String tabResult) {
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

    public static List<List<String>> parseXMLResult(String xmlResult) throws Exception {
        InputSource is = new  InputSource(new StringReader(xmlResult));
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        XMLResultHandler handler = new XMLResultHandler();
        factory.newSAXParser().parse(is, handler);
        return handler.getResults();
    }

    public static String getResult(String requestString) throws IOException,
            MalformedURLException, SAXException {
        WebConversation wc = new WebConversation();
        WebRequest     req = new GetMethodWebRequest(requestString);
        return wc.getResponse(req).getText();
    }
    
    
    public static int getResponseCode(String requestString) throws MalformedURLException, IOException, SAXException {
        try {
            WebConversation wc = new WebConversation();
            WebRequest     req = new GetMethodWebRequest( requestString);
            return wc.getResponse(req).getResponseCode();
        } catch (HttpException e) {
            return e.getResponseCode();
        }
    }
    
    public static String getResponseMessage(String requestString) throws MalformedURLException, IOException, SAXException {
        try {
            WebConversation wc = new WebConversation();
            WebRequest     req = new GetMethodWebRequest( requestString);
            WebResponse res = wc.getResponse(req);
            return res.getResponseMessage();
            
        } catch (HttpException e) {
            return e.getResponseMessage();
        }
    }
    
    public static void checkEmployee(List<String> employee, String name,
            String age, String end, String fullTime) {
        TestCase.assertEquals(4, employee.size());
        TestCase.assertEquals(employee.get(0), name);
        TestCase.assertEquals(employee.get(1), age);
        TestCase.assertEquals(employee.get(2), end);
        TestCase.assertEquals(employee.get(3), fullTime);
    }

    public static String getServiceBaseURL() throws IOException {
        InputStream webProps = PrecomputeTemplatesTask.class
            .getClassLoader().getResourceAsStream("WEB-INF/web.properties");
        ResourceBundle rb = new PropertyResourceBundle(webProps);
        String context = rb.getString("webapp.path").trim();
        String webAppUrl = rb.getString("webapp.deploy.url").trim();
        String serviceUrl = webAppUrl + "/" +  context + "/" + WebServiceConstants.MODULE_NAME;
        return serviceUrl;
    }

}
