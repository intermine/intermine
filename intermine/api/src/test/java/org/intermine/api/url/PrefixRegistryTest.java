package org.intermine.api.url;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.junit.Assert;
import org.junit.Before;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PrefixRegistryTest
{
    PrefixRegistry registry = null;
    List<String> prefixes = Arrays.asList("ensembl", "fb", "go", "mim", "pubmed", "uniprot");

    @Before
    public void setUp(){
        registry = PrefixRegistry.getRegistry();
    }

    public void testGetPrefixes() {
        Set<String> prefixes = registry.getPrefixes();
        Assert.assertEquals(6, prefixes);
        for (String prefix : prefixes) {
            Assert.assertTrue(prefixes.contains(prefix));
        }
    }
}
