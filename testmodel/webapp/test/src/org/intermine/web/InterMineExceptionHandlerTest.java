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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.struts.config.ExceptionConfig;
import org.intermine.web.logic.InterMineExceptionHandler;

import servletunit.struts.MockStrutsTestCase;

/**
 * The way this will be tested is to call an actual action with
 * methods that we know cause Exceptions to be thrown
 */
public class InterMineExceptionHandlerTest extends MockStrutsTestCase {

    private InterMineExceptionHandler handler;
    private ExceptionConfig ec;

    public InterMineExceptionHandlerTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        handler = new InterMineExceptionHandler();
        ec = new ExceptionConfig();
        ec.setPath("/rubbish");
        ec.setKey("ec.key");
    }

    public void testSimpleException() throws Exception {
        Exception e = new Exception();
        handler.execute(e, ec, null, null, getRequest(), null);

        // The original exception
        assertEquals(e, getRequest().getAttribute("exception"));
        // The stack trace
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        assertEquals(sw.toString(), getRequest().getAttribute("stacktrace"));
        // The messages
        verifyActionErrors(new String [] {"ec.key"});
    }

    public void testNestedException() throws Exception {
        Exception e1 = new Exception();
        Exception e2 = new Exception(e1);
        handler.execute(e2, ec, null, null, getRequest(), null);

        // The original exception
        assertEquals(e2, getRequest().getAttribute("exception"));
        // The stack trace
        StringWriter sw = new StringWriter();
        e2.printStackTrace(new PrintWriter(sw));
        assertEquals(sw.toString(), getRequest().getAttribute("stacktrace"));
        // The messages
        verifyActionErrors(new String [] { "ec.key",
                                           "ec.key" });

    }

}
