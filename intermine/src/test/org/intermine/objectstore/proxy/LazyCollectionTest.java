/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.proxy;

import junit.framework.TestCase;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Manager;

public class LazyCollectionTest extends TestCase {

    private LazyCollection col;


    public LazyCollectionTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
    }

    public void testNullArg() throws Exception {
        try {
            col = new LazyCollection(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }


    public void testNotOneSelectItem() throws Exception {
        Query q1 = new Query();
        try {
            col = new LazyCollection(q1);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        q1.addToSelect(qc1).addToSelect(qc2);
        try {
            col = new LazyCollection(q1);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testSelectNotAQueryClass() throws Exception {
        Query q1 = new Query();
        QueryValue qv1 = new QueryValue(new Integer(1234));
        q1.addToSelect(qv1);
        try {
            col = new LazyCollection(q1);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
