package org.intermine.pathquery;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class JsonOutputTest {

    private static final String QUERY_XML = "PathQueryBindingTest.xml";
    private static final String EXPECTATIONS = "expected-pathquery-json.properties";
    private Map<String, PathQuery> savedQueries;
    private Properties expectations;

    @Before
    public void setup() {
        ClassLoader cl = getClass().getClassLoader();
        InputStream pqsrc = cl.getResourceAsStream(QUERY_XML);
        savedQueries = PathQueryBinding.unmarshalPathQueries(new InputStreamReader(pqsrc), 2);
        InputStream expectedsrc = cl.getResourceAsStream(EXPECTATIONS);
        expectations = new Properties();
        try {
            expectations.load(expectedsrc);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to load expectations from " + EXPECTATIONS, ioe);
        }
    }

    @Test
    public void runTest() {
        Enumeration<?> names = expectations.propertyNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement().toString();
            PathQuery pq = savedQueries.get(name);
            if (pq == null) {
                fail("Could not find PathQuery " + name);
            }
            String expected = expectations.getProperty(name);
            assertEquals(String.format("%s did not serialise to '%s'", name, pq.toJson()), expected, pq.toJson());
        }
    }

}
