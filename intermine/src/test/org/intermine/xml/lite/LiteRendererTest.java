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

import java.math.BigDecimal;
import java.util.Date;

import org.flymine.util.TypeUtil;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.testmodel.*;
import org.flymine.metadata.Model;

public class LiteRendererTest extends TestCase
{

    Model model;

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testRender() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = "org.flymine.model.testmodel.Employee" + LiteRenderer.DELIM
            + "org.flymine.model.testmodel.Employable org.flymine.model.testmodel.HasAddress"
            + LiteRenderer.DELIM + "aage" + LiteRenderer.DELIM + "0"
            + LiteRenderer.DELIM + "afullTime" + LiteRenderer.DELIM + "false"
            + LiteRenderer.DELIM + "aname" + LiteRenderer.DELIM + "Employee1"
            + LiteRenderer.DELIM + "aid" + LiteRenderer.DELIM + "1234"
            + LiteRenderer.DELIM + "rdepartment" + LiteRenderer.DELIM + "5678";

        assertEquals(expected, LiteRenderer.render(e, model));
    }


    public void testRenderXML() throws Exception {
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

        assertEquals(expected, LiteRenderer.renderXml(e, model));
    }


    public void testObjectToItem() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        Item exp = new Item();
        exp.setClassName("org.flymine.model.testmodel.Employee");
        exp.setImplementations("org.flymine.model.testmodel.Employable org.flymine.model.testmodel.HasAddress");
        Field f1 = new Field();
        f1.setName("age");
        f1.setValue("0");
        Field f2 = new Field();
        f2.setName("fullTime");
        f2.setValue("false");
        Field f3 = new Field();
        f3.setName("name");
        f3.setValue("Employee1");
        Field f4 = new Field();
        f4.setName("id");
        f4.setValue("1234");
        exp.addField(f1);
        exp.addField(f2);
        exp.addField(f3);
        exp.addField(f4);
        Field f5 = new Field();
        f5.setName("department");
        f5.setValue("5678");
        exp.addReference(f5);

        assertEquals(LiteRenderer.renderXml(exp), LiteRenderer.renderXml(LiteRenderer.objectToItem(e, model)));

    }


    public void testTypesRenderXml() throws Exception {
        Types t = new Types();
        t.setId(new Integer(1234));
        t.setName("Types1");
        t.setBooleanType(true);
        t.setFloatType(1.2f);
        t.setDoubleType(1.3d);
        t.setShortType((short) 231);
        t.setIntType(2);
        t.setLongType(327641237623423l);
        t.setBooleanObjType(Boolean.TRUE);
        t.setFloatObjType(new Float(2.2f));
        t.setDoubleObjType(new Double(2.3d));
        t.setShortObjType(new Short((short) 786));
        t.setIntObjType(new Integer(4));
        t.setLongObjType(new Long(876328471234l));
        t.setBigDecimalObjType(new BigDecimal("9872876349183274123432.876128716235487621432"));
        t.setDateObjType(new Date(7777777777l));
        t.setStringObjType("A String");

        String expected = "<object class=\"org.flymine.model.testmodel.Types\" implements=\"org.flymine.model.FlyMineBusinessObject\">"
            + "<field name=\"booleanObjType\" value=\"true\"/>"
            + "<field name=\"doubleType\" value=\"1.3\"/>"
            + "<field name=\"floatType\" value=\"1.2\"/>"
            + "<field name=\"longObjType\" value=\"876328471234\"/>"
            + "<field name=\"booleanType\" value=\"true\"/>"
            + "<field name=\"stringObjType\" value=\"A String\"/>"
            + "<field name=\"intType\" value=\"2\"/>"
            + "<field name=\"doubleObjType\" value=\"2.3\"/>"
            + "<field name=\"id\" value=\"1234\"/>"
            + "<field name=\"dateObjType\" value=\"7777777777\"/>"
            + "<field name=\"intObjType\" value=\"4\"/>"
            + "<field name=\"bigDecimalObjType\" value=\"9872876349183274123432.876128716235487621432\"/>"
            + "<field name=\"floatObjType\" value=\"2.2\"/>"
            + "<field name=\"longType\" value=\"327641237623423\"/>"
            + "<field name=\"shortObjType\" value=\"786\"/>"
            + "<field name=\"shortType\" value=\"231\"/>"
            + "<field name=\"name\" value=\"Types1\"/>"
            + "</object>";

        assertEquals(expected, LiteRenderer.renderXml(t, model));
    }
}
