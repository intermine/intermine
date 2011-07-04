package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.MockServletContext;

public class WebConfigTest extends TestCase
{

	MockServletContext context = new MockServletContext();

    public WebConfigTest(String arg) {
        super(arg);

        Properties p = new Properties();
        p.setProperty("web.config.classname.mappings", "CLASS_NAME_MAPPINGS");
        p.setProperty("web.config.fieldname.mappings", "FIELD_NAME_MAPPINGS");
        SessionMethods.setWebProperties(context, p);

        InputStream is = getClass().getClassLoader().getResourceAsStream("WebConfigTest.xml");
        InputStream classesIS = getClass().getClassLoader().getResourceAsStream("testClassMappings.properties");
        InputStream fieldsIS = getClass().getClassLoader().getResourceAsStream("testFieldMappings.properties");
        context.addInputStream("/WEB-INF/webconfig-model.xml", is);
        context.addInputStream("/WEB-INF/CLASS_NAME_MAPPINGS", classesIS);
        context.addInputStream("/WEB-INF/FIELD_NAME_MAPPINGS", fieldsIS);
    }

    public void testParse() throws Exception{
        WebConfig wc1 = WebConfig.parse(context, Model.getInstanceByName("testmodel"));

        Displayer employeeDisplayer = new Displayer();
        employeeDisplayer.setSrc("/model/employee.jsp");

        Type employeeType = new Type();
        employeeType.setClassName("org.intermine.model.testmodel.Employee");
        employeeType.addLongDisplayer(employeeDisplayer);
        employeeType.setLabel("Angestellter");
        FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        df1.setShowInInlineCollection(true);
        df1.setShowInResults(true);
        df1.setShowInSummary(true);
        employeeType.addFieldConfig(df1);
        FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("department.name");
        df2.setShowInInlineCollection(true);
        df2.setShowInResults(true);
        df2.setShowInSummary(true);
        employeeType.addFieldConfig(df2);

        Displayer managerDisplayer = new Displayer();
        managerDisplayer.setSrc("/model/manager.jsp");
        managerDisplayer.setAspects("Aspect1, Aspect2");
        Displayer tdisp = new Displayer();
        tdisp.setSrc("/model/tableManager.jsp");

        Type managerType = new Type();
        managerType.setClassName("org.intermine.model.testmodel.Manager");
        managerType.addLongDisplayer(managerDisplayer);
        managerType.setTableDisplayer(tdisp);

        FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("name");
        df3.setShowInInlineCollection(true);
        df3.setShowInResults(true);
        df3.setShowInSummary(true);
        managerType.addFieldConfig(df3);
        FieldConfig df4 = new FieldConfig();
        df4.setFieldExpr("seniority");
        df4.setShowInInlineCollection(true);
        df4.setShowInResults(true);
        df4.setShowInSummary(true);
        managerType.addFieldConfig(df4);
        FieldConfig df5 = new FieldConfig();
        df5.setFieldExpr("title");
        df5.setDoNotTruncate(true);
        managerType.addFieldConfig(df5);

        Displayer disp2 = new Displayer();
        disp2.setSrc("/model/page4.jsp");
        Displayer disp3 = new Displayer();
        disp3.setSrc("tile2.tile");

        Type thingType = new Type();
        thingType.setClassName("org.intermine.model.testmodel.Thing");
        thingType.addLongDisplayer(disp2);
        thingType.addLongDisplayer(disp3);

        Type contractorType = new Type();
        contractorType.setClassName("org.intermine.model.testmodel.Contractor");
        FieldConfig oldComs = new FieldConfig();
        oldComs.setFieldExpr("oldComs");
        oldComs.setLabel("Companies they used to work for");
        oldComs.setShowInInlineCollection(false);
        oldComs.setShowInResults(false);
        oldComs.setShowInSummary(false);
        contractorType.addFieldConfig(oldComs);

        TableExportConfig tableExportConfig = new TableExportConfig();
        tableExportConfig.setId("myExporter");
        tableExportConfig.setClassName("java.lang.String");

        Type companyType = new Type();
        companyType.setLabel("Firma");
        companyType.setClassName("org.intermine.model.testmodel.Company");
        FieldConfig vatNo = new FieldConfig();
        vatNo.setFieldExpr("vatNumber");
        vatNo.setLabel("VAT Number");
        vatNo.setShowInInlineCollection(false);
        vatNo.setShowInResults(false);
        vatNo.setShowInSummary(false);
        companyType.addFieldConfig(vatNo);

        Type secretaryType = new Type();
        secretaryType.setLabel("Personal Assistant");
        secretaryType.setClassName("org.intermine.model.testmodel.Secretary");

        Type simpleType = new Type();
        simpleType.setClassName("org.intermine.model.testmodel.SimpleObject");

        WebConfig wc2 = new WebConfig();
        wc2.addType(employeeType);
        wc2.addType(managerType);
        wc2.addType(thingType);
        wc2.addType(companyType);
        wc2.addType(secretaryType);
        wc2.addType(contractorType);
        wc2.addType(simpleType);
        wc2.addTableExportConfig(tableExportConfig);
        wc2.setSubClassConfig(Model.getInstanceByName("testmodel"));

        HashMap displayerAspects = new HashMap();
        displayerAspects.put("Aspect1", Arrays.asList(
                new Object[]{managerType.getLongDisplayers().iterator().next()}));
        displayerAspects.put("Aspect2", Arrays.asList(
                new Object[]{managerType.getLongDisplayers().iterator().next()}));
        assertEquals(displayerAspects, (wc1.getTypes().get("org.intermine.model.testmodel.Manager"))
                .getAspectDisplayers());

        assertEquals(wc2.toString(), wc1.toString());

    }

    public void testParseNull() throws Exception{
        try {
            WebConfig.parse(null, Model.getInstanceByName("testmodel"));
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

}
