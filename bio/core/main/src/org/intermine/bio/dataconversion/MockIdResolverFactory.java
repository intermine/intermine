package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A mock IdResolver factory needed for testing.
 * @author rns
 */
public class MockIdResolverFactory extends IdResolverFactory
{
    /**
     * Construct with class name for mock IdResolver
     * @param clsName the type to resolve
     */
    public MockIdResolverFactory(String clsName) {
        if (resolver == null) {
            resolver = new IdResolver(clsName);
        }
    }

    /**
     * Create a MockIdResolver
     * @return a MockIdResolver
     */
    public IdResolver getIdResolver() {
        return resolver;
    }

    /**
     * Create mock entries for the IdResolver, these will be added when getIdResolver
     * is called.
     * @param taxonId the organism of identifiers
     * @param primaryId main identifier
     * @param synonyms synonyms for the main identifier
     */
//    public void addResolverEntry(String taxonId, String primaryId, Set<String> synonyms) {
//        resolver.addSynonyms(taxonId, primaryId, synonyms);
//    }

    @Override
    protected void createIdResolver() {
    }
}
