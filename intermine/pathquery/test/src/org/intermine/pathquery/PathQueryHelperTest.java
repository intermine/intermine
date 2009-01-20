package org.intermine.pathquery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.intermine.metadata.Model;

import junit.framework.TestCase;

public class PathQueryHelperTest extends TestCase {

    private Model model;
    
    public PathQueryHelperTest(String arg) {
        super(arg);
    }
    
    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }
    
    // should take the first view element
    public void testSetDefaultSortOrderNormal() throws Exception {
        PathQuery pq = new PathQuery(model);
        pq.setView("Employee.department.name Employee.name");
        assertTrue(pq.getSortOrder().isEmpty());
        
        PathQueryHelper.setDefaultSortOrder(pq);
        List<String> expected = new ArrayList<String>(Collections.singleton("Employee.department.name asc"));
        assertEquals(expected, pq.getSortOrderStrings());
    }
    
    // first view element isn't valid
    public void testSetDefaultSortOrderOuter() throws Exception {
        PathQuery pq = new PathQuery(model);
        pq.setView("Employee:department.name Employee.name");
        assertTrue(pq.getSortOrder().isEmpty());
        
        PathQueryHelper.setDefaultSortOrder(pq);
        List<String> expected = new ArrayList<String>(Collections.singleton("Employee.name asc"));
        assertEquals(expected, pq.getSortOrderStrings());
    }
    
    // no valid view elements for sorting
    public void testSetDefaultSortOrderAllOuter() throws Exception {
        PathQuery pq = new PathQuery(model);
        pq.setView("Employee:department.name Employee:department.manager.name");
        
        PathQueryHelper.setDefaultSortOrder(pq);
        assertTrue(pq.getSortOrder().isEmpty());
    }
    
}
