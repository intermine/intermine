package org.intermine.web.config;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.InputStream;

import org.intermine.metadata.Model;

public class WebConfigTest extends TestCase
{

    public WebConfigTest(String arg) {
        super(arg);
    }

    public void testParse() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/WebConfigTest.xml");
        WebConfig wc1 = WebConfig.parse(is, Model.getInstanceByName("testmodel"));

        Displayer employeeDisplayer = new Displayer();
        employeeDisplayer.setSrc("/model/employee.jsp");

        Type employableType = new Type();
        employableType.setClassName("org.intermine.model.testmodel.Employable");
        employableType.addLongDisplayer(employeeDisplayer);
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("class1field1");
        employableType.addFieldConfig(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("class1field2.field");
        employableType.addFieldConfig(df2);

        Displayer managerDisplayer = new Displayer();
        managerDisplayer.setSrc("/model/manager.jsp");

        Type managerType = new Type();
        managerType.setClassName("org.intermine.model.testmodel.Manager");
        managerType.addLongDisplayer(managerDisplayer);

        FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("name");
        managerType.addFieldConfig(df3);
        FieldConfig df4 = new FieldConfig();
        df4.setFieldExpr("seniority");
        managerType.addFieldConfig(df4);

        Displayer disp2 = new Displayer();
        disp2.setSrc("/model/page4.jsp");
        Displayer disp3 = new Displayer();
        disp3.setSrc("tile2.tile");

        Type thingType = new Type();
        thingType.setClassName("org.intermine.model.testmodel.Thing");
        thingType.addLongDisplayer(disp2);
        thingType.addLongDisplayer(disp3);

        Exporter exporter = new Exporter();
        exporter.setId("myExporter");
        exporter.setActionPath("/somePath");
        exporter.setClassName("java.lang.String");

        WebConfig wc2 = new WebConfig();
        wc2.addType(employableType);
        wc2.addType(managerType);
        wc2.addType(thingType);
        wc2.addExporter(exporter);
        wc2.setSubClassConfig(Model.getInstanceByName("testmodel"));

        assertEquals(1, managerType.getLongDisplayers().size());
        assertEquals("/model/manager.jsp",
                     ((Displayer) managerType.getLongDisplayers().iterator().next()).getSrc());

        assertEquals(wc2, wc1);

    }

    public void testParseNull() throws Exception{
        try {
            WebConfig.parse(null, Model.getInstanceByName("testmodel"));
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

}
