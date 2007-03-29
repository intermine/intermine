package org.intermine.web.logic.bag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.bag.BagQuery;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryHelper;

public class BagQueryHelperTest extends TestCase {
	
	Model model;
	Map classKeys;
    BagQueryConfig bagQueryConfig = 
        new BagQueryConfig(new HashMap());
    BagQueryConfig bagQueryConfigWithExtraConstraint = 
        new BagQueryConfig(new HashMap());
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
        
		BagQuery bq = BagQueryHelper.createDefaultBagQuery(model.getPackageName() + ".Employee",
                                                           bagQueryConfig, model, classKeys, input);
		String expected = "SELECT DISTINCT a1_.id AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (LOWER(a1_.name) IN ?) 1: [employeea2, employeea1]";
		assertEquals(expected, bq.getQuery(input, "DepartmentB1").toString());		
	}
    
    public void testCreateDefaultBagQueryWithExtra() throws Exception {
        Set input = new HashSet(Arrays.asList(new Object[] {"EmployeeA1", "EmployeeA2"}));
        
        BagQuery bq = BagQueryHelper.createDefaultBagQuery(model.getPackageName() + ".Employee",
                                                           bagQueryConfigWithExtraConstraint, 
                                                           model, classKeys, input);
        String expected = "SELECT DISTINCT a1_.id AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a4_ WHERE ((LOWER(a1_.name) IN ?) AND a1_.department CONTAINS a4_ AND a4_.name = \'DepartmentB1\') 1: [employeea2, employeea1]";
        assertEquals(expected, bq.getQuery(input, "DepartmentB1").toString());      
    }

	public void testCreateDefaultBagQueryMultiple() throws Exception {
		Set input = new HashSet(Arrays.asList(new Object[] {"EmployeeA1", "EmployeeB1"}));
		BagQuery bq = BagQueryHelper.createDefaultBagQuery(model.getPackageName() + ".Manager",
                                                           bagQueryConfig, model, classKeys, input);
		String expected = "SELECT DISTINCT a1_.id AS a2_, a1_.title AS a3_, a1_.name AS a4_ FROM org.intermine.model.testmodel.Manager AS a1_ WHERE (LOWER(a1_.title) IN ? OR LOWER(a1_.name) IN ?) 1: [employeeb1, employeea1] 2: [employeeb1, employeea1]";
		assertEquals(expected, bq.getQuery(input, "DepartmentB1").toString());		
	}
}
