package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

/**
 * A class that wraps both a collection of hits and a collection of facets.
 * @author Alex Kalderimis
 * @author arunans23
 *
 */
public class KeywordSearchResults
{

    private final Collection<KeywordSearchResultContainer> results;
    private int totalHits;
    private final Collection<KeywordSearchFacet> facets;

    /**
     * @param results the hits
     * @param facets The facets
     * @param totalHits count of search results
     */
    public KeywordSearchResults(
            Collection<KeywordSearchResultContainer> results,
            Collection<KeywordSearchFacet> facets,
            int totalHits) {
        this.results = results;
        this.facets = facets;
        this.totalHits = totalHits;
    }

    /** @return the hits **/
    public Collection<KeywordSearchResultContainer> getHits() {
        return results;
    }

    /** @return the facets **/
    public Collection<KeywordSearchFacet> getFacets() {
        return facets;
    }

    /** @return the totalHits **/
    public int getTotalHits() {
        return totalHits;
    }
}
