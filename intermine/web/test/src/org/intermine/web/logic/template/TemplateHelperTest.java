package org.intermine.web.logic.template;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.junit.Before;
import org.junit.Test;

public class TemplateHelperTest {

    private TemplateQuery template;

    private static final String TEMPLATE_NAME = "TEST_TEMPLATE";
    private static final String TEMPLATE_TITLE = "TEMPLATE --> TESTING";
    private static final String COMMENT = "A TEMPLATE THAT WE ARE USING FOR TESTING";

    private String switchedOffCode;

    @Before
    public void setup() {
        Model model = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(model);

        template = new TemplateQuery(TEMPLATE_NAME, TEMPLATE_TITLE, COMMENT, pq);
        template.addViews("Employee.name", "Employee.age", "Employee.end", "Employee.department.manager.name");
        template.addConstraint(Constraints.eq("Employee.end", "10"));
        String ageCode = template.addConstraint(Constraints.eq("Employee.age", "10"));
        template.setEditable(template.getConstraintForCode(ageCode), true);
        String code = template.addConstraint(Constraints.eq("Employee.department.name", "Sales"));
        template.setEditable(template.getConstraintForCode(code), true);
        switchedOffCode = template.addConstraint(Constraints.eq("Employee.department.manager.name", "B*"));
        template.setEditable(template.getConstraintForCode(switchedOffCode), true);
        template.setSwitchOffAbility(template.getConstraintForCode(switchedOffCode), SwitchOffAbility.OFF);
    }

    @Test
    public void directAttributeRemoval() {
        TemplateQuery adjusted = TemplateHelper.removeDirectAttributesFromView(template);

        assertEquals(adjusted.getView(), Arrays.asList("Employee.age", "Employee.department.manager.name"));
    }

}
