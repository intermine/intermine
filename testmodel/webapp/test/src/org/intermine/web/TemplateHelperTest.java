package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Reader;
import java.io.InputStreamReader;

import org.intermine.objectstore.query.Query;

import junit.framework.TestCase;

public class TemplateHelperTest extends TestCase
{
    Map templates;

    public void setUp() {
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new InputStreamReader(TemplateHelper.class.getClassLoader().getResourceAsStream("WEB-INF/classes/default-template-queries.xml"));
        templates = binding.unmarshal(reader);
    }

    public void testPrecomputeQuery() throws Exception {
        Iterator i = templates.keySet().iterator();
        while (i.hasNext()) {
            System.out.println("template: " + (String) i.next());
        }
        TemplateQuery t = (TemplateQuery) templates.get("employeeByName");
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee Employee.name\"><node path=\"Employee\" type=\"Employee\"></node></query>";
        Map pathToQueryNode = new HashMap();
        Query query = MainHelper.makeQuery(PathQuery.fromXml(queryXml), new HashMap(), pathToQueryNode);
        List indexes = new ArrayList();
        assertEquals(query.toString(), TemplateHelper.getPrecomputeQuery(t, indexes).toString());
        assertTrue(indexes.size() == 1);
        System.out.println("pathToQueryNode: " + pathToQueryNode);
        List expIndexes = new ArrayList(Collections.singleton(pathToQueryNode.get("Employee.name")));
        assertEquals(expIndexes.toString(), indexes.toString());
    }

}
