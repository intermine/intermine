package org.intermine.bio.dataconversion;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    String idresolverCache = "resources/resolver.cache.test";
    String entrezDataFile = "resources/entrez.data.sample";
    String idresolverConfig = "resolver_config.properties";

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
        factory.propFile = idresolverConfig;
        factory.readConfig();
        assertEquals(5, factory.configXref.size());
        assertTrue(factory.configXref.containsKey("7955"));
        assertFalse(factory.configXref.containsValue("OMIM"));
        assertTrue(factory.configPrefix.containsKey("10090"));
        assertTrue(factory.configStrains.containsValue("559292"));
        assertTrue(factory.ignoredTaxonIds.contains("6239"));
    }

    public void testGetStrain() throws Exception {
        Set<String> taxSet = new LinkedHashSet<String>(Arrays.asList(new String[] {"10090", "4932"}));
        assertTrue(factory.getStrain(taxSet).containsKey("559292"));
    }

    public void testCreateIdResolver() throws Exception {
        // resolver cached
        factory.ID_RESOLVER_CACHED_FILE_NAME = idresolverCache;

        factory.createIdResolver(Collections.<String> emptySet());
        assertNull(IdResolverFactory.resolver);

        factory.createIdResolver("101");
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"101", "102"})), IdResolverFactory.resolver.getTaxons());

        factory.createIdResolver(new HashSet<String>(Arrays.asList(new String[] {"7227", "4932"})));
        assertTrue(IdResolverFactory.resolver.getTaxons().size() != 4);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"101", "102"})), IdResolverFactory.resolver.getTaxons());

        // not cached
        File f = new File(entrezDataFile);
        if (!f.exists()) {
            fail("data file not found");
        }

        factory.createFromFile(f, new HashSet<String>(Arrays.asList(new String[] {"7227", "4932", "10090", "7955"})));
        assertTrue(IdResolverFactory.resolver.getTaxons().size() == 5);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"7955", "102", "4932", "101", "10090"})), IdResolverFactory.resolver.getTaxons());
        assertTrue(IdResolverFactory.resolver.resolveId("10090", "gene", "Abca2").iterator().next().startsWith("MGI"));
    }
}
