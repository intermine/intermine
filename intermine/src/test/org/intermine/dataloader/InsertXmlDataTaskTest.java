package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
