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
    public void testResolve() {
        FairResolver fr = new FairResolver();
        fr.addMapping("ensembl", "ENSG00000092054", 1);

        // Found case
        {
            int id = fr.resolve("Ensembl", "ENSG00000092054");
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
