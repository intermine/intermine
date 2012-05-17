package org.intermine.web.logic.widget;

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
import java.util.Properties;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.MockServletContext;


/**
 * A TestCase that sets up a webconfig for use in TestCases that extend this class.  The
 * setUp() method creates a new webconfig from  WebConfigTest.xml.
 * @author dbutano
 *
 */
public class WidgetConfigTestCase extends InterMineAPITestCase {
    protected WebConfig webConfig;

    public WidgetConfigTestCase() {
        super(null);
    }

    public void setUp() throws Exception {
        super.setUp();
        MockServletContext context = new MockServletContext();
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
        webConfig = WebConfig.parse(context, os.getModel());
        webConfig.getClass();
    }
}
