package org.flymine.util;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;


public class ListBeanTest extends TestCase
{
    private String s1, s2;
    private Integer v1, v2;
    private List list;

    public ListBeanTest(String arg) {
        super(arg);
    }

    public void setUp() {
        s1 = "s1";
        s2 = "s2";
        v1 = new Integer(101);
        v2 = new Integer(102);
        list = new ArrayList();
        list.add(s1);
        list.add(s2);
        list.add(v1);
        list.add(v2);
    }

    public void testReturnsCorrectList() throws Exception {
        ListBean lb = new ListBean();
        lb.setItems(list);
        assertEquals(list, lb.getItems());
    }

    public void testClearsList() throws Exception {
        ListBean lb = new ListBean();
        List list2 = new ArrayList();
        list2.add(s1);
        list2.add(v1);

        lb.setItems(list);
        lb.setItems(list2);
        assertEquals(list2, lb.getItems());
    }
}
