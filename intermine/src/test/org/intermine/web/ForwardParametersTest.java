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

import junit.framework.TestCase;

import org.apache.struts.action.ActionForward;
import org.intermine.web.ForwardParameters;

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
}
