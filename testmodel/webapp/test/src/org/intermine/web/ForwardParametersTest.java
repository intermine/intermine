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

import junit.framework.TestCase;

import org.apache.struts.action.ActionForward;

public class ForwardParametersTest extends TestCase
{
    public ForwardParametersTest(String arg) {
        super(arg);
    }
    
    public void testAppendOneParameter() throws Exception {
        ActionForward oaf = new ActionForward("/dummy.do", true);
        ActionForward af = new ForwardParameters(oaf).addParameter("p1", "v1").forward();
        
        assertEquals(oaf.getPath() + "?p1=v1", af.getPath());
    }
    
    public void testAppendTwoParameters() throws Exception {
        ActionForward oaf = new ActionForward("/dummy.do", true);
        ActionForward af = new ForwardParameters(oaf).addParameter("p1", "v1")
                                                     .addParameter("p2", "v2").forward();
        
        assertEquals(oaf.getPath() + "?p1=v1&p2=v2", af.getPath());
    }
    
    public void testNoParameters() {
        ActionForward oaf = new ActionForward("/dummy.do", true);
        ActionForward af = new ForwardParameters(oaf).forward();
        
        assertEquals(oaf.getPath(), af.getPath());
    }
    
    public void testParametersWithAnchor() {
        ActionForward oaf = new ActionForward("/dummy.do", true);
        ActionForward af = new ForwardParameters(oaf).addParameter("p1", "v1")
                                                     .addParameter("p2", "v2")
                                                     .addAnchor("asdf").forward();
        
        assertEquals(oaf.getPath() + "?p1=v1&p2=v2#asdf", af.getPath());
    }
    
    public void testOnlyAnchor() {
        ActionForward oaf = new ActionForward("/dummy.do", true);
        ActionForward af = new ForwardParameters(oaf).addAnchor("asdf").forward();
        
        assertEquals(oaf.getPath() + "#asdf", af.getPath());
    }
}
