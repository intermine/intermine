package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreException;

public class IntegrationWriterFactoryTest extends TestCase
{
    protected IntegrationWriter iw;

    public IntegrationWriterFactoryTest(String args) {
        super(args);
    }

    public void testValidSingleAlias() throws Exception {
        IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter("integration.unittestsingle");
        assertNotNull(iw);        
    }

    public void testValidMultiAlias() throws Exception {
        IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter("integration.unittestmulti");
        assertNotNull(iw);
    }

    public void testInvalidAlias() throws Exception {
        try {
            IntegrationWriterFactory.getIntegrationWriter("integration.wrong");
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}
