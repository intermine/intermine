package org.intermine.bio.dataconversion;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

/**
 * HgncIdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class HgncIdResolverFactoryTest extends TestCase {
    HgncIdResolverFactory factory;
    String hgncDataFile = "hgnc.data.sample";

    public HgncIdResolverFactoryTest() {
    }

    public HgncIdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new HgncIdResolverFactory();
        IdResolverFactory.resolver = null;
        factory.createIdResolver();
    }

    public void testCreateFromFile() throws Exception {
        File f = new File(getClass().getClassLoader().getResource(hgncDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }

        factory.createFromFile(f);
        // IdResolverFactory.resolver.writeToFile(new File("build/hgnc"));
        assertTrue(IdResolverFactory.resolver.getTaxons().contains("9606"));
        assertTrue(IdResolverFactory.resolver.isPrimaryIdentifier("9606", "A1BG"));
        assertEquals("A1CF", IdResolverFactory.resolver.resolveId("9606", "APOBEC1CF").iterator().next());
        assertEquals("A1BG-AS1", IdResolverFactory.resolver.resolveId("9606", "gene", "NCRNA00181").iterator().next());
    }
}
