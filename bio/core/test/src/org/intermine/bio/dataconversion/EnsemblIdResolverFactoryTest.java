package org.intermine.bio.dataconversion;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

/**
 * EnsemblIdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class EnsemblIdResolverFactoryTest extends TestCase {
    EnsemblIdResolverFactory factory;
    String ensemblDataFile = "ensembl.data.sample";
    final String idresolverCache = "resolver.cache.test";

    public EnsemblIdResolverFactoryTest() {
    }

    public EnsemblIdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new EnsemblIdResolverFactory();
        IdResolverFactory.resolver = null;
        factory.createIdResolver();
    }

    public void testCreateFromFile() throws Exception {

        File f = new File(getClass().getClassLoader().getResource(ensemblDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }
        factory.populateFromFile(f);
        // IdResolverFactory.resolver.writeToFile(new File("build/ensembl"));
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"9606"})), IdResolverFactory.resolver.getTaxons());
        assertTrue(IdResolverFactory.resolver.isPrimaryIdentifier("9606", "ENSG00000197468"));
        assertEquals("ENSG00000231049", IdResolverFactory.resolver.resolveId("9606", "OR52B5P").iterator().next());
        assertEquals(Collections.EMPTY_SET, IdResolverFactory.resolver.resolveId("9606", "gene", "monkey"));

        factory = new EnsemblIdResolverFactory();
    }
}
