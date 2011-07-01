package org.intermine.web.logic;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.MockServletContext;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class WebUtilTest extends TestCase {
	MockServletContext context = new MockServletContext();
	WebConfig config;
	Model model;

    public WebUtilTest(String arg) throws FileNotFoundException, IOException, SAXException, ClassNotFoundException {
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

        model = Model.getInstanceByName("testmodel");
        config = WebConfig.parse(context, model);
    }

    public void testFormatColumn() throws PathException {
    	Path p = new Path(model, "Employee.name");
    	String expected = "Angestellter > Name";
    	// Check class name labels
    	assertEquals(expected, WebUtil.formatColumnName(p, config));

    	p = new Path(model, "Contractor.oldComs.vatNumber");
    	// Check reference and attribute labels
    	expected = "Contractor > Companies they used to work for > VAT Number";
    	assertEquals(expected, WebUtil.formatColumnName(p, config));

    	p = new Path(model, "Contractor.personalAddress.address");
    	// Check default munging
    	expected = "Contractor > Personal Address > Address";
    	assertEquals(expected, WebUtil.formatColumnName(p, config));
    }

    public void testFormatPath() throws PathException {
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

}
