package org.intermine.web.logic.export;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class RowFormatterImplTest extends TestCase
{

    /**
     * Test basic format behavior.
     */
    public void testFormat() {
        RowFormatterImpl formatter = new RowFormatterImpl(",", true);
        Object o1 = new Integer(1);
        Object o2 = new Date();
        Object o3 = "test";
        List<Object> objs = new ArrayList<Object>();
        objs.add(o1);
        objs.add(o2);
        objs.add(o3);
        String result = formatter.format(objs);
        assertEquals("1,\""+o2.toString()+"\",\"test\"", result);
    }

    /**
     * Tests correct behavior for strings containing used delimiter.
     */
    public void testStringWithDelimiter() {
        RowFormatterImpl formatter = new RowFormatterImpl(",", false);
        Object o1 = "first";
        Object o3 = "thi,rd";
        List<Object> objs = new ArrayList<Object>();
        objs.add(o1);
        objs.add(o3);
        String result = formatter.format(objs);
        assertEquals("first,\"thi,rd\"", result);        
    }
    
    public void testDoubleQuotating() {
        RowFormatterImpl formatter = new RowFormatterImpl(",", true);
        Object o1 = "first second third\"";
        List<Object> objs = new ArrayList<Object>();
        objs.add(o1);
        String result = formatter.format(objs);
        assertEquals("\"first second third\"\"\"", result);
    }
    
    /**
     * Test quoting a string that's null.
     */
    public void testEmptyString() throws Exception {
        RowFormatterImpl formatter = new RowFormatterImpl(",", true);
        Object o1 = "";
        List<Object> objs = new ArrayList<Object>();
        objs.add(o1);
        String result = formatter.format(objs);
        assertEquals("\"\"", result);
    }

}
