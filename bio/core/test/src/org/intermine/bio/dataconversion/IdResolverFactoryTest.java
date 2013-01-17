package org.intermine.bio.dataconversion;

import junit.framework.TestCase;

/**
 * IdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class IdResolverFactoryTest extends TestCase {

    IdResolverFactory factory;
    String idresolverCache = "idresolver.cache.test";

    public IdResolverFactoryTest() {
    }

    public IdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new EntrezGeneIdResolverFactory(); // can only test a concrete class

    }
}
