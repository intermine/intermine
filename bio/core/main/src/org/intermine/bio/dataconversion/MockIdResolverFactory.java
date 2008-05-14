package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

/**
 * A mock IdResolver factory needed for testing.
 * @author rns
 */
public class MockIdResolverFactory extends IdResolverFactory
{
    private String clsName;
    private Set<ResolverEntry> mockEntries = new HashSet();
    
    /**
     * Construct with class name for mock IdResolver
     * @param clsName the type to resolve
     */
    public MockIdResolverFactory(String clsName) {
        this.clsName = clsName;
    }
    
    /**
     * Create a MockIdResolver
     * @return a MockIdResolver
     */
    public IdResolver getIdResolver() {
        IdResolver resolver = new IdResolver(clsName);
        for (ResolverEntry entry : mockEntries) {
            resolver.addEntry(entry.taxonId, entry.primaryId, entry.synonyms);
        }
        return resolver;
    }

    /**
     * Create mock entries for the IdResolver, these will be added when getIdResolver
     * is called.
     * @param taxonId the organism of identifiers
     * @param primaryId main identifier
     * @param synonyms synonms for the main identifier
     */
    public void addResolverEntry(String taxonId, String primaryId, Set<String> synonyms) {
        mockEntries.add(new ResolverEntry(taxonId, primaryId, synonyms));
    }

    private class ResolverEntry
    {
        String taxonId, primaryId;
        Set<String> synonyms;
    
        public ResolverEntry(String taxonId, String primaryId, Set<String> synonyms) {
            this.taxonId = taxonId;
            this.primaryId = primaryId;
            this.synonyms = synonyms;
        }
    }
}
