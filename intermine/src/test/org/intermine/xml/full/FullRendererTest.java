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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.flymine.util.TypeUtil;
import org.flymine.util.DynamicUtil;
import org.flymine.model.testmodel.*;
import org.flymine.metadata.Model;

public class FullRendererTest extends TestCase
{
    private Model model;
    private final String ENDL = System.getProperty("line.separator");

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testRender() throws Exception {
        Department d1 = new Department();
        d1.setId(new Integer(5678));
        Department d2 = new Department();
        d2.setId(new Integer(6789));

        List list = Arrays.asList(new Object[] {d1, d2});

        String expected = "<items>" + ENDL
            + "<object class=\"http://www.flymine.org/model/testmodel#Department\" implements=\"http://www.flymine.org/model/testmodel#RandomInterface\">" + ENDL
            + "<field name=\"id\" value=\"5678\"/>" + ENDL
            + "</object>" + ENDL
            + "<object class=\"http://www.flymine.org/model/testmodel#Department\" implements=\"http://www.flymine.org/model/testmodel#RandomInterface\">" + ENDL
            + "<field name=\"id\" value=\"6789\"/>" + ENDL
            + "</object>" + ENDL
            + "</items>" + ENDL;

        assertEquals(expected, FullRenderer.render(list, model));
    }

    public void testRenderObjectMaterial() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = "<object class=\"http://www.flymine.org/model/testmodel#Employee\" implements=\"http://www.flymine.org/model/testmodel#Employable http://www.flymine.org/model/testmodel#HasAddress\">" + ENDL
            + "<field name=\"age\" value=\"0\"/>" + ENDL
            + "<field name=\"fullTime\" value=\"false\"/>" + ENDL
            + "<field name=\"name\" value=\"Employee1\"/>" + ENDL
            + "<field name=\"id\" value=\"1234\"/>" + ENDL
            + "<reference name=\"department\" ref_id=\"5678\"/>" + ENDL
            + "</object>" + ENDL;

        assertEquals(expected, FullRenderer.renderObject(e, model));
    }

    public void testRenderObjectDynamic() throws Exception {
        Department d1 = new Department();
        d1.setId(new Integer(5678));
        Department d2 = new Department();
        d2.setId(new Integer(6789));

        Object o = DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Company.class, Broke.class})));
        Company c = (Company) o;
        c.setId(new Integer(1234));
        c.setName("BrokeCompany1");
        c.setDepartments(Arrays.asList(new Object[] {d1, d2}));

        Broke b = (Broke) o;
        b.setDebt(10);

        String expected = "<object class=\"\" implements=\"http://www.flymine.org/model/testmodel#Broke http://www.flymine.org/model/testmodel#Company\">" + ENDL
            + "<field name=\"vatNumber\" value=\"0\"/>" + ENDL
            + "<field name=\"debt\" value=\"10\"/>" + ENDL
            + "<collection name=\"departments\">" + ENDL
            + "<reference ref_id=\"5678\"/>" + ENDL
            + "<reference ref_id=\"6789\"/>" + ENDL
            + "</collection>" + ENDL
            + "<field name=\"name\" value=\"BrokeCompany1\"/>" + ENDL
            + "<field name=\"id\" value=\"1234\"/>" + ENDL
            + "</object>" + ENDL;

        assertEquals(expected, FullRenderer.renderObject(b, model));
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

        String expected = "<object class=\"http://www.flymine.org/model/testmodel#Types\" implements=\"http://www.flymine.org/model/testmodel#FlyMineBusinessObject\">" + ENDL
            + "<field name=\"intObjType\" value=\"4\"/>" + ENDL
            + "<field name=\"booleanObjType\" value=\"true\"/>" + ENDL
            + "<field name=\"doubleType\" value=\"1.3\"/>" + ENDL
            + "<field name=\"floatType\" value=\"1.2\"/>" + ENDL
            + "<field name=\"floatObjType\" value=\"2.2\"/>" + ENDL
            + "<field name=\"booleanType\" value=\"true\"/>" + ENDL
            + "<field name=\"stringObjType\" value=\"A String\"/>" + ENDL
            + "<field name=\"doubleObjType\" value=\"2.3\"/>" + ENDL
            + "<field name=\"intType\" value=\"2\"/>" + ENDL
            + "<field name=\"name\" value=\"Types1\"/>" + ENDL
            + "<field name=\"id\" value=\"1234\"/>" + ENDL
            + "<field name=\"dateObjType\" value=\"7777777777\"/>" + ENDL
            + "</object>" + ENDL;

        assertEquals(expected, FullRenderer.renderObject(t, model));
    }
}
