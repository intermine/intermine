package org.flymine.task;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

public class InsertXmlDataTaskTest extends TestCase
{
    public InsertXmlDataTaskTest(String arg) {
        super(arg);
    }

    private InsertXmlDataTask task;

    public void setUp() {
        task = new InsertXmlDataTask();
        task.setIntegrationWriter("integration.test");
        task.setFile(new File("filename"));
    }

    public void testNoIntegrationWriter() {
        task.setIntegrationWriter(null);
        try {
            task.execute();
            fail("Expected: BuildException");
        } catch (BuildException e) {
        }
    }

    public void testNoFile() {
        task.setFile(null);
        try {
            task.execute();
            fail("Expected: BuildException");
        } catch (BuildException e) {
        }
    }
}
