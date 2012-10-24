package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2012 modMine_Test-2.M
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Tests for the WebservicePerlCodeGenerator class.
 *
 * @author Fengyuan Hu
 * @author Alex Kalderimis
 */
public class WebservicePerlCodeGeneratorTest extends WebserviceJavaCodeGeneratorTest {

    public WebservicePerlCodeGeneratorTest(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Override
    public void setUp() {
        lang = "perl";
        cg = new WebservicePerlCodeGenerator();
    }

    /*
     * The tests are defined in the parent class. This subclass simply redefines the language
     * to be used for the generator.
     */

}
