package org.intermine.testing;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;
import junit.extensions.*;
import java.lang.reflect.*;

/**
 * A test case that provides a one-time setup and teardown.
 *
 * Note that <b>every</b> subclass of this TestCase class must implement a static suite method with
 * no arguments that follows the following pattern:
 *
 * <pre>public static Test suite() {
 *     return OneTimeTestCase.buildSuite(&lt;this class&gt;.class);
 * }</pre>
 *
 * In addition, any subclass that implements the setUp method must call super.setUp(), and any
 * subclass that implements the oneTimeTearDown method must call super.oneTimeTearDown().
 *
 * @author Matthew Wakeling
 */
public class OneTimeTestCase extends TestCase
{
    private static Method oneTimeTearDownMethod = null;
    private static Exception exception = null;
    
    /**
     * Constructor.
     */
    public OneTimeTestCase(String arg) {
        super(arg);
    }

    /**
     * Set up the test. The first time this is run for a given class, that class' OneTimeSetUp
     * method will be called, and the class' OneTimeTearDown method registered.
     */
    public void setUp() throws Exception {
        super.setUp();
        if ((oneTimeTearDownMethod == null) && (exception == null)) {
            try {
                // This is the first time for this test class.
                Class subjectClass = this.getClass();
                Method OTSetUp = subjectClass.getMethod("oneTimeSetUp", new Class[] {});
                OTSetUp.invoke(null, new Object[] {});
                oneTimeTearDownMethod = subjectClass.getMethod("oneTimeTearDown", new Class[] {});
                System.out.println("Setting one-time teardown method to " + oneTimeTearDownMethod);
            } catch (Exception e) {
                exception = e;
                throw e;
            }
        } else if (exception != null) {
            throw exception;
        }
    }

    /**
     * Returns a suite. This method must be overridden by all subclasses that have any tests.
     * Therefore, this default implementation exists in order to produce an error message if this
     * is not done.
     *
     * @return a Test suite
     */
    public static Test suite() {
        return new TestSuite(OneTimeTestCase.class);
    }

    /**
     * Returns a test suite that will do the one-time tear down.
     *
     * @return a Test suite
     */
    public static Test buildSuite(Class c) {
        TestSetup r = new TestSetup(new TestSuite(c)) {
            protected void tearDown() throws Exception {
                oneTimeTearDownMethod.invoke(null, new Object[] {});
            }
        };
        return r;
    }

    /**
     * Runs one-time setting up.
     */
    public static void oneTimeSetUp() throws Exception {
    }

    /**
     * Runs one-time tear down.
     */
    public static void oneTimeTearDown() throws Exception {
        oneTimeTearDownMethod = null;
        System.out.println("Setting one-time teardown method to null");
    }
}
