package org.flymine.xml.full;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.lang.reflect.Field;
import java.util.Date;

import org.flymine.util.TypeUtil;
import org.flymine.model.testmodel.*;

public class FullRendererTest extends TestCase
{
    public void testRender() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = "<object class=\"org.flymine.model.testmodel.Employee\" implements=\"org.flymine.model.testmodel.Employable org.flymine.model.testmodel.HasAddress\">"
            + "<field name=\"age\" value=\"0\"/>"
            + "<field name=\"fullTime\" value=\"false\"/>"
            + "<field name=\"name\" value=\"Employee1\"/>"
            + "<field name=\"id\" value=\"1234\"/>"
            + "<reference name=\"department\" value=\"5678\"/>"
            + "</object>";

        assertEquals(expected, FullRenderer.render(e));
    }

    public void testRenderTypes() throws Exception {
        Types t = new Types();
        t.setId(new Integer(1234));
        t.setName("Types1");
        t.setFloatType(1.2f);
        t.setDoubleType(1.3d);
        t.setIntType(2);
        t.setBooleanType(true);
        t.setBooleanObjType(Boolean.TRUE);
        t.setIntObjType(new Integer(4));
        t.setFloatObjType(new Float(2.2f));
        t.setDoubleObjType(new Double(2.3d));
        t.setDateObjType(new Date(7777777777l));
        t.setStringObjType("A String");

        String expected = "<object class=\"org.flymine.model.testmodel.Types\" implements=\"org.flymine.model.FlyMineBusinessObject\">"
            + "<field name=\"intObjType\" value=\"4\"/>"
            + "<field name=\"booleanObjType\" value=\"true\"/>"
            + "<field name=\"doubleType\" value=\"1.3\"/>"
            + "<field name=\"floatType\" value=\"1.2\"/>"
            + "<field name=\"floatObjType\" value=\"2.2\"/>"
            + "<field name=\"booleanType\" value=\"true\"/>"
            + "<field name=\"stringObjType\" value=\"A String\"/>"
            + "<field name=\"doubleObjType\" value=\"2.3\"/>"
            + "<field name=\"intType\" value=\"2\"/>"
            + "<field name=\"name\" value=\"Types1\"/>"
            + "<field name=\"id\" value=\"1234\"/>"
            + "<field name=\"dateObjType\" value=\"7777777777\"/>"
            + "</object>";

        assertEquals(expected, FullRenderer.render(t));
    }
}
