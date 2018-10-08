package org.intermine.bio.dataconversion;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.io.FileUtils;

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
    String taxId3 = "103";
    String clsName1 = "gene";
    private final String clsName2 = "mRNA";
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
    Set<String> mainIdSet1 = new LinkedHashSet<String>(Arrays.asList(new String[] { mainId1, mainId2 }));
    Set<String> mainIdSet2 = new LinkedHashSet<String>(Arrays.asList(new String[] { mainId3, mainId4 }));
    Set<String> SynonymSet1 = new LinkedHashSet<String>(Arrays.asList(new String[] { synonym1, synonym2 }));
    Set<String> SynonymSet2 = new LinkedHashSet<String>(Arrays.asList(new String[] { synonym3 }));

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
        resolver.addSynonyms(taxId1, clsName1, primaryId2, SynonymSet1);
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
            assertEquals("Catched exception", clsName3 + " IdResolver has no data for taxonId: '" + taxId1 + "'.", ex.getMessage());
        }

        try {
            resolver.checkTaxonId(taxId3, clsName3);
            fail("Expected to Fail to assert: No exception thrown");
        } catch(IllegalArgumentException ex) {
            assertEquals("Catched exception", clsName3 + " IdResolver has no data for taxonId: '" + taxId3 + "'.", ex.getMessage());
        }
    }

    public void testIsPrimaryIdentifier() throws Exception {
        boolean val;
        String testId = "G1";

        val = resolver.isPrimaryIdentifier(taxId1, clsName1, primaryId1);
        assertTrue(val);

        val = resolver.isPrimaryIdentifier(taxId1, clsName1, testId);
        assertFalse(val);
    }

    public void testResolveId() throws Exception {
        assertEquals(Collections.emptySet(), resolver.resolveId(taxId1, clsName2, primaryId1));
        assertEquals(Collections.singleton(primaryId1), resolver.resolveId(taxId1, clsName1, primaryId1));
        assertEquals(Collections.singleton(primaryId1), resolver.resolveId(taxId1, clsName1, mainId1));
        assertEquals(new HashSet<String>(Arrays.asList(primaryId1, primaryId2)), resolver.resolveId(taxId1, clsName1, synonym1));

        try {
            resolver.resolveId(taxId1, clsName3, primaryId1);
            fail("Expected to Fail to assert: No exception thrown");
        } catch(IllegalArgumentException ex) {
            assertEquals("Catched exception", clsName3 + " IdResolver has no data for taxonId: '" + taxId1 + "'.", ex.getMessage());
        }
    }

    public void testResolveIds() throws Exception {
        assertEquals(primaryId3, resolver.resolveIds(taxId1, clsName2, Arrays.asList(mainId1, synonym3)));
        assertEquals(primaryId3, resolver.resolveIds(taxId1, clsName2, Arrays.asList(mainId1, synonym1)));
        assertEquals(null, resolver.resolveIds(taxId1, clsName1, Arrays.asList(synonym1)));
        assertEquals(null, resolver.resolveIds(taxId1, clsName1, Arrays.asList(synonym1, synonym2)));
        assertEquals(null, resolver.resolveIds(taxId1, clsName1, Arrays.asList(mainId1, synonym1, synonym3)));
    }

    public void testGetSynonyms() throws Exception {
        assertEquals(4, resolver.getSynonyms(taxId1, clsName1, primaryId1).size());
        assertNull(resolver.getSynonyms(taxId1, clsName1, primaryId3));
    }

    public void testCountResolutions() throws Exception {
        assertEquals(2, resolver.countResolutions(taxId1, clsName1, synonym1));
        assertEquals(0, resolver.countResolutions(taxId1, clsName1, synonym4));
    }

    public void testHasTaxon() throws Exception {
        assertTrue(resolver.hasTaxon(taxId1));
        assertFalse(resolver.hasTaxon(taxId3));
    }

    public void testHasTaxons() throws Exception {
        assertTrue(resolver.hasTaxons(new HashSet<String>(Arrays.asList(taxId1, taxId2))));
        assertFalse(resolver.hasTaxons(new HashSet<String>(Arrays.asList(taxId1, taxId3))));
    }

    public void testGetTaxons() throws Exception {
        assertEquals(new HashSet<String>(Arrays.asList(taxId1, taxId2)), resolver.getTaxons());
    }

    public void testHasClassName() throws Exception {
        assertTrue(resolver.hasClassName(clsName1));
        assertFalse(resolver.hasClassName(clsName3));
    }

    public void testGetClassNames() throws Exception {
        assertEquals(new HashSet<String>(Arrays.asList(clsName1, clsName2)), resolver.getClassNames());
    }

    public void testHasTaxonAndClassName() throws Exception {
        assertTrue(resolver.hasTaxonAndClassName(taxId1, clsName1));
        assertFalse(resolver.hasTaxonAndClassName(taxId1, clsName3));
    }

    public void testHasTaxonsAndClassNames() throws Exception {
        Map<String, Set<String>> taxonIdAndClsNameMap = new HashMap<String, Set<String>>();
        Set<String> clsNameSet = new HashSet<String>();
        clsNameSet.add(clsName1);

        taxonIdAndClsNameMap.put(taxId1, clsNameSet);
        assertTrue(resolver.hasTaxonsAndClassNames(taxonIdAndClsNameMap));

        taxonIdAndClsNameMap.put(taxId2, clsNameSet);
        assertTrue(resolver.hasTaxonsAndClassNames(taxonIdAndClsNameMap));

        clsNameSet.add(clsName2);
        assertFalse(resolver.hasTaxonsAndClassNames(taxonIdAndClsNameMap));
    }

    public void testGetTaxonsAndClassNames() throws Exception {
        Map<String, Set<String>> taxonIdAndClsNameMap = new HashMap<String, Set<String>>();
        Set<String> clsNameTax1Set = new LinkedHashSet<String>();
        clsNameTax1Set.add(clsName1);
        clsNameTax1Set.add(clsName2);
        Set<String> clsNameTax2Set = new LinkedHashSet<String>();
        clsNameTax2Set.add(clsName1);
        taxonIdAndClsNameMap.put(taxId1, clsNameTax1Set);
        taxonIdAndClsNameMap.put(taxId2, clsNameTax2Set);

        assertEquals(taxonIdAndClsNameMap, resolver.getTaxonsAndClassNames());
    }

    public void testAddEntry() throws Exception {
        resolver.addEntry(taxId3, clsName3, primaryId3, SynonymSet1, false);
        resolver.addEntry(taxId3, clsName3, primaryId3, mainIdSet1, true);

        MultiKey key = new MultiKey(taxId3, clsName3);
        assertTrue(resolver.orgIdMaps.containsKey(key));
        assertTrue(resolver.orgIdMaps.get(key).containsKey(primaryId3));
        assertEquals(SynonymSet1, resolver.orgIdSynMaps.get(key).get(primaryId3));
        assertEquals(mainIdSet1, resolver.orgIdMainMaps.get(key).get(primaryId3));
    }

    public void testWriteToFile() throws Exception {
        //File cacheFile = new File("build/resolver.cache");
        File cacheFile = getResolverCache();
        resolver.writeToFile(cacheFile);
        File testFile = new File(getClass().getClassLoader().
                getResource("resolver.cache.test").toURI());

        assertEquals("The files differ!", FileUtils.readFileToString(testFile, "utf-8"), FileUtils.readFileToString(cacheFile, "utf-8"));
    }

    private File getResolverCache() throws IOException {
        return File.createTempFile("resolver", "cache");
    }

    public void testFileRoundTrip() throws Exception {
        File f = getResolverCache();
        resolver.writeToFile(f);

        IdResolver readFromFile = new IdResolver();
        readFromFile.populateFromFile(f);
        assertEquals(resolver.orgIdMaps, readFromFile.orgIdMaps);
        assertEquals(resolver.orgMainMaps, readFromFile.orgMainMaps);
        assertEquals(resolver.orgSynMaps, readFromFile.orgSynMaps);
    }
}
