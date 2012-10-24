package org.intermine.util;

/*
 * Copyright (C) 2002-2012 FlyMine
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
        String input = "<query name=\"\" model=\"testmodel\" view=\"Employee\"><node path=\"Employee\" " +
                "type=\"Employee\"></node><node path=\"Employee.age\" type=\"int\"><constraint op=\"=\" " +
                "value=\"10\"></constraint></node><node path=\"Employee.department\" type=\"Department\">" +
                "</node><node path=\"Employee.department.name\" type=\"String\"><constraint op=\"=\" " +
                "value=\"DepartmentA1\"></constraint></node></query>";
        String expected = "<query name=\"\" model=\"testmodel\" view=\"Employee\">\n" +
                "  <node path=\"Employee\" type=\"Employee\">\n" +
                "  </node>\n" +
                "  <node path=\"Employee.age\" type=\"int\">\n" +
                "    <constraint op=\"=\" value=\"10\">\n" +
                "    </constraint>\n" +
                "  </node>\n" +
                "  <node path=\"Employee.department\" type=\"Department\">\n" +
                "  </node>\n" +
                "  <node path=\"Employee.department.name\" type=\"String\">\n" +
                "    <constraint op=\"=\" value=\"DepartmentA1\">\n" +
                "    </constraint>\n" +
                "  </node>\n" +
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
