package org.intermine.web.bag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.web.ClassKeyHelper;

public class BagQueryHelperTest extends TestCase {
	
	Model model;
	Map classKeys;
	
	public BagQueryHelperTest(String arg0) {
		super(arg0);
	}
	
    protected void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));
        classKeys = ClassKeyHelper.readKeys(model, props);
    }

	public void testCreateDefaultBagQuerySingle() throws Exception {
		Set input = new HashSet(Arrays.asList(new Object[] {"EmployeeA1", "EmployeeA2"}));
		BagQuery bq = BagQueryHelper.createDefaultBagQuery("Employee", classKeys, model, input);
		String expected = "SELECT DISTINCT a1_.id AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.name IN ?) 1: [EmployeeA2, EmployeeA1]";
		assertEquals(expected, bq.getQuery(input).toString());		
	}

	public void testCreateDefaultBagQueryMultiple() throws Exception {
		Set input = new HashSet(Arrays.asList(new Object[] {"EmployeeA1", "EmployeeB1"}));
		BagQuery bq = BagQueryHelper.createDefaultBagQuery("Manager", classKeys, model, input);
		String expected = "SELECT DISTINCT a1_.id AS a2_, a1_.title AS a3_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Manager AS a1_ WHERE (a1_.title IN ? OR a1_.name IN ?) 1: [EmployeeB1, EmployeeA1] 2: [EmployeeB1, EmployeeA1]";
		assertEquals(expected, bq.getQuery(input).toString());		
	}
}
