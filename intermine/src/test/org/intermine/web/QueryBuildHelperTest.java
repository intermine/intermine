package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.TreeSet;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;

import junit.framework.TestCase;

public class QueryBuildHelperTest extends TestCase
{
    public QueryBuildHelperTest (String testName) {
        super(testName);
    }
    
    public void testAliasClass() throws Exception {
        Collection existingAliases = new TreeSet();

        existingAliases.add("Type_0");
        existingAliases.add("Type_1");
        existingAliases.add("Type_2");
        existingAliases.add("OtherType_0");
        existingAliases.add("OtherType_1");

        String newAlias = QueryBuildHelper.aliasClass(existingAliases, "OtherType");

        assertEquals("OtherType_2", newAlias);
    }

    public void testAddClass() throws Exception {
        Map queryClasses = new HashMap();
        QueryBuildHelper.addClass(queryClasses, "org.flymine.model.testmodel.Employee");
        assertEquals(1, queryClasses.size());
        QueryBuildHelper.addClass(queryClasses, "org.flymine.model.testmodel.Employee");
        assertEquals(2, queryClasses.size());
        Set keySet = new TreeSet(queryClasses.keySet());
        Iterator iterator = keySet.iterator();
        String className1 = (String)iterator.next();
        assertEquals("Employee_0", className1);
        String className2 = (String)iterator.next();
        assertEquals("Employee_1", className2);
    }
}
