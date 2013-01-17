package org.intermine.bio.dataconversion;

import java.io.File;

import junit.framework.TestCase;

/**
 * EntrezGeneIdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class EntrezGeneIdResolverFactoryTest extends TestCase {

    EntrezGeneIdResolverFactory factory;
    String idresolverCache = "resolver.cache.test";
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
        IdResolverFactory.resolver = new IdResolver();
    }

    public void testReadConfig() throws Exception {
        File configFile = new File(getClass().getClassLoader().
                getResource(idresolverConfig).toURI());
        factory.readConfig();
        assertEquals(5, factory.config_xref.size());
        assertTrue(factory.config_xref.containsKey("7955"));
        assertFalse(factory.config_xref.containsValue("OMIM"));

    }

    public void testGetIdResolver() throws Exception {

    }
}
