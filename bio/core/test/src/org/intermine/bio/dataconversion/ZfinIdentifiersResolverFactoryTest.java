package org.intermine.bio.dataconversion;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

/**
 * ZfinIdentifiersResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class ZfinIdentifiersResolverFactoryTest extends TestCase {
    ZfinIdentifiersResolverFactory factory;
    String zfinDataFile = "zfin.data.sample";

    public ZfinIdentifiersResolverFactoryTest() {
    }

    public ZfinIdentifiersResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new ZfinIdentifiersResolverFactory();
        IdResolverFactory.resolver = null;
        factory.createIdResolver();
    }

    public void testCreateFromFile() throws Exception {
        File f = new File(getClass().getClassLoader().getResource(zfinDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }
        factory.createFromFile(f);
        // IdResolverFactory.resolver.writeToFile(new File("build/zfin"));
        assertTrue(IdResolverFactory.resolver.getTaxons().contains("7955"));
        assertTrue(IdResolverFactory.resolver.isPrimaryIdentifier("7955", "ZDB-GENE-000125-12"));
        assertEquals("ZDB-GENE-000112-47", IdResolverFactory.resolver.resolveId("7955", "ppardb").iterator().next());
        assertEquals("ZDB-GENE-000128-11", IdResolverFactory.resolver.resolveId("7955", "gene", "ENSDARG00000001859").iterator().next());
    }
}