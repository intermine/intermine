package org.flymine.objectstore;

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

import org.flymine.model.testmodel.*;

public class ObjectStoreWriterAbstractImplTest extends TestCase
{
    private ObjectStoreWriterTestImpl osw;

    public ObjectStoreWriterAbstractImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        osw = new ObjectStoreWriterTestImpl(os);
    }

    public void testHasValidKeyValidObject() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO employee = new CEO();
        employee.setName("EmployeeB1");
        employee.setAge(40);
        employee.setAddress(address);

        assertTrue(osw.hasValidKey(employee));
    }

    public void testHasValidKeyInvalidObjectNoReference() throws Exception {
        CEO ceo = new CEO();
        ceo.setName("EmployeeB1");
        ceo.setAge(40);

        assertFalse(osw.hasValidKey(ceo));
    }

    public void testHasValidKeyInvalidObjectNoAttribute() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO ceo = new CEO();
        ceo.setAge(40);
        ceo.setAddress(address);

        assertFalse(osw.hasValidKey(ceo));
    }

    public void testHasValidKeyNullObject() throws Exception {
        try {
            osw.hasValidKey(null);
            fail ("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Implementation of  ObjectStoreWriterAbstractImpl for test purposes
     */
    protected class ObjectStoreWriterTestImpl extends ObjectStoreWriterAbstractImpl {
        protected ObjectStoreWriterTestImpl(ObjectStore os) {
            super(os);
        }

        public void store(Object o) throws ObjectStoreException {
        }

        public void delete(Object o) throws ObjectStoreException {
        }

        public boolean isInTransaction() throws ObjectStoreException {
            return false;
        }

        public void beginTransaction() throws ObjectStoreException {
        }

        public void commitTransaction() throws ObjectStoreException {
        }

        public void abortTransaction() throws ObjectStoreException {
        }
    }
}

