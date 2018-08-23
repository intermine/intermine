package org.intermine.api.fairresolution;

import org.junit.Assert;
import org.junit.Test;

public class FairResolverTest {
    @Test
    public void testResolve() {
        FairResolver fr = new FairResolver();

        // Found case
        {
            int id = fr.resolve("Ensembl", "ENSG00000092054");
            Assert.assertEquals(id, 1);
        }

        // Not found case
        {
            Integer id = fr.resolve("garbage", "flatfoot");
            Assert.assertNull(id);
        }
    }
}
