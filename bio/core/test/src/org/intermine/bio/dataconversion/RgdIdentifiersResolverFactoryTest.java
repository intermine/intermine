package org.intermine.bio.dataconversion;

import java.io.File;

import junit.framework.TestCase;

/**
 * RgdIdentifiersResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class RgdIdentifiersResolverFactoryTest extends TestCase {

    RgdIdentifiersResolverFactory factory;
    String rgdDataFile = "rgd.data.sample";

    public RgdIdentifiersResolverFactoryTest() {
    }

    public RgdIdentifiersResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new RgdIdentifiersResolverFactory();
        IdResolverFactory.resolver = null;
        factory.createIdResolver();
    }

    public void testCreateFromFile() throws Exception {
        File f = new File(getClass().getClassLoader().getResource(rgdDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }
        factory.createFromFile(f);
        // IdResolverFactory.resolver.writeToFile(new File("build/rgd"));
        assertTrue(IdResolverFactory.resolver.getTaxons().contains("10116"));
        assertEquals("RGD:1307273", IdResolverFactory.resolver.resolveId("10116", "Abcd4").iterator().next());
        assertTrue(IdResolverFactory.resolver.resolveId("10116", "gene", "ENSRNOG00000011964").iterator().next().startsWith("RGD"));
    }
}
