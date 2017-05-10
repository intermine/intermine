package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class XmlUtilTest extends TestCase
{


    public void testIndentXmlSimple() throws Exception {
        String input = "<query name=\"\" model=\"testmodel\" view=\"Employee\"><constraint path=\"Employee.age\" op=\"=\" " +
                "value=\"10\"/><constraint path=\"Employee.department.name\" op=\"=\" value=\"DepartmentA1\"/></query>";
        String expected = "<query name=\"\" model=\"testmodel\" view=\"Employee\">\n" +
                "  <constraint path=\"Employee.age\" op=\"=\" value=\"10\"/>\n" +
                "  <constraint path=\"Employee.department.name\" op=\"=\" value=\"DepartmentA1\"/>\n" +
                "</query>";

        String output = XmlUtil.indentXmlSimple(input);
        System.out.println(output);
        assertEquals(output, expected);
    }

    public void testFixEntityNames() throws Exception {
        assertEquals("foo &gamma; bar &beta; zz",
                     XmlUtil.fixEntityNames("foo &ggr; bar &bgr; zz"));
    }
}
