package org.flymine.objectstore.query;

import junit.framework.TestCase;

import java.util.List;

import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;

public class CollatedResultTest extends TestCase
{
    public CollatedResultTest(String arg1) {
        super(arg1);
    }

    public void testConstructNullResult() throws Exception {
        try {
            CollatedResult res = new CollatedResult(null, new Query(), new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullQuery() throws Exception {
        try {
            CollatedResult res = new CollatedResult("test", null, new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullObjectStore() throws Exception {
        try {
            CollatedResult res = new CollatedResult("test", new Query(), null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }
}
