package org.intermine.task;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;

import junit.framework.Test;

/**
 * AcceptanceTestTaskTest class
 *
 * @author Kim Rutherford
 */

public class AcceptanceTestTaskTest extends StoreDataTestCase
{
    ObjectStore os;

    public AcceptanceTestTaskTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        os = ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public void executeTest(String type) {

    }

    public static Test suite() {
        return buildSuite(AcceptanceTestTaskTest.class);
    }
    
    public void testReadOneTest1() throws Exception {
        String expSql = "select * from intermineobject";

        String testConf =
            "no-results {\n"
            + "  sql: " + expSql + "\n"
            + "}\n";
        
        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
        
        assertEquals(expSql, test.getSql());
        assertNull(test.getNote());
    }

    public void testReadOneTest2() throws Exception {
        String expSql = "select * from intermineobject";
        String expNote = "some note";
        String testConf =
            "no-results {\n"
            + "  sql: " + expSql + "\n"
            + "  note: " + expNote + "\n"
            + "}\n";
        
        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
        
        assertEquals(expSql, test.getSql());
        assertEquals(expNote, test.getNote());
    }
    
    public void testReadOneTest3() throws Exception {
        String expSql = "select * from intermineobject";
        String expNote = "some note referring to ticket #123";
        String expMaxResults = "10";
        String testConf =
            "no-results {\n"
            + "  sql: " + expSql + "; \n"
            + "  note: " + expNote + "\n"
            + "  max-results: " + expMaxResults + "\n"
            + "}\n";
        
        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
        
        assertEquals(expSql, test.getSql());
        assertEquals(expNote, test.getNote());
        assertEquals(new Integer(10), test.getMaxResults());  
    }
    
    public void testHyperlinking() throws Exception {
        String note = "some note referring to ticket #123";
        String expNote = "some note referring to ticket <a href=\"" 
            + AcceptanceTestTask.TRAC_TICKET_URL_PREFIX + "123\">#123</a>";
        String hyperlinkedNote = AcceptanceTestTask.hyperLinkNote(note);
        assertEquals(expNote, hyperlinkedNote);
    }
    
    public void testReadOneTestError1() throws Exception {
        String testConf =
            "no-results {\n"            
             + "}\n";
        
        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        
        try {
            AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
            fail("expected IOException");
        } catch (IOException e) {
            // expected
        }
    }
    
    public void testReadOneTestError2() throws Exception {
        String testConf =
            "no-results {\n"
             + "}\n";
        
        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        
        try {
            AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
            fail("expected IOException");
        } catch (IOException e) {
            // expected
        }
    }

    public void testReadOneTestError3() throws Exception {
        String testConf =
            "bogus_type {\n"
             + "  sql: select * from intermineobject\n"
             + "}\n";
        
        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        
        try {
            AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
            fail("expected IOException");
        } catch (IOException e) {
            // expected
        }
    }
    
    public void testAcceptanceTestReadAll() throws Exception {
        String configFile = "acceptance_test.conf";

        AcceptanceTestTask task = new AcceptanceTestTask();
        
        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);

        if (inputStream == null) {
            throw new BuildException("cannot find config file (" + configFile 
                                     + ") in the class path");
        }
        
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));
        List testResults = task.runAllTests(db, reader);
        
        assertEquals(5, testResults.size());

        for (int i = 0; i < testResults.size(); i++) {
            AcceptanceTestResult atr = (AcceptanceTestResult) testResults.get(i);
            
            if (i < 4) {
                if (!atr.isSuccessful()) {
                    fail("test for " + atr.getTest().getSql() + " failed");
                }
            } else {
                if (atr.isSuccessful()) {
                    fail("test for " + atr.getTest().getSql() + " should have failed");
                }
            }
        }
    }

    public void testQueries() {

    }

}
