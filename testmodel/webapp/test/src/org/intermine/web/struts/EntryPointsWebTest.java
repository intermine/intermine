package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Tests that certain pages can be hit with no existing session without giving a
 * "Your session has expired" message.
 * 
 * @author tom
 */
public class EntryPointsWebTest extends WebTestBase
{
    public EntryPointsWebTest(String name) {
        super(name);
    }
    
    // VALID ENTRY POINTS
    
    public void testEntryBegin() throws Exception {
        beginAt("/begin.do");
        assertTitleContains("InterMine: Begin");
        assertTextNotPresent("Your session has expired");
    }
    
    public void testEntryLogin() throws Exception {
        beginAt("/login.do");
        assertTitleContains("InterMine: Login page");
        assertTextNotPresent("Your session has expired");
    }
    
    public void testEntryPortal() throws Exception {
        beginAt("/portal.do?externalid=EmployeeA1");
        assertTextNotPresent("Your session has expired");
    }
    
    public void testEntryPing() throws Exception {
        beginAt("/ping.do");
        assertTextNotPresent("Your session has expired");
    }
    
    public void testEntryMyMine() throws Exception {
        beginAt("/mymine.do");
        assertTitleContains("InterMine: Begin");
        assertTextNotPresent("Your session has expired");
    }
    
    // NOT VALID ENTRY POINTS
    
    // TODO Fix this test when session initialisation is fixed
    // from website
//    public void testEntryQuery() throws Exception {
//        beginAt("/query.do");
//        assertTitleContains("InterMine: Begin");
//        assertTextPresent("Your session has expired");
//    }
    
    private void assertTitleContains(String title) {
        assertEquals(title, getDialog().getPageTitle().trim());
    }
}
