package org.intermine.task;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.*;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;

import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AcceptanceTestTaskTest
{
    private static ObjectStore os;

    @BeforeClass
    public static void setUp() throws Exception {
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
        Map data = ObjectStoreTestUtils.getTestData("testmodel", "testmodel_data.xml");
        ObjectStoreTestUtils.storeData(osw, data);
        osw.close();
    }

    @Test
    public void testReadOneTest1() throws Exception {
        String expSql = "select * from intermineobject";

        String testConf =
            "no-results {\n"
            + "  sql: " + expSql + "\n"
            + "}\n";

        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);
        AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);

        Assert.assertEquals(expSql, test.getSql());
        Assert.assertNull(test.getNote());
    }

    @Test
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

        Assert.assertEquals(expSql, test.getSql());
        Assert.assertEquals(expNote, test.getNote());
    }

    @Test
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

        Assert.assertEquals(expSql, test.getSql());
        Assert.assertEquals(expNote, test.getNote());
        Assert.assertEquals(new Integer(10), test.getMaxResults());
    }

    @Test
    public void testHyperlinking() throws Exception {
        String note = "some note referring to ticket #123";
        String expNote = "some note referring to ticket <a href=\""
            + AcceptanceTestTask.TRAC_TICKET_URL_PREFIX + "123\">#123</a>";
        String hyperlinkedNote = AcceptanceTestTask.hyperLinkNote(note);
        Assert.assertEquals(expNote, hyperlinkedNote);
    }

    @Test
    public void testReadOneTestError1() throws Exception {
        String testConf =
            "no-results {\n"
             + "}\n";

        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);

        try {
            AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
            Assert.fail("expected IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testReadOneTestError2() throws Exception {
        String testConf =
            "no-results {\n"
             + "}\n";

        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);

        try {
            AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
            Assert.fail("expected IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testReadOneTestError3() throws Exception {
        String testConf =
            "bogus_type {\n"
             + "  sql: select * from intermineobject\n"
             + "}\n";

        StringReader sr = new StringReader(testConf);
        LineNumberReader reader = new LineNumberReader(sr);

        try {
            AcceptanceTest test = AcceptanceTestTask.readOneTestConfig(reader);
            Assert.fail("expected IOException");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
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

        Assert.assertEquals(5, testResults.size());

        for (int i = 0; i < testResults.size(); i++) {
            AcceptanceTestResult atr = (AcceptanceTestResult) testResults.get(i);

            if (i < 4) {
                if (!atr.isSuccessful()) {
                    Assert.fail("test for " + atr.getTest().getSql() + " failed");
                }
            } else {
                if (atr.isSuccessful()) {
                    Assert.fail("test for " + atr.getTest().getSql() + " should have failed");
                }
            }
        }
    }
}
