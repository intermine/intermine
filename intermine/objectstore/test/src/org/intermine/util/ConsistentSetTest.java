package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Set;
import java.util.HashSet;

public class ConsistentSetTest extends TestCase
{
    public ConsistentSetTest(String arg1) {
        super(arg1);
    }

   public void testAdd() throws Exception {
       Object o1 = "string1";
       Object o2 = "string2";

       Set set = new ConsistentSet();

       set.add(o1);
       set.add(o2);

       assertEquals(2, set.size());
       assertTrue(set.contains(o1));
       assertTrue(set.contains(o2));
   }

   public void testAddingSame() throws Exception {
       Object o1 = "string1";
       Object o2 = "string2";
       Object o3 = "string2";

       Set set = new ConsistentSet();

       set.add(o1);
       set.add(o2);
       set.add(o3);

       assertEquals(2, set.size());
       assertTrue(set.contains(o1));
       assertTrue(set.contains(o2));
       assertTrue(set.contains(o3));

   }

   public void testAlteringContents() throws Exception {
       TestObject o1 = new TestObject("string1");
       TestObject o2 = new TestObject("string2");
       TestObject o3 = new TestObject("string2");
       TestObject o4 = new TestObject("somethingelse");

       Set set = new ConsistentSet();

       set.add(o1);
       set.add(o2);

       o2.a = "somethingelse";

       assertTrue(set.contains(o1));
       assertTrue(!set.contains(o3));
       assertTrue(set.contains(o4));
   }

    private class TestObject {
       public String a;

       public TestObject(String str) {
           this.a = str;
       }

       public boolean equals(Object obj) {
           if (obj instanceof TestObject) {
               return a.equals(((TestObject) obj).a);
           }
           return false;
       }

        public int hashCode() {
            return a.hashCode();
        }

    }

    public void testOutOfOrderEquals() throws Exception {
        TestObject o1 = new TestObject("string1");
        TestObject o2 = new TestObject("string2");
        TestObject o3 = new TestObject("string3");
        TestObject o4 = new TestObject("string4");

        Set set1 = new ConsistentSet();
        Set set2 = new ConsistentSet();

        set1.add(o1);
        set1.add(o2);
        set2.add(o3);
        set2.add(o4);

        o3.a = "string2";
        o4.a = "string1";

        assertEquals(set1.hashCode(), set2.hashCode());
        assertEquals(set1, set2);
    }

    public void testEqualsHashSet() throws Exception {
        TestObject o1 = new TestObject("string1");
        TestObject o2 = new TestObject("string2");

        Set set1 = new ConsistentSet();
        Set set2 = new HashSet();

        set1.add(o1);
        set1.add(o2);
        set2.add(o2);
        set2.add(o1);

        assertEquals(set1.hashCode(), set2.hashCode());
        assertEquals(set1, set2);
        assertEquals(set2.hashCode(), set1.hashCode());
        assertEquals(set2, set1);
    }
}
