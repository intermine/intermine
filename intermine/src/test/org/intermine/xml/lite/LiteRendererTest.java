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
        Field field = TypeUtil.getField(e.getClass(), "id");
        field.setAccessible(true);
        field.set(e, new Integer(1234));
        e.setName("Employee1");

        field = TypeUtil.getField(d.getClass(), "id");
        field.setAccessible(true);
        field.set(d, new Integer(5678));
        e.setDepartment(d);

        String expected = "<object id=\"1234\" implements=\"org.flymine.model.testmodel.Employee org.flymine.model.testmodel.Employable\">"
            + "<field name=\"fullTime\" value=\"false\"/>"
            + "<field name=\"name\" value=\"Employee1\"/>"
            // Take the following line out when OJB ditched
            + "<field name=\"ojbConcreteClass\" value=\"org.flymine.model.testmodel.Employee\"/>"
            + "<field name=\"department\" value=\"5678\"/>"
            + "<field name=\"age\" value=\"0\"/>"
            + "</object>";

        System.out.println(expected);
        System.out.println( LiteRenderer.render(e));

        assertEquals(expected, LiteRenderer.render(e));
    }


}
