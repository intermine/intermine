package org.flymine.xml.lite;

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

import org.flymine.util.TypeUtil;
import org.flymine.model.testmodel.*;

public class LiteRendererTest extends TestCase
{
    public void testRender() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = "<object id=\"1234\" implements=\"org.flymine.model.testmodel.Employee org.flymine.model.testmodel.Employable org.flymine.model.testmodel.HasAddress\">"
            + "<field name=\"age\" value=\"0\"/>"
            + "<field name=\"fullTime\" value=\"false\"/>"
            + "<field name=\"name\" value=\"Employee1\"/>"
            + "<field name=\"id\" value=\"1234\"/>"
            + "<reference name=\"department\" value=\"5678\"/>"
            + "</object>";

        System.out.println(expected);
        System.out.println(LiteRenderer.render(e));

        assertEquals(expected, LiteRenderer.render(e));
    }


}
