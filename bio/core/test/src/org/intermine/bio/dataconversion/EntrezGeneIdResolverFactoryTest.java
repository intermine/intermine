package org.intermine.bio.dataconversion;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.TestCase;

/**
 * EntrezGeneIdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class EntrezGeneIdResolverFactoryTest extends TestCase {

    EntrezGeneIdResolverFactory factory;
    final String idresolverCache = "resolver.cache.test";
    final String entrezDataFile = "entrez.data.sample";
    final String idresolverConfig = "resolver_config.properties";

    public EntrezGeneIdResolverFactoryTest() {
    }

    public EntrezGeneIdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new EntrezGeneIdResolverFactory();
        IdResolverFactory.resolver = null;
    }

    public void testReadConfig() throws Exception {

        factory.readConfig(idresolverConfig);

        assertEquals(5, factory.getXrefs().size());
        assertTrue("7955 is one of " + factory.getXrefs().keySet(),
                factory.getXrefs().containsKey("7955"));
        assertFalse("OMIM is not one of " + factory.getXrefs().values(),
                factory.getXrefs().containsValue("OMIM"));
        assertTrue("10116 is one of " + factory.getPrefixes().keySet(),
                factory.getPrefixes().containsKey("10116"));
        assertNotNull("{559292} has a strain",
                factory.getStrain(Collections.singleton("559292")));
        assertTrue("We are ignoring 6239",
                factory.getIgnoredTaxonIds().contains("6239"));

    }

    public void testGetStrain() throws Exception {
        Set<String> taxSet = new LinkedHashSet<String>(Arrays.asList(new String[] {"10090", "4932", "10116", "9606"}));
        assertTrue(factory.getStrain(taxSet).containsKey("559292"));
    }

    public void testCreateIdResolver() throws Exception {

        File f = new File(getClass().getClassLoader().getResource(idresolverCache).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }

        factory.createIdResolver(Collections.<String> emptySet());
        assertNull(IdResolverFactory.resolver);



        factory.createIdResolver("101");
        // resolver cached
        factory.restoreFromFile(f);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"101", "102"})), IdResolverFactory.resolver.getTaxons());

        factory.createIdResolver(new HashSet<String>(Arrays.asList(new String[] {"7227", "4932"})));
        assertTrue(IdResolverFactory.resolver.getTaxons().size() != 4);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"101", "102"})), IdResolverFactory.resolver.getTaxons());

        // not cached
        File entrezFile = new File(getClass().getClassLoader().getResource(entrezDataFile).toURI());
        if (!entrezFile.exists()) {
            fail("data file not found");
        }

        factory.createFromFile(entrezFile, new HashSet<String>(Arrays.asList(new String[] {"7227", "4932", "10090", "7955", "9606", "10116"})));
        assertTrue(IdResolverFactory.resolver.getTaxons().size() == 7);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"7955", "102", "4932", "101", "10116", "9606", "10090"})), IdResolverFactory.resolver.getTaxons());

        // mouse
        String mouseGene = IdResolverFactory.resolver.resolveId("10090", "gene", "Abca2").iterator().next();
        assertEquals("MGI:99606", mouseGene);

        // rat
        String ratGene = IdResolverFactory.resolver.resolveId("10116", "gene", "Asip").iterator().next();
        assertEquals("RGD:2003", ratGene);

        // hgnc
        String peopleGene = IdResolverFactory.resolver.resolveId("9606", "gene", "HGNC:5").iterator().next();
        assertEquals("1", peopleGene);

    }
}
