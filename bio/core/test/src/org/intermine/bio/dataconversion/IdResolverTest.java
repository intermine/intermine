package org.intermine.bio.dataconversion;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * IdResolver Unit Test
 *
 * @author rns
 * @author Fengyuan Hu
 *
 */
public class IdResolverTest extends TestCase
{
    private IdResolver resolver;
    String taxId1 = "101";
    String taxId2 = "102";
    String clsName1 = "gene";
    String clsName2 = "mRNA";
    String clsName3 = "exon";
    String primaryId1 = "Gene1";
    String primaryId2 = "Gene2";
    String primaryId3 = "mRNA1";
    String mainId1 = "G1";
    String mainId2 = "g1";
    String mainId3 = "M1";
    String mainId4 = "m1";
    String synonym1 = "syn1";
    String synonym2 = "syn2";
    String synonym3 = "syn3";
    String synonym4 = "syn4";
    Set<String> mainIdSet1 = new HashSet<String>(Arrays.asList(new String[] { mainId1, mainId2 }));
    Set<String> mainIdSet2 = new HashSet<String>(Arrays.asList(new String[] { mainId3, mainId4 }));
    Set<String> SynonymSet1 = new HashSet<String>(Arrays.asList(new String[] { synonym1, synonym2 }));
    Set<String> SynonymSet2 = new HashSet<String>(Arrays.asList(new String[] { synonym3 }));

    public IdResolverTest() {
    }

    public IdResolverTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        resolver = new IdResolver();

        // organism 101
        resolver.addMainIds(taxId1, clsName1, primaryId1, mainIdSet1);
        resolver.addSynonyms(taxId1, clsName1, primaryId1, SynonymSet1);
        resolver.addSynonyms(taxId1, clsName1, primaryId2, SynonymSet2);
        resolver.addMainIds(taxId1, clsName2, primaryId3, mainIdSet1);
        resolver.addSynonyms(taxId1, clsName2, primaryId3, SynonymSet1);

        // organism 102
        resolver.addMainIds(taxId2, clsName1, primaryId1, mainIdSet2);
        resolver.addSynonyms(taxId2, clsName1, primaryId1, SynonymSet2);
    }

    public void testCheckTaxonId() throws Exception {
        try {
            resolver.checkTaxonId(taxId1, clsName3);
            fail("Expected to Fail to assert: No exception thrown");
        } catch(IllegalArgumentException ex) {
            assertEquals("Catched exception", clsName3 + " IdResolver has no data for taxonId: " + taxId1 + ".", ex.getMessage());
        }

        try {
            resolver.checkTaxonId(taxId1, clsName1);
            fail("Expected to Fail to assert: No exception thrown");
        } catch(IllegalArgumentException ex) {
            assertEquals("Catched exception", clsName1 + " IdResolver has no data for taxonId: " + taxId1 + ".", ex.getMessage());
        }
    }

    public void testIsPrimaryIdentifier() throws Exception {
        boolean ans = true;
        boolean val;
        String testId = "G1";

        val = resolver.isPrimaryIdentifier(taxId1, clsName1, primaryId1);
        assertEquals(ans, val);

        val = resolver.isPrimaryIdentifier(taxId1, clsName1, testId);
        assertEquals(!ans, val);
    }

    public void testResolveId() throws Exception {
        assertEquals(Collections.emptySet(), resolver.resolveId(taxId1, clsName2, primaryId1));
        assertEquals(Collections.singleton(primaryId1), resolver.resolveId(taxId1, clsName1, primaryId1));
        assertEquals(Collections.singleton(primaryId1), resolver.resolveId(taxId1, clsName1, mainId1));
        assertEquals(Collections.singleton(primaryId1), resolver.resolveId(taxId1, clsName1, synonym1));

        try {
            resolver.resolveId(taxId1, clsName3, primaryId1);
            fail("Expected to Fail to assert: No exception thrown");
        } catch(IllegalArgumentException ex) {
            assertEquals("Catched exception", clsName3 + " IdResolver has no data for taxonId: " + taxId1 + ".", ex.getMessage());
        }
    }

    public void testResolveIds() throws Exception {

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
}
