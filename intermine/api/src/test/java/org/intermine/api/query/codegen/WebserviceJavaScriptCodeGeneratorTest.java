package org.intermine.api.query.codegen;

public class WebserviceJavaScriptCodeGeneratorTest extends
        WebserviceJavaCodeGeneratorTest {

    public WebserviceJavaScriptCodeGeneratorTest() {
        super();
    }

    public WebserviceJavaScriptCodeGeneratorTest(String testName) {
        super(testName);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Override
    public void setUp() {
        lang = "js";
        cg = new WebserviceJavaScriptCodeGenerator();
    }

}
