package org.flymine.dataloader;

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;

public class IntegrationWriterFactoryTest extends TestCase
{
    protected ObjectStoreAbstractImpl os;
    protected IntegrationWriter iw;

    public IntegrationWriterFactoryTest(String args) {
        super(args);
    }

    public void setUp() throws Exception {
        os = (ObjectStoreAbstractImpl) ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public void testWorks() throws Exception {
        iw = IntegrationWriterFactory.getIntegrationWriter("integration.unittest", "source1");
        assertNotNull(iw);

    }

    public void testWrongProps() throws Exception {
        try {
            iw = IntegrationWriterFactory.getIntegrationWriter("integration.wrong", "source1");
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}
