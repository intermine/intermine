package org.flymine.objectstore.ojb;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.flymine.objectstore.ObjectStoreAbstractImplTestCase;
import org.flymine.objectstore.ObjectStoreTestCase;
import org.flymine.objectstore.ObjectStoreFactory;

public class ObjectStoreOjbImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        ObjectStoreTestCase.os = (ObjectStoreOjbImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();

        results.put("InterfaceReference", NO_RESULT);
        results.put("InterfaceCollection", NO_RESULT);
    }

    public ObjectStoreOjbImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreOjbImplTest.class);
    }
}
