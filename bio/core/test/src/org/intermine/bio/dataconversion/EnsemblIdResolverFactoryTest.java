package org.intermine.bio.dataconversion;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

    public void testValidChromosomes() throws Exception {
        Set<String> validedChrs = factory.validChromosomes();
        assertTrue(validedChrs.contains("12" + ""));
        assertTrue(validedChrs.contains("X"));
        assertFalse(validedChrs.contains("A"));
        assertFalse(validedChrs.contains("23"));
    }

    public void testCreateFromFile() throws Exception {
        File f = new File(getClass().getClassLoader().getResource(ensemblDataFile).toURI());
        if (!f.exists()) {
            fail("data file not found");
        }
        factory.createFromFile(f);
        // IdResolverFactory.resolver.writeToFile(new File("build/ensembl"));
        assertEquals(new LinkedHashSet<String>(Arrays.asList(new String[] {"9606"})), IdResolverFactory.resolver.getTaxons());
        assertTrue(IdResolverFactory.resolver.isPrimaryIdentifier("9606", "pid2"));
        assertEquals("pid3", IdResolverFactory.resolver.resolveId("9606", "pid3").iterator().next());
        assertEquals(Collections.EMPTY_SET, IdResolverFactory.resolver.resolveId("9606", "gene", "pid1"));
    }
}
