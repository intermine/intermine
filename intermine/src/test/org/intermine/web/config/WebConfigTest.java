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

public class WebConfigTest extends TestCase
{

    public WebConfigTest(String arg) {
        super(arg);
    }

    public void testParse() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/WebConfigTest.xml");
        WebConfig wc1 = WebConfig.parse(is);

        Displayer disp1 = new Displayer();
        disp1.setSrc("/model/page2.jsp");
        Type type1 = new Type();
        type1.setClassName("Class1");
        type1.addLongDisplayer(disp1);
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("class1field1");
        type1.addFieldConfig(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("class1field2.field");
        type1.addFieldConfig(df2);

        Displayer disp2 = new Displayer();
        disp2.setSrc("/model/page4.jsp");
        Displayer disp3 = new Displayer();
        disp3.setSrc("tile2.tile");
        Type type2 = new Type();
        type2.setClassName("Class2");
        type2.addLongDisplayer(disp2);
        type2.addLongDisplayer(disp3);

        Exporter exporter = new Exporter();
        exporter.setId("myExporter");
        exporter.setActionPath("/somePath");
        exporter.setClassName("java.lang.String");

        WebConfig wc2 = new WebConfig();
        wc2.addType(type1);
        wc2.addType(type2);
        wc2.addExporter(exporter);

        assertEquals(wc2, wc1);
    }

    public void testParseNull() throws Exception{
        try {
            WebConfig.parse(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

}
