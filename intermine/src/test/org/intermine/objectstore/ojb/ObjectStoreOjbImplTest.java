/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
