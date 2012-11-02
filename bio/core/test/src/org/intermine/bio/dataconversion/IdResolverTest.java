package org.intermine.bio.dataconversion;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class IdResolverTest extends TestCase
{
    private IdResolver resolver;
    private final static String RAT = "10116";
    private final static String MOUSE = "10090";
    
    
    public IdResolverTest() {
    }

    public IdResolverTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resolver = new IdResolver("Gene");
        resolver.addMainIds("101", "Gene1",
                new HashSet<String>(Arrays.asList(new String[] { "G1", "g1" })));
        resolver.addSynonyms("101", "Gene1",
                new HashSet<String>(Arrays.asList(new String[] { "syn1", "syn2" })));
        resolver.addSynonyms("101", "Gene2",
                new HashSet<String>(Arrays.asList(new String[] { "syn3" })));
    }

//    public void testFileRoundTrip() throws Exception {
//        File f = new File("build/resolverTest");
//        resolver.writeToFile(f);
//
//        IdResolver readFromFile = new IdResolver("Gene");
//        readFromFile.populateFromFile(f);
//        assertEquals(resolver.orgIdMaps, readFromFile.orgIdMaps);
//        assertEquals(resolver.orgMainMaps, readFromFile.orgMainMaps);
//        assertEquals(resolver.orgSynMaps, readFromFile.orgSynMaps);
//    }

    public void testParse() {
        Set taxonIds = new HashSet();
        taxonIds.add(RAT);
        taxonIds.add(MOUSE);
        IdResolver rslv = IdResolverService.getIdResolverByOrganism(taxonIds);
        rslv.resolveId(RAT, "ENSRNOG00000015654");
    }

}
