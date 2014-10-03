package org.intermine.webservice.server.jbrowse.util;

import static org.junit.Assert.*;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class NameSpacedPropertiesTest {

    private Properties namespaced;

    @Before
    public void setUp() {
        Properties parents = new Properties();
        parents.setProperty("foo.bar", "baz");
        parents.setProperty("foo.buzz", "biz");
        parents.setProperty("some other prop", "some other value");
        parents.setProperty("foo.bozz", "bezz");
        parents.setProperty("some other irrelevant key", "another value");
        namespaced = new NameSpacedProperties("foo", parents);
    }

    @Test
    public void getProperty() {
        assertEquals("baz", namespaced.getProperty("bar"));
        assertEquals("bezz", namespaced.getProperty("bozz"));
        assertNull(namespaced.getProperty("not even there"));
        assertNull(namespaced.getProperty("some other prop"));
    }

    @Test
    public void getPropertyWithDefault() {
        assertEquals("baz", namespaced.getProperty("bar", "NOPE"));
        assertEquals("foop", namespaced.getProperty("not even there", "foop"));
        assertEquals("fope", namespaced.getProperty("some other prop", "fope"));
    }

    @Test
    public void setProperty() {
        try {
            namespaced.setProperty("some prop", "some val");
            fail("Should have thrown an exception.");
        } catch (RuntimeException rte) {
            // Ignore.
        }
    }

    @Test
    public void propertyNames() {
        
        Set<String> names = new TreeSet<String>();
        Enumeration<?> propNames = namespaced.propertyNames();
        while (propNames.hasMoreElements()) {
            names.add((String) propNames.nextElement());
        }
        assertEquals(names + " should have 3 elements", 3, names.size());
        String[] expectedNames = new String[]{"bar", "bozz", "buzz"};
        String[] realNames = names.toArray(new String[3]);
        for (int i = 0; i < 3; i++) {
            assertEquals(expectedNames[i], realNames[i]);
        }
    }

}
