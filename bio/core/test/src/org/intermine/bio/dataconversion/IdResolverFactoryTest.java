package org.intermine.bio.dataconversion;

import java.io.File;

import junit.framework.TestCase;

/**
 * IdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class IdResolverFactoryTest extends TestCase {

    IdResolverFactory factory;
    String idresolverCache = "resolver.cache.test";

    public IdResolverFactoryTest() {
    }

    public IdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new EntrezGeneIdResolverFactory(); // can only test on a concrete class
        IdResolverFactory.resolver = new IdResolver();
    }

    public void testGetIdResolver() throws Exception {
        assertEquals(IdResolverFactory.resolver, factory.getIdResolver(true));
        assertEquals(IdResolverFactory.resolver, factory.getIdResolver(false));
    }

    public void testRestoreFromFile() throws Exception {
        assertFalse(factory.restoreFromFile());

        File testFile = new File(getClass().getClassLoader().
                getResource(idresolverCache).toURI());
        assertTrue(factory.restoreFromFile(testFile));
        assertEquals(2, IdResolverFactory.resolver.getTaxons().size());
    }

    public void testCreateMultipleResolvers() throws Exception {

    }
}
