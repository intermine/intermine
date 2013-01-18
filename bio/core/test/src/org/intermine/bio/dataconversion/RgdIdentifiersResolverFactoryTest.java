package org.intermine.bio.dataconversion;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

/**
 * RgdIdentifiersResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class RgdIdentifiersResolverFactoryTest extends TestCase {

    RgdIdentifiersResolverFactory factory;
    String rgdDataFile = "resources/rgd.data.sample";

    public RgdIdentifiersResolverFactoryTest() {
    }

    public RgdIdentifiersResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new RgdIdentifiersResolverFactory();
        IdResolverFactory.resolver = new IdResolver();
    }

    public void testCreateFromFile() throws Exception {
        File f = new File(rgdDataFile);
        if (!f.exists()) {
            fail("data file not found");
        }

        factory.createFromFile(f);
        assertTrue(IdResolverFactory.resolver.getTaxons().size() == 1);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"10116"})), IdResolverFactory.resolver.getTaxons());
        assertEquals("RGD:1307273", IdResolverFactory.resolver.resolveId("10116", "Abcd4").iterator().next());
        assertTrue(IdResolverFactory.resolver.resolveId("10116", "ENSRNOG00000011964").iterator().next().startsWith("RGD"));
    }
}
