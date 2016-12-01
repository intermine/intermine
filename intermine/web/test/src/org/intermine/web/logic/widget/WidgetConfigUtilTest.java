package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfigUtil;

public class WidgetConfigUtilTest extends WidgetConfigTestCase
{
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
        Model model = os.getModel();
        assertEquals(false, WidgetConfigUtil.isPathContainingSubClass(model, path1));
        assertEquals(true, WidgetConfigUtil.isPathContainingSubClass(model, path2));
        assertEquals(false, WidgetConfigUtil.isPathContainingSubClass(model, path3));
    }

    public void testIsFilterConstraint() throws Exception {
        WidgetConfig config = webConfig.getWidgets().get("contractor_enrichment_with_filter1");
        PathConstraint pc1 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "EmployeeA1");
        PathConstraint pc2 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[filter]");
        PathConstraint pc3 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[testFilter]");
        PathConstraint pc4 = new PathConstraintAttribute("Employee.name", ConstraintOp.EQUALS, "[TESTFilter]");
        assertEquals(false, WidgetConfigUtil.isFilterConstraint(config, pc1));
        assertEquals(false, WidgetConfigUtil.isFilterConstraint(config, pc2));
        assertEquals(true, WidgetConfigUtil.isFilterConstraint(config, pc3));
        assertEquals(true, WidgetConfigUtil.isFilterConstraint(config, pc4));
    }
}
