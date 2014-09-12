package org.intermine.bio.dataconversion;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

/**
 * HumanIdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class HumanIdResolverFactoryTest extends TestCase {
    HumanIdResolverFactory factory;
    String humanidDataFile = "humanid.data.sample";

    public HumanIdResolverFactoryTest() {
    }

    public HumanIdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new HumanIdResolverFactory();
        IdResolverFactory.resolver = null;
        factory.createIdResolver();
    }

    public void testCreateFromFile() throws Exception {
        File f = new File(getClass().getClassLoader().getResource(humanidDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }
        factory.createFromFile(f);
        // IdResolverFactory.resolver.writeToFile(new File("build/humanid"));
        assertTrue(IdResolverFactory.resolver.getTaxons().contains("9606"));
        assertTrue(IdResolverFactory.resolver.isPrimaryIdentifier("9606", "CDKN1B"));
        assertEquals("NBN", IdResolverFactory.resolver.resolveId("9606", "ENSG00000104320").iterator().next());
        assertEquals("LIX1", IdResolverFactory.resolver.resolveId("9606", "OMIM:610466").iterator().next());
        assertEquals("ERC2", IdResolverFactory.resolver.resolveId("9606", "HGNC:31922").iterator().next());
        assertEquals(Collections.EMPTY_SET, IdResolverFactory.resolver.resolveId("9606", "pid1"));
    }
}
