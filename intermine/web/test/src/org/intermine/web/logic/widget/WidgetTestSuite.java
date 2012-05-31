package org.intermine.web.logic.widget;

import junit.framework.TestSuite;


public class WidgetTestSuite extends TestSuite{
    
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        //suite.addTestSuite(EnrichmentWidgetTest.class);
        suite.addTestSuite(WidgetConfigTest.class);
        suite.addTestSuite(WidgetConfigUtilTest.class);
        suite.addTestSuite(WidgetLdrTest.class);
        return suite;
    }
}
