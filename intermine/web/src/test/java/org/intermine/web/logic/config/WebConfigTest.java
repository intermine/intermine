package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.Properties;

import org.custommonkey.xmlunit.XMLTestCase;
import org.intermine.metadata.Model;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.MockServletContext;

public class WebConfigTest extends XMLTestCase
{

	MockServletContext context = new MockServletContext();

    public WebConfigTest(final String arg) {
        super(arg);

        final Properties p = new Properties();
        p.setProperty("web.config.classname.mappings", "CLASS_NAME_MAPPINGS");
        p.setProperty("web.config.fieldname.mappings", "FIELD_NAME_MAPPINGS");
        SessionMethods.setWebProperties(context, p);

        final InputStream is = getClass().getClassLoader().getResourceAsStream("WebConfigTest.xml");
        final InputStream classesIS = getClass().getClassLoader().getResourceAsStream("testClassMappings.properties");
        final InputStream fieldsIS = getClass().getClassLoader().getResourceAsStream("testFieldMappings.properties");
        context.addInputStream("/WEB-INF/webconfig-model.xml", is);
        context.addInputStream("/WEB-INF/CLASS_NAME_MAPPINGS", classesIS);
        context.addInputStream("/WEB-INF/FIELD_NAME_MAPPINGS", fieldsIS);
    }

    public void testParse() throws Exception{
        final WebConfig wc1 = WebConfig.parse(context, Model.getInstanceByName("testmodel"));

        final Type employeeType = new Type();
        employeeType.setClassName("org.intermine.model.testmodel.Employee");
        employeeType.setLabel("Angestellter");
        final FieldConfig df1 = new FieldConfig();
        df1.setFieldExpr("name");
        df1.setShowInInlineCollection(true);
        df1.setShowInResults(true);
        df1.setShowInSummary(true);
        employeeType.addFieldConfig(df1);
        final FieldConfig df2 = new FieldConfig();
        df2.setFieldExpr("department.name");
        df2.setLabel("Abteilung");
        df2.setShowInInlineCollection(true);
        df2.setShowInResults(true);
        df2.setShowInSummary(true);
        employeeType.addFieldConfig(df2);
        final FieldConfig age = new FieldConfig();
        age.setFieldExpr("age");
        age.setLabel("Years Alive");
        age.setShowInSummary(false);
        age.setShowInResults(false);
        age.setShowInInlineCollection(false);
        employeeType.addFieldConfig(age);

        final Type managerType = new Type();
        managerType.setClassName("org.intermine.model.testmodel.Manager");
        final Displayer managerTableDisplayer = new Displayer();
        managerTableDisplayer.setSrc("/model/tableManager.jsp");
        managerType.setTableDisplayer(managerTableDisplayer);

        final FieldConfig df3 = new FieldConfig();
        df3.setFieldExpr("name");
        df3.setShowInInlineCollection(true);
        df3.setShowInResults(true);
        df3.setShowInSummary(true);
        managerType.addFieldConfig(df3);
        final FieldConfig df4 = new FieldConfig();
        df4.setFieldExpr("seniority");
        df4.setShowInInlineCollection(true);
        df4.setShowInResults(true);
        df4.setShowInSummary(true);
        managerType.addFieldConfig(df4);
        final FieldConfig df5 = new FieldConfig();
        df5.setFieldExpr("title");
        df5.setDoNotTruncate(true);
        managerType.addFieldConfig(df5);
        managerType.addFieldConfig(age);

        final Type contractorType = new Type();
        contractorType.setClassName("org.intermine.model.testmodel.Contractor");
        final FieldConfig oldComs = new FieldConfig();
        oldComs.setFieldExpr("oldComs");
        oldComs.setLabel("Companies they used to work for");
        oldComs.setShowInInlineCollection(false);
        oldComs.setShowInResults(false);
        oldComs.setShowInSummary(false);
        contractorType.addFieldConfig(oldComs);

        final TableExportConfig tableExportConfig = new TableExportConfig();
        tableExportConfig.setId("myExporter");
        tableExportConfig.setClassName("java.lang.String");

        final Type companyType = new Type();
        companyType.setLabel("Firma");
        companyType.setClassName("org.intermine.model.testmodel.Company");
        final FieldConfig vatNo = new FieldConfig();
        vatNo.setFieldExpr("vatNumber");
        vatNo.setLabel("VAT Number");
        vatNo.setShowInInlineCollection(false);
        vatNo.setShowInResults(false);
        vatNo.setShowInSummary(false);
        companyType.addFieldConfig(vatNo);
        final FieldConfig deps = new FieldConfig();
        deps.setFieldExpr("departments");
        deps.setLabel("Abteilungen");
        deps.setShowInInlineCollection(false);
        deps.setShowInSummary(false);
        deps.setShowInResults(false);
        companyType.addFieldConfig(deps);

        final Type secretaryType = new Type();
        secretaryType.setLabel("Personal Assistant");
        secretaryType.setClassName("org.intermine.model.testmodel.Secretary");

        final Type simpleType = new Type();
        simpleType.setClassName("org.intermine.model.testmodel.SimpleObject");

        final Type departmentType = new Type();
        departmentType.setClassName("org.intermine.model.testmodel.Department");
        departmentType.setLabel("Abteilung");
        final FieldConfig emps = new FieldConfig();
        emps.setLabel("Angestellter");
        emps.setFieldExpr("employees");
        emps.setShowInInlineCollection(false);
        emps.setShowInResults(false);
        emps.setShowInSummary(false);
        departmentType.addFieldConfig(emps);

        final WebConfig wc2 = new WebConfig();
        wc2.addType(employeeType);
        wc2.addType(managerType);
        wc2.addType(companyType);
        wc2.addType(secretaryType);
        wc2.addType(contractorType);
        wc2.addType(simpleType);
        wc2.addType(departmentType);
        wc2.addTableExportConfig(tableExportConfig);

        wc2.validate(Model.getInstanceByName("testmodel"));
        wc2.setSubClassConfig(Model.getInstanceByName("testmodel"));

        /*
        HashMap displayerAspects = new HashMap();
        displayerAspects.put("Aspect1", Arrays.asList(
                new Object[]{managerType.getLongDisplayers().iterator().next()}));
        displayerAspects.put("Aspect2", Arrays.asList(
                new Object[]{managerType.getLongDisplayers().iterator().next()}));
        assertEquals(displayerAspects, (wc1.getTypes().get("org.intermine.model.testmodel.Manager"))
                .getAspectDisplayers());
        */
        
        
        assertXMLEqual(wc2.toString(), wc1.toString());

    }

    public void testParseNull() throws Exception{
        try {
            WebConfig.parse(null, Model.getInstanceByName("testmodel"));
            fail("Expected: NullPointerException");
        } catch (final NullPointerException e) {
        }

    }

}
