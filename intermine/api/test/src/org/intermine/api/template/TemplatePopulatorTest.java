package org.intermine.api.template;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathError;
import org.intermine.pathquery.PathQuery;

public class TemplatePopulatorTest extends TestCase {
	
	
    private Map<String, TemplateQuery> templates;
    private TemplateQuery simpleTemplate;
    private TemplateQuery template;
    private Map<String, List<TemplateValue>> values = new HashMap();
    
    public void setUp() throws Exception {
        super.setUp();
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new InputStreamReader(TemplatePrecomputeHelperTest.class.getClassLoader().getResourceAsStream("default-template-queries.xml"));
        templates = binding.unmarshal(reader, new HashMap(), PathQuery.USERPROFILE_VERSION);
        simpleTemplate = templates.get("employeeByName");
        template = templates.get("employeesFromCompanyAndDepartment");
    }

    
    public void testInvalidPathInValues() throws Exception {
    	TemplateValue value = new TemplateValue("Employee.error", ConstraintOp.EQUALS, "error name", "A");
    	values.put("Employee.error", Arrays.asList(new TemplateValue[] {value}));
    	try {
    		TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
        	fail("Expected a PathError.");
    	} catch (PathError e) {
    	}
    }
    
    public void testNoValuesForNode() {
    	TemplateValue value = new TemplateValue("Employee.age", ConstraintOp.EQUALS, "21", "A");
    	values.put("Employee.age", Arrays.asList(new TemplateValue[] {value}));
    	try {
    		TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
        	fail("Expected a TemplatePopulationException.");
    	} catch (TemplatePopulatorException e) {
    	}
    }
    
    public void testTooManyValuesForNode() {
    	TemplateValue value1 = new TemplateValue("Employee.name", ConstraintOp.EQUALS, "name", "A");
    	TemplateValue value2 = new TemplateValue("Employee.name", ConstraintOp.EQUALS, "other name", "A");
    	values.put("Employee.name", Arrays.asList(new TemplateValue[] {value1, value2}));
    	try {
    		TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
        	fail("Expected a TemplatePopulationException.");
    	} catch (TemplatePopulatorException e) {
    	}
    }
 
    public void testTooFewValuesForNode() {
    	TemplateQuery employeesOfACertainAge = templates.get("employeesOfACertainAge");
    	TemplateValue value = new TemplateValue("Employee.age", ConstraintOp.EQUALS, "21", "A");
    	values.put("Employee.age", Arrays.asList(new TemplateValue[] {value}));
    	try {
    		TemplatePopulator.getPopulatedTemplate(employeesOfACertainAge, values);
        	fail("Expected a TemplatePopulationException.");
    	} catch (TemplatePopulatorException e) {
    	}
    }
    
    public void testWrongCode() {
    	TemplateValue value = new TemplateValue("Employee.name", ConstraintOp.EQUALS, "name", "Z");
    	values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
    	try {
    		TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
    		fail("Expected a TemplatePopulationException.");
    	} catch (TemplatePopulatorException e) {
    	}
    }
    
    public void testOneConstraint() {
    	TemplateValue value = new TemplateValue("Employee.name", ConstraintOp.NOT_EQUALS, "Kevin Bacon", "A");
    	values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
    	TemplateQuery res = TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
    	assertEquals(1, res.getAllEditableConstraints().size());
    	Constraint resCon = res.getAllEditableConstraints().get(0);
    	assertEquals(ConstraintOp.NOT_EQUALS, resCon.getOp());
    	assertEquals("Kevin Bacon", resCon.getValue());
    }
    
    public void testOneBagConstraint() {
    	TemplateValue value = new TemplateValue("Employee.name", ConstraintOp.IN, "bag1", "A");
    	value.setBagConstraint(true);
    	values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
    	TemplateQuery res = TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
    	assertEquals(1, res.getAllEditableConstraints().size());
    	// constraint should now be on parent node
    	assertEquals(1, res.getNodes().get("Employee").getConstraints().size());
    	assertEquals(0, res.getNodes().get("Employee.name").getConstraints().size());

    	Constraint resCon = res.getAllEditableConstraints().get(0);
    	assertEquals(ConstraintOp.IN, resCon.getOp());
    	assertEquals("bag1", resCon.getValue());
    }
    
//    public void testBagNotOneConstraint() {
//    	InterMineBag bag = 
//    	try {
//    		
//    	}
//    }
    
    
    public void testBagConstraintWrongOp() {
    	TemplateValue value = new TemplateValue("Employee.name", ConstraintOp.GREATER_THAN, "bag1", "A");
    	value.setBagConstraint(true);
    	values.put("Employee.name", Arrays.asList(new TemplateValue[] {value}));
    	try {
    		TemplatePopulator.getPopulatedTemplate(simpleTemplate, values);
    		fail("Expected an exception.");
    	} catch (TemplatePopulatorException e) {    		
    	}
    }
    
//    private class DummyBag extends InterMineBag
//    {
//    	public DummyBag(String name, String type, String description) {
//    		super();
//    		this.type = type;
//    		setName(name);
//    		setDescription(description);
//    	}
//    }
//    private InterMineBag createBag(String name, String type) throws ObjectStoreException {
//    	DummayBag bag = new DummyBag(name, type, "");
//    	return bag;
//    }
    
}
