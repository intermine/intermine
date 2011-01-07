package org.intermine.pathquery;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

import org.intermine.metadata.Model;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests reaction of unmarshalling implementation at errors in query. The tests are written in the way, 
 * that they pass for current implementation of unmarshalling, but the behavior 
 * of tests and implementation of unmarshalling should be changed.
 * @author Jakub Kulaviak
 **/
public class PathQueryUnmarshalTest extends  TestCase
{
    
    public void testUnknownModel() {
        /*
         * Just now throws exception. It will change later.
         */
        try {
            createQuery("UnknownModel.xml");    
        } catch (Exception ex) {
            return;
        }
        fail("Expected exception");
    }

    public void testInvalidView() {
        PathQuery query = createQuery("InvalidView.xml");
        assertTrue(query.verifyQuery().size() == 1);
    }
    
    public void testInvalidSortOrder() {
        PathQuery query = createQuery("InvalidSortOrder.xml");
        assertTrue(query.verifyQuery().size() == 0);
    }

    public void testInvalidConstraintLogic() {
        PathQuery query = createQuery("InvalidConstraintLogic.xml");    
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
    }

    public void testIncompleteConstraintLogic() {
        PathQuery query = createQuery("IncompleteConstraintLogic.xml");    
        assertEquals(Arrays.asList("Value in constraint Employee.age > bad is not in correct format for type of Integer"), query.verifyQuery());
    }

    /* ? */
    public void testInvalidConstraintIdentifier() {
    }

    /* ? */
    public void testInvalidConstraintEditable() {
    }

    public void testInvalidConstraintOperation() {
        PathQuery query = null;
        try {
            query = createQuery("InvalidConstraintOperation.xml");    
        } catch (Exception ex) {
            return;
        }
        fail("Exception expected, but wasn't thrown.");
    }

    public void testInvalidConstraintValue() {
        PathQuery query = createQuery("InvalidConstraintValue.xml");
        System.out.println(query.verifyQuery());
        assertEquals(Collections.EMPTY_LIST, query.verifyQuery());
    }

    private PathQuery createQuery(String fileName)  {
        String path = "PathQueryBindingUnmarshal/" + fileName;
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        Model model = Model.getInstanceByName("testmodel");
        PathQuery ret = PathQueryBinding.unmarshal(new InputStreamReader(is), 1).values().iterator().next();
        return ret;
    }    
}
