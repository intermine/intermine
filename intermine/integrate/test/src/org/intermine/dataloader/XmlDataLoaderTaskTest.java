package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

public class XmlDataLoaderTaskTest extends TestCase
{
    public XmlDataLoaderTaskTest(String arg) {
        super(arg);
    }

    private XmlDataLoaderTask task;

    public void setUp() {
        task = new XmlDataLoaderTask();
        task.setIntegrationWriter("integration.test");
        task.setSourceName("sourceName");
    }

    public void testNoIntegrationWriter() {
        task.setIntegrationWriter(null);
        try {
            task.execute();
            fail("Expected: BuildException");
        } catch (BuildException e) {
        }
    }


    public void testNoSourceName() {
        task.setSourceName(null);
        try {
            task.execute();
            fail("Expected: BuildException");
        } catch (BuildException e) {
        }
    }
}
