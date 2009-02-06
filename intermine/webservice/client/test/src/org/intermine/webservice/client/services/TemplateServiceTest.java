package org.intermine.webservice.client.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.TestUtil;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests functionality of TemplateService - client class, implementing easy
 * access to InterMine web service. That's why it tests the web service itself
 * as well.
 * 
 * @author Jakub Kulaviak
 **/
public class TemplateServiceTest extends TestCase
{

    /**
     * Checks Java client and that default parameters of template are replaced with 
     * parameters provided by client.
     */
    public void testNonDefaultParameters() {
        TemplateService service = TestUtil.getTemplateService();
        List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
        parameters.add(new TemplateParameter("Employee.name", "contains", "EmployeeA"));
        
        TemplateParameter par1 = new TemplateParameter("Employee.age", "gt", "10");
        par1.setCode("B");
        parameters.add(par1);
        
        TemplateParameter par2 = new TemplateParameter("Employee.age", "lt", "60");
        par2.setCode("C");
        parameters.add(par2);
        
        parameters.add(new TemplateParameter("Employee.fullTime", "eq", "true"));
        List<List<String>> results = service.getResult("fourConstraints", parameters, 10);
        assertEquals(2, results.size());
        // returns 2 results, notice that the logic for constraints B and C is OR -> returns Employee of age 10 
        TestUtil.checkRow(results.get(0), "EmployeeA1", "10", "1", "true");
        TestUtil.checkRow(results.get(1), "EmployeeA2", "20", "2", "true");
    }
}
