package org.flymine.objectstore.ojb;

import junit.framework.Test;

import org.flymine.objectstore.ObjectStoreAbstractImplTestCase;

public class ObjectStoreOjbImplTest extends ObjectStoreAbstractImplTestCase
{
    public ObjectStoreOjbImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreOjbImplTest.class);
    }
}
