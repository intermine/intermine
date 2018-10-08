package org.intermine.template;

import static org.junit.Assert.*;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.junit.Before;
import org.junit.Test;

public class TemplateQueryTest {

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
        template.addViews("Employee.name", "Employee.department.manager.name");
        String code = template.addConstraint(Constraints.eq("Employee.department.name", "Sales"));
        template.setEditable(template.getConstraintForCode(code), true);
        switchedOffCode = template.addConstraint(Constraints.eq("Employee.department.manager.name", "B*"));
        template.setEditable(template.getConstraintForCode(switchedOffCode), true);
        template.setSwitchOffAbility(template.getConstraintForCode(switchedOffCode), SwitchOffAbility.OFF);
    }

    @Test
    public void cloning() {
        TemplateQuery clone = template.clone();
        assertNotSame(clone, template);
        assertEquals(clone.getQueryToExecute(), template.getQueryToExecute());

        clone.setSwitchOffAbility(clone.getConstraintForCode(switchedOffCode), SwitchOffAbility.ON);
        assertTrue(!clone.getQueryToExecute().equals(template.getQueryToExecute()));

        assertEquals(clone.getView(), template.getView());
        clone.addViews("Employee.end");
        assertTrue(!clone.getView().equals(template.getView()));
    }



}

