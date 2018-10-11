package org.intermine.bio.dataconversion;

import junit.framework.TestCase;

/**
 * MockIdResolverFactory Unit Test
 *
 * @author Fengyuan Hu
 *
 */
public class MockIdResolverFactoryTest extends TestCase {

    MockIdResolverFactory factory;

    public MockIdResolverFactoryTest() {
    }

    public MockIdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new MockIdResolverFactory("gene");
    }

    public void testGetIdResolver() throws Exception {
        assertNotNull(factory.getIdResolver());

    }
}
