package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;

public class ResultsTest extends TestCase
{
    public ResultsTest(String arg1) {
        super(arg1);
    }

    public void testConstructNullQuery() throws Exception {
        try {
            Results res = new Results(null, new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullObjectStore() throws Exception {
        try {
            Results res = new Results(new Query(), null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }



}
