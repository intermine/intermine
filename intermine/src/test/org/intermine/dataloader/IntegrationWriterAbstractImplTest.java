package org.flymine.dataloader;

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;

public class IntegrationWriterAbstractImplTest extends TestCase
{
    protected ObjectStoreAbstractImpl os;
    protected IntegrationWriterAbstractImpl iWriter;

    public IntegrationWriterAbstractImplTest(String args) {
        super(args);
    }

    public void setUp() throws Exception {
        os = (ObjectStoreAbstractImpl) ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public void testWorks() throws Exception {
        iWriter = IntegrationWriterAbstractImpl.getInstance("source1", os, "dataloader.unittest");
        assertNotNull(iWriter);

    }

    public void testWrongProps() throws Exception {
        try {
            iWriter = IntegrationWriterAbstractImpl.getInstance("source1", os, "dataloader.wrong");
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }


}
