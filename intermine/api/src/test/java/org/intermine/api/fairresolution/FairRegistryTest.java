package org.intermine.api.fairresolution;

import org.junit.Assert;
import org.junit.Test;

public class FairRegistryTest {

    @Test
    public void testAddMapping() {
        FairRegistry fr = new FairRegistry();

        Assert.assertNull(fr.resolve("ensembl", "ENSG00000092054"));

        fr.add("ensembl", "ENSG00000092054", 1);
        Assert.assertEquals(1, fr.resolve("ensembl", "ENSG00000092054").intValue());

        // Overwriting the mapping simply replaces
        fr.add("ensembl", "ENSG00000092054", 3);
        Assert.assertEquals(3, fr.resolve("ensembl", "ENSG00000092054").intValue());

        // PrefixRegistry should be case insensitive
        fr.add("eNSEMBL", "ENSG1234", 11);
        Assert.assertEquals(11, fr.resolve("ensembl", "ENSG1234").intValue());


        // But local IDs should not
        fr.add("ensembl", "ensg1234", 13);
        Assert.assertEquals(11, fr.resolve("ensembl", "ENSG1234").intValue());
        Assert.assertEquals(13, fr.resolve("ensembl", "ensg1234").intValue());
    }

    @Test
    public void testSize() {
        FairRegistry fr = new FairRegistry();
        Assert.assertEquals(0, fr.idsSize());
        Assert.assertEquals(0, fr.prefixesSize());

        fr.add("a", "a1", 1);
        Assert.assertEquals(1, fr.idsSize());
        Assert.assertEquals(1, fr.prefixesSize());

        fr.add("a", "a2", 2);
        fr.add("b", "b1", 3);
        Assert.assertEquals(2, fr.prefixesSize());

        Assert.assertEquals(3, fr.idsSize());

        // Replace shouldn't change the size
        fr.add("a", "a2", 4);
        Assert.assertEquals(3, fr.idsSize());
        Assert.assertEquals(2, fr.prefixesSize());
    }

    @Test
    public void testResolve() {
        FairRegistry fr = new FairRegistry();
        fr.add("ensembl", "ENSG00000092054", 1);

        Assert.assertEquals(1, fr.resolve("ensembl", "ENSG00000092054").intValue());

        // Allow prefixes to have any combination of case
        Assert.assertEquals(1, fr.resolve("eNSEMBl", "ENSG00000092054").intValue());

        // But not the local unique identifiers
        Assert.assertNull(fr.resolve("eNSEMBl", "ensg00000092054"));

        // Not found case
        Assert.assertNull(fr.resolve("garbage", "flatfoot"));
    }
}
