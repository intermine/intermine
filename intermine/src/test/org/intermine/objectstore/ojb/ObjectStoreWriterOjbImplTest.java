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
