package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import net.sourceforge.jwebunit.WebTestCase;

/**
 *
 * @author tom riley
 */
public class AspectsWebTest extends WebTestBase
{
    public AspectsWebTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();       
        //getTestContext().setBaseUrl("http://theoden.flymine.org:8080/intermine-app2");
    }

    public void testAspectIcons() throws Exception {
        beginAt("/");

        // begin page
        assertLinkPresentWithText("People");
        assertLinkPresentWithText("Entities");

        // move to People aspect
        clickLinkWithText("People");
        assertEquals("InterMine: Starting Point: People", getDialog().getPageTitle().trim());
        assertTextPresent("Query starting points"); // starting points
        assertLinkPresentWithText("Employee");
        assertLinkPresentWithText("Manager");
        assertLinkPresentWithText("CEO");
        assertLinkPresentWithText("Contractor");
        assertLinkPresentWithText("Secretary");
        assertTextPresent("View all the employees with certain name"); // related templates
        assertFormPresent("aspectForm"); // aspect popup
        selectOptionByValue("name", "Entities"); // select Entities from aspect popup

        // move to entities aspect
        assertEquals("InterMine: Starting Point: Entities", getDialog().getPageTitle().trim());
        assertTextPresent("Custom tile body."); // custom aspect tile
        assertLinkPresentWithText("Execute employeesWithOldManagers, skipping query builder");

        // test query link
        clickLinkWithText("Execute employeesWithOldManagers, go to builder");
        assertEquals("InterMine: Query builder page", getDialog().getPageTitle().trim());
    }
}
