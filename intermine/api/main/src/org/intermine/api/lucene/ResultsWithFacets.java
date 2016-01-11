package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 *
 */
public class ResultsWithFacets
{

    private final Collection<KeywordSearchHit> results;
    private int totalHits;
    private final Collection<KeywordSearchFacet> facets;

    /**
     * @param results the hits
     * @param facets The facets
     * @param totalHits count of search results
     */
    public ResultsWithFacets(
            Collection<KeywordSearchHit> results,
            Collection<KeywordSearchFacet> facets,
            int totalHits) {
        this.results = results;
        this.facets = facets;
        this.totalHits = totalHits;
    }

    /** @return the hits **/
    public Collection<KeywordSearchHit> getHits() {
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
