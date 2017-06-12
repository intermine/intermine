package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2016
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Tests for the WebservicePythonCodeGenerator class.
 *
 * @author Alex Kalderimis
 */
public class WebservicePythonCodeGeneratorTest extends WebserviceJavaCodeGeneratorTest {

    public WebservicePythonCodeGeneratorTest(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Override
    public void setUp() {
        lang = "python";
        cg = new WebservicePythonCodeGenerator();
    }

    /*
     * The tests are defined in the parent class. This subclass simply redefines the language
     * to be used for the generator.
     */

}
