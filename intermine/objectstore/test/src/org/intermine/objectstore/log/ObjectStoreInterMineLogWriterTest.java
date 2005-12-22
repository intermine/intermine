package org.intermine.objectstore.log;

import junit.framework.TestCase;

import org.intermine.log.InterMineLogger;
import org.intermine.log.InterMineLoggerFactory;
import org.apache.log4j.Logger;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 * User: pmclaren
 * Date: 12-Dec-2005
 * Time: 17:30:59
 */

/**
 * @author Peter McLaren
 **/
public class ObjectStoreInterMineLogWriterTest extends TestCase
{

    private static final Logger LOG = Logger.getLogger(ObjectStoreInterMineLogWriterTest.class);

    InterMineLogger imLog;
    String logModelNameSpace;

    protected void setUp() throws Exception {
        LOG.debug("ObjectStoreInterMineLogWriterTest.setUp() called!");
        imLog = InterMineLoggerFactory.getInterMineLogger();
        imLog.logMessage("ObjectStoreInterMineLogWriterTest", "setUp() called!");
    }

    public void testSimpleLogging() throws Exception {

        LOG.debug("ObjectStoreInterMineLogWriterTest.testSimpleLogging() was called!");
        imLog.logMessage("ObjectStoreInterMineLogWriterTest", "testing the logging!");
        assertEquals(1, 1);
    }

}
