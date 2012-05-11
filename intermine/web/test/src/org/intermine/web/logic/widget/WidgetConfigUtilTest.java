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

import junit.framework.TestCase;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;
import org.intermine.web.struts.MockServletContext;

public class WidgetConfigUtilTest extends InterMineAPITestCase
{
    public WidgetConfigUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testIsListConstraint() {
        PathConstraint pc1 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "EmployeeA1");
        PathConstraint pc2 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[]");
        PathConstraint pc3 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[list]");
        PathConstraint pc4 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[LIST]");
        assertEquals(false, WidgetConfigUtil.isListConstraint(pc1));
        assertEquals(false, WidgetConfigUtil.isListConstraint(pc2));
        assertEquals(true, WidgetConfigUtil.isListConstraint(pc3));
        assertEquals(true, WidgetConfigUtil.isListConstraint(pc4));
    }

    public void testIsPathContainingSubClass() {
        String path1 = "Employee.department.manager";
        String path2 = "Employee.department.manager[CEO]";
        String path3 = "Employee.department.manager[ceo]";
        Model model = im.getModel();
        assertEquals(false, WidgetConfigUtil.isPathContainingSubClass(model, path1));
        assertEquals(true, WidgetConfigUtil.isPathContainingSubClass(model, path2));
        assertEquals(false, WidgetConfigUtil.isPathContainingSubClass(model, path3));
    }

    public void testIsFilterConstraint() throws Exception {
        WidgetConfig config = createWidgetConfig();
        PathConstraint pc1 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "EmployeeA1");
        PathConstraint pc2 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[filter]");
        PathConstraint pc3 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[testFilter]");
        PathConstraint pc4 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[TESTFilter]");
        assertEquals(false, WidgetConfigUtil.isFilterConstraint(config, pc1));
        assertEquals(false, WidgetConfigUtil.isFilterConstraint(config, pc2));
        assertEquals(true, WidgetConfigUtil.isFilterConstraint(config, pc3));
        assertEquals(true, WidgetConfigUtil.isFilterConstraint(config, pc4));
    }
    
    private WidgetConfig createWidgetConfig() throws Exception {
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
        WebConfig webConfig = WebConfig.parse(context, im.getModel());
        WidgetConfig widgetConfig = webConfig.getWidgets().get("contractor_enrichment");
        return widgetConfig;
    }
}
