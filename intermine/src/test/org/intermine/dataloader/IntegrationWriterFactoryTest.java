package org.flymine.dataloader;

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreException;

public class IntegrationWriterFactoryTest extends TestCase
{
    protected IntegrationWriter iw;

    public IntegrationWriterFactoryTest(String args) {
        super(args);
    }

    public void testValidAlias() throws Exception {
        IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter("integration.unittest", "source1");
        assertNotNull(iw);        
    }

    public void testInvalidAlias() throws Exception {
        try {
            IntegrationWriterFactory.getIntegrationWriter("integration.wrong", "source1");
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}
