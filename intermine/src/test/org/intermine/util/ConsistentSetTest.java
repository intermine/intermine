package org.flymine.util;

import junit.framework.*;

import java.util.Set;

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

}
