package org.flymine.dataloader;

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreFactory;

public class IntegrationWriterAbstractImplTest extends TestCase
{
    public IntegrationWriterAbstractImplTest(String args) {
        super(args);
    }

    public void setUp() throws Exception {

    }

    public void testWorks() throws Exception {
        ObjectStoreAbstractImpl os = (ObjectStoreAbstractImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        IntegrationWriterAbstractImpl iWriter;
        iWriter = IntegrationWriterAbstractImpl.getInstance("source1", os, "dataloader.unittest");
        assertNotNull(iWriter);

    }




}
