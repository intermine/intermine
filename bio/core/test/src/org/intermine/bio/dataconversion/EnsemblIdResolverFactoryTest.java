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
    final String idresolverCache = "ensembl.idresolver.cache";

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
    }

    public void testCreateFromFile() throws Exception {

        File f = new File(getClass().getClassLoader().getResource(ensemblDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }

        factory.createIdResolver();
        // resolver cached
        factory.populateFromFile(f);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"9606"})), IdResolverFactory.resolver.getTaxons());
//
//        factory.createIdResolver(new HashSet<String>(Arrays.asList(new String[] {"7227", "4932"})));
//        assertTrue(IdResolverFactory.resolver.getTaxons().size() != 4);
//        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"101", "102"})), IdResolverFactory.resolver.getTaxons());
//
//        // not cached
//        File entrezFile = new File(getClass().getClassLoader().getResource(entrezDataFile).toURI());
//        if (!entrezFile.exists()) {
//            fail("data file not found");
//        }
//
//        factory.createFromFile(entrezFile, new HashSet<String>(Arrays.asList(new String[] {"7227", "4932", "10090", "7955", "9606", "10116"})));
//        assertTrue(IdResolverFactory.resolver.getTaxons().size() == 7);
//        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"7955", "102", "4932", "101", "10116", "9606", "10090"})), IdResolverFactory.resolver.getTaxons());
//
//        // mouse
//        String mouseGene = IdResolverFactory.resolver.resolveId("10090", "gene", "Abca2").iterator().next();
//        assertEquals("MGI:99606", mouseGene);
//
//        // rat
//        String ratGene = IdResolverFactory.resolver.resolveId("10116", "gene", "Asip").iterator().next();
//        assertEquals("RGD:2003", ratGene);

        // hgnc
        String peopleGene = IdResolverFactory.resolver.resolveId("9606", "gene", "HGNC:6331").iterator().next();
        assertEquals("ENSG00000275008", peopleGene);

        // NCBI
        peopleGene = IdResolverFactory.resolver.resolveId("9606", "gene", "100874343").iterator().next();
        assertEquals("ENSG00000231948", peopleGene);
    }
}
