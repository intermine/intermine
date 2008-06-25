package org.intermine.web.logic.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.PathQueryUtil;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.ClassKeyHelper;

/*
 * Copyright (C) 2002-2008 FlyMine
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
        assertTrue(query.getProblems().length == 1);
    }
    
    public void testInvalidSortOrder() {
        PathQuery query = createQuery("InvalidSortOrder.xml");
        assertTrue(query.getProblems().length == 0);
    }

    public void testInvalidConstraintLogic() {
        PathQuery query = createQuery("InvalidConstraintLogic.xml");    
        assertEquals(0, query.getProblems().length);
    }

    public void testIncompleteConstraintLogic() {
        try {
            createQuery("IncompleteConstraintLogic.xml");    
        } catch (Exception ex)  {
            return;
        }
        fail("Exception expected, but wasn't thrown.");
    }

    public void testInvalidNodeType() {
        PathQuery query = createQuery("InvalidNodeType.xml");
        assertEquals(0, query.getProblems().length);
        //System.out.println(PathQueryUtil.getProblemsSummary(query.getProblems()));        
    }

    public void testInvalidNodePath() {
        PathQuery query = createQuery("InvalidNodePath.xml");
        assertEquals(2, query.getProblems().length);
        //System.out.println(PathQueryUtil.getProblemsSummary(query.getProblems()));        
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
        assertEquals(0, query.getProblems().length);
        System.out.println(PathQueryUtil.getProblemsSummary(query.getProblems()));        
    }

    private PathQuery createQuery(String fileName)  {
        String path = "PathQueryBindingUnmarshal/" + fileName;
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        Model model = Model.getInstanceByName("testmodel");
        Properties classKeyProps = new Properties();
            try {
                classKeyProps.load(getClass().getClassLoader()
                                   .getResourceAsStream("class_keys.properties"));
            } catch (IOException e) {
                throw new RuntimeException("Exception occured", e);
            }
        Map<String, List<FieldDescriptor>> classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
        PathQuery ret = PathQueryBinding.unmarshal(new InputStreamReader(is),
                classKeys).values().iterator().next();
        MainHelper.checkPathQuery(ret, new HashMap());
        return ret;
    }    
}
