package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Tests for the SavedQueryParser class
 *
 * @author Kim Rutherford
 */

import java.io.*;

import servletunit.struts.MockStrutsTestCase;

public class SavedQueryParserTest extends MockStrutsTestCase
{
    public SavedQueryParserTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/example-queries.xml");
        Reader reader = new InputStreamReader(is);
        assertEquals("{allCompanies=QueryInfo <query={}, view=[Company], resultsInfo=null>, employeesNamesAndAges=QueryInfo <query={}, view=[Employee.name, Employee.age], resultsInfo=null>, employeesWithOldManagers=QueryInfo <query={Employee=Employee:Employee [], Employee.department=Employee.department:Department [], Employee.department.manager=Employee.department.manager:Manager [], Employee.department.manager.age=Employee.department.manager.age:int [> 10]}, view=[Employee.name, Employee.age, Employee.department.name, Employee.department.manager.age], resultsInfo=null>}",
                     "" + new SavedQueryParser().process(reader));
    }
}
