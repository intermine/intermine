package org.intermine.web.logic.template;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.intermine.MockHttpRequest;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateHelper.TemplateValueParseException;
import org.junit.Before;
import org.junit.Test;

public class TemplateHelperTest {

    private TemplateQuery template;

    private static final String TEMPLATE_NAME = "TEST_TEMPLATE";
    private static final String TEMPLATE_TITLE = "TEMPLATE --> TESTING";
    private static final String COMMENT = "A TEMPLATE THAT WE ARE USING FOR TESTING";

    private String switchedOffCode;

    private Map<String, String[]> oneConstraint,
        severalConstraints,
        parametersWithTooManyConstraints,
        parametersWithLotsOfConstraints,
        incompleteParameters;

    private final Map<String, String[]> emptyHeaders = Collections.EMPTY_MAP;

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

        // These should all be ignored.
        Map<String, String[]> otherParams = new HashMap<String, String[]>();
        otherParams.put("name", new String[] {"some name"});
        otherParams.put("start", new String[] {"10"});
        otherParams.put("size", new String[] {"25"});

        oneConstraint = new HashMap<String, String[]>(otherParams);
        oneConstraint.put("constraint1", new String[] {"Employee.name"});
        oneConstraint.put("code1", new String[] {"A"});
        oneConstraint.put("op1", new String[] {"CONTAINS"});
        oneConstraint.put("value1", new String[] {"foo"});

        severalConstraints = new HashMap<String, String[]>();
        severalConstraints.putAll(oneConstraint);
        severalConstraints.put("constraint2", new String[] {"Employee.name"});
        severalConstraints.put("code1", new String[] {"B"});
        severalConstraints.put("op2", new String[] {"ne"});
        severalConstraints.put("value2", new String[] {"fool"});
        severalConstraints.put("constraint3", new String[] {"Employee.department.name"});
        severalConstraints.put("code1", new String[] {"C"});
        severalConstraints.put("op3", new String[] {"ONE OF"});
        severalConstraints.put("value3", new String[] {"Sales", "Marketing"});

        parametersWithLotsOfConstraints = new HashMap<String, String[]>(otherParams);
        for (int i = 1; i <= PathQuery.MAX_CONSTRAINTS; i++) {
            parametersWithLotsOfConstraints.put("constraint" + i, new String[] {"Employee.name"});
            parametersWithLotsOfConstraints.put("code1", new String[] {"A"});
            parametersWithLotsOfConstraints.put("op" + i, new String[] {"ONE OF"});
            parametersWithLotsOfConstraints.put("value" + i, new String[] {"Sales", "Marketing"});
        }

        parametersWithTooManyConstraints = new HashMap<String, String[]>(otherParams);
        for (int i = 1; i <= PathQuery.MAX_CONSTRAINTS + 1; i++) {
            parametersWithTooManyConstraints.put("constraint" + i, new String[] {"Employee.name"});
            parametersWithTooManyConstraints.put("code1", new String[] {"A"});
            parametersWithTooManyConstraints.put("op" + i, new String[] {"ONE OF"});
            parametersWithTooManyConstraints.put("value" + i, new String[] {"Sales", "Marketing"});
        }

        incompleteParameters = new HashMap<String, String[]>();
        incompleteParameters.putAll(severalConstraints);
        incompleteParameters.put("op9", new String[] {"eq"});
    }

    @Test
    public void directAttributeRemoval() {
        TemplateQuery adjusted = TemplateHelper.removeDirectAttributesFromView(template);

        assertEquals(adjusted.getView(), Arrays.asList("Employee.age", "Employee.department.manager.name"));
    }

    @Test
    public void parseSingleConstraintTemplateParameters() throws TemplateValueParseException {

        HttpServletRequest one = new MockHttpRequest("GET", emptyHeaders, oneConstraint);
        Map<String, List<ConstraintInput>> ret = TemplateHelper.parseConstraints(one);
        assertEquals("I expect just one pair from this map", 1, ret.size());
        assertEquals("The key should be the value of constraint1",
                Collections.singleton(oneConstraint.get("constraint1")[0]),
                ret.keySet());
        assertEquals("And the value should be a constraint input",
                new ConstraintInput("constraint1", "Employee.name", "A", ConstraintOp.CONTAINS, "foo", Arrays.asList("foo"), null),
                ret.get("Employee.name").get(0));
    }

    @Test
    public void parseMultipleConstraintTemplateParameters() throws TemplateValueParseException {
        HttpServletRequest many = new MockHttpRequest("GET", emptyHeaders, severalConstraints);
        Map<String, List<ConstraintInput>> ret = TemplateHelper.parseConstraints(many);
        assertEquals("I expect two pairs from this map", 2, ret.size());
        assertEquals("The key should be the two paths",
                new HashSet<String>(Arrays.asList("Employee.name", "Employee.department.name")),
                ret.keySet());
        assertEquals("And the value should be a constraint input - 0 - 0",
                new ConstraintInput("constraint1", "Employee.name", "A", ConstraintOp.CONTAINS, "foo", Arrays.asList("foo"), null),
                ret.get("Employee.name").get(0));
        assertEquals("And the value should be a constraint input - 0 - 1",
                new ConstraintInput("constraint2", "Employee.name", "B", ConstraintOp.NOT_EQUALS, "fool", Arrays.asList("fool"), null),
                ret.get("Employee.name").get(1));
        assertEquals("And the value should be a constraint input - 1 - 0",
                new ConstraintInput("constraint3", "Employee.department.name", "C", ConstraintOp.ONE_OF, "Sales", Arrays.asList("Sales", "Marketing"), null),
                ret.get("Employee.department.name").get(0));
    }

    @Test
    public void parseTooManyTemplateParameters() {
        HttpServletRequest toomany = new MockHttpRequest("GET", emptyHeaders, parametersWithTooManyConstraints);
        try {
            TemplateHelper.parseConstraints(toomany);
        } catch (TemplateValueParseException e) {
            assertTrue("The error message should be informative", e.getMessage().contains("Maximum number"));
        }
    }

    @Test
    public void parseMaximumNumberOfConstraints() throws TemplateValueParseException {
        HttpServletRequest lots = new MockHttpRequest("GET", emptyHeaders, parametersWithLotsOfConstraints);
        Map<String, List<ConstraintInput>> ret = TemplateHelper.parseConstraints(lots);
        assertEquals("There is only one pair", 1, ret.size());
        assertEquals("That pair has all the constraint inputs", PathQuery.MAX_CONSTRAINTS, ret.get("Employee.name").size());
    }

    @Test
    public void parseIncompleteParameters() {
        HttpServletRequest missingparts = new MockHttpRequest("GET", emptyHeaders, incompleteParameters);
        try {
            TemplateHelper.parseConstraints(missingparts);
        } catch (TemplateValueParseException e) {
            assertTrue("The error message should be informative", e.getMessage().contains("no path was provided"));
        }
    }

}
