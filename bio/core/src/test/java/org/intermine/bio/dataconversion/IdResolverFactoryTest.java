package org.intermine.bio.dataconversion;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

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
    final static String ENTREZ_FILE = "entrez.data.sample";
    final static String MOUSE_FILE = "mgi.data.sample";


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

    public void testCreateMultipleResolvers() throws Exception {
        // test entrez + mgi factory
        EntrezGeneIdResolverFactory entrezFactory = new EntrezGeneIdResolverFactory();


        ClassLoader cl = getClass().getClassLoader();
        URL u = cl.getResource(ENTREZ_FILE);
        if (u == null) {
            fail("data file not found");
            return;
        }

        File f = new File(u.toURI());
        if (!f.exists()) {
            fail("data file not found");
        }

        entrezFactory.createFromFile(f, new HashSet<String>(Arrays.asList(new String[] {"7227", "4932", "7955"})));

        MgiIdentifiersResolverFactory mgiFactory = new MgiIdentifiersResolverFactory();

        File mgiFile = new File(getClass().getClassLoader().getResource(MOUSE_FILE).toURI());
        if (!mgiFile.exists()) {
            fail("data file not found");
        }
        mgiFactory.createFromFile(mgiFile);

        assertTrue(IdResolverFactory.resolver.getTaxons().size() == 3);
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"4932", "10090", "7955"})), IdResolverFactory.resolver.getTaxons());
    }
}

