package org.intermine.web.logic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.MockServletContext;
import org.xml.sax.SAXException;

public class WebUtilTest extends TestCase {
    MockServletContext context = new MockServletContext();
    WebConfig config;
    Model model;

    public WebUtilTest(final String arg) throws FileNotFoundException,
            IOException, SAXException, ClassNotFoundException {
        super(arg);

        final Properties p = new Properties();
        p.setProperty("web.config.classname.mappings", "CLASS_NAME_MAPPINGS");
        p.setProperty("web.config.fieldname.mappings", "FIELD_NAME_MAPPINGS");
        SessionMethods.setWebProperties(context, p);

        final InputStream is = getClass().getClassLoader().getResourceAsStream(
                "WebConfigTest.xml");
        final InputStream classesIS = getClass().getClassLoader()
                .getResourceAsStream("testClassMappings.properties");
        final InputStream fieldsIS = getClass().getClassLoader()
                .getResourceAsStream("testFieldMappings.properties");
        context.addInputStream("/WEB-INF/webconfig-model.xml", is);
        context.addInputStream("/WEB-INF/CLASS_NAME_MAPPINGS", classesIS);
        context.addInputStream("/WEB-INF/FIELD_NAME_MAPPINGS", fieldsIS);

        model = Model.getInstanceByName("testmodel");
        config = WebConfig.parse(context, model);
    }

    public void testFormatPath() throws PathException {
        Path p = new Path(model, "Employee.name");
        String expected = "Angestellter > Name";
        // Check class name labels
        assertEquals(expected, WebUtil.formatPath(p, config));

        p = new Path(model, "Contractor.oldComs.vatNumber");
        // Check reference and attribute labels
        expected = "Contractor > Companies they used to work for > VAT Number";
        assertEquals(expected, WebUtil.formatPath(p, config));

        p = new Path(model, "Contractor.personalAddress.address");
        // Check default munging
        expected = "Contractor > Personal Address > Address";
        assertEquals(expected, WebUtil.formatPath(p, config));

        // Check path making
        expected = "Contractor > Companies they used to work for > VAT Number";
        assertEquals(expected, WebUtil.formatPath(
                "Contractor.oldComs.vatNumber", model, config));

        // Check path making, complete labelling
        expected = "Firma > Abteilungen > Angestellter";
        assertEquals(expected, WebUtil.formatPath(
                "Company.departments.employees", model, config));

        // Check composite attribute labelling
        p = new Path(model, "Employee.department.name");
        expected = "Angestellter > Abteilung";
        assertEquals(expected, WebUtil.formatPath(p, config));

        // Check composite attribute labelling, from a reference.
        p = new Path(model, "Manager.department.employees.department.name");
        expected = "Manager > Department > Angestellter > Abteilung";
        assertEquals(expected, WebUtil.formatPath(p, config));
    }

    public void testFormatField() throws PathException {
        Path p = new Path(model, "Employee.name");
        String expected = "Name";
        assertEquals(expected, WebUtil.formatField(p, config));

        p = new Path(model, "Contractor.oldComs.vatNumber");
        expected = "VAT Number";
        assertEquals(expected, WebUtil.formatField(p, config));

        p = new Path(model, "Contractor.personalAddress");
        expected = "Personal Address";
        assertEquals(expected, WebUtil.formatField(p, config));
    }

    public void testSubclassedPath() throws PathException {
        Path p;
        String expected;

        p = new Path(model, "Department.employees[Manager].seniority");
        expected = "Abteilung > Angestellter > Seniority";
        assertEquals(expected, WebUtil.formatPath(p, config));

        p = new Path(model, "Company.departments.employees[Manager].seniority");
        expected = "Firma > Abteilungen > Angestellter > Seniority";
        assertEquals(expected, WebUtil.formatPath(p, config));
    }

    public void testFormatPathDescription() {
        final PathQuery pq = new PathQuery(model);
        pq.setDescription("Employee.department.company", "COMPANY");
        pq.setDescription("Employee.department", "DEPARTMENT");
        pq.setDescription("Employee", "EMPLOYEE");

        assertEquals("EMPLOYEE",
                WebUtil.formatPathDescription("Employee", pq, config));

        assertEquals("EMPLOYEE > Years Alive",
                WebUtil.formatPathDescription("Employee.age", pq, config));

        assertEquals("EMPLOYEE > Address > Address",
                WebUtil.formatPathDescription("Employee.address.address", pq,
                        config));

        assertEquals("EMPLOYEE > Full Time",
                WebUtil.formatPathDescription("Employee.fullTime", pq, config));

        assertEquals("DEPARTMENT", WebUtil.formatPathDescription(
                "Employee.department", pq, config));
    }

    public void testCompositePathDescriptions() {
        final PathQuery pq = new PathQuery(model);
        pq.setDescription("Employee.department.company", "COMPANY");
        pq.setDescription("Employee.department", "DEPARTMENT");
        pq.setDescription("Employee.address", "RESIDENCE");
        pq.setDescription("Employee", "EMPLOYEE");

        assertEquals("RESIDENCE > Address", WebUtil.formatPathDescription(
                "Employee.address.address", pq, config));

        // Obeys existing composite rules for attributes.
        assertEquals("DEPARTMENT", WebUtil.formatPathDescription(
                "Employee.department.name", pq, config));

        assertEquals("DEPARTMENT > Manager > Name",
                WebUtil.formatPathDescription(
                        "Employee.department.manager.name", pq, config));

        assertEquals("COMPANY", WebUtil.formatPathDescription(
                "Employee.department.company", pq, config));

        assertEquals("COMPANY > Abteilungen > Manager > Years Alive",
                WebUtil.formatPathDescription(
                        "Employee.department.company.departments.manager.age",
                        pq, config));

        // Paths without any configuration are handled as per formatPath
        assertEquals("Contractor > Companies they used to work for > VAT Number",
                WebUtil.formatPathDescription("Contractor.oldComs.vatNumber", pq, config));
    }

    /**
     * Check that formatted pathquery views take both the pathdescriptions and the webconfig into account.
     */
    public void testFormatPathQueryView() {
        final PathQuery pq = new PathQuery(model);
        pq.setDescription("Employee.department.company", "COMPANY");
        pq.setDescription("Employee.department", "DEPARTMENT");
        pq.setDescription("Employee", "EMPLOYEE");
        pq.addViews("Employee.name", "Employee.fullTime", "Employee.department.name",
                "Employee.department.company.contractors.oldComs.vatNumber");

        final List<String> expected = Arrays.asList("EMPLOYEE > Name", "EMPLOYEE > Full Time",
                "DEPARTMENT", "COMPANY > Contractors > Companies they used to work for > VAT Number");
        assertEquals(expected, WebUtil.formatPathQueryView(pq, config));
    }

}
