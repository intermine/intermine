package org.flymine.objectstore.ojb;

import org.flymine.objectstore.ObjectStoreWriterTestCase;
import org.flymine.objectstore.ObjectStoreFactory;

public class ObjectStoreWriterOjbImplTest extends ObjectStoreWriterTestCase
{
    public ObjectStoreWriterOjbImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        writer = new ObjectStoreWriterOjbImpl(os);
    }


}
