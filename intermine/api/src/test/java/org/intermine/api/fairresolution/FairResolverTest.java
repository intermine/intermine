package org.intermine.api.fairresolution;

import org.junit.Assert;
import org.junit.Test;

public class FairResolverTest {

    @Test
    public void testAddMapping() {
        FairResolver fr = new FairResolver();

        Assert.assertNull(fr.resolve("ensembl", "ENSG00000092054"));

        {
            fr.addMapping("ensembl", "ENSG00000092054", 1);
            int id = fr.resolve("ensembl", "ENSG00000092054");
            Assert.assertEquals(id, 1);
        }

        // Overwriting the mapping simply replaces
        {
            fr.addMapping("ensembl", "ENSG00000092054", 3);
            int id = fr.resolve("ensembl", "ENSG00000092054");
            Assert.assertEquals(id, 3);
        }

        // Prefixes should be case insensitive
        {
            fr.addMapping("eNSEMBL", "ENSG1234", 11);
            int id = fr.resolve("ensembl", "ENSG1234");
            Assert.assertEquals(id, 11);
        }

        // But local IDs should not
        {
            fr.addMapping("ensembl", "ensg1234", 13);
            int id = fr.resolve("ensembl", "ENSG1234");
            Assert.assertEquals(id, 11);
        }

        {
            int id = fr.resolve("ensembl", "ensg1234");
            Assert.assertEquals(id, 13);
        }
    }

    @Test
    public void testSize() {
        FairResolver fr = new FairResolver();
        Assert.assertEquals(0, fr.localUniqueIdsSize());
        Assert.assertEquals(0, fr.prefixesSize());

        fr.addMapping("a", "a1", 1);
        Assert.assertEquals(1, fr.localUniqueIdsSize());
        Assert.assertEquals(1, fr.prefixesSize());

        fr.addMapping("a", "a2", 2);
        fr.addMapping("b", "b1", 3);
        Assert.assertEquals(2, fr.prefixesSize());

        Assert.assertEquals(3, fr.localUniqueIdsSize());

        // Replace shouldn't change the size
        fr.addMapping("a", "a2", 4);
        Assert.assertEquals(3, fr.localUniqueIdsSize());
        Assert.assertEquals(2, fr.prefixesSize());
    }

    @Test
    public void testResolve() {
        FairResolver fr = new FairResolver();
        fr.addMapping("ensembl", "ENSG00000092054", 1);

        // Found case
        {
            int id = fr.resolve("ensembl", "ENSG00000092054");
            Assert.assertEquals(id, 1);
        }

        // Allow prefixes to have any combination of case
        {
            int id = fr.resolve("eNSEMBl", "ENSG00000092054");
            Assert.assertEquals(id, 1);
        }

        // But not the local unique identifiers
        {
            Integer id = fr.resolve("eNSEMBl", "ensg00000092054");
            Assert.assertNull(id);
        }

        // Not found case
        {
            Integer id = fr.resolve("garbage", "flatfoot");
            Assert.assertNull(id);
        }
    }
}
