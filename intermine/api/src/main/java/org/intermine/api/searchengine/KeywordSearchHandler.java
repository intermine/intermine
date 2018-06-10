package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for handling indexes.
 *
 * @author arunans23
 */

public interface KeywordSearchHandler
{
    public KeywordSearchResults doKeywordSearch(InterMineAPI im, String queryString, Map<String,
            String> facetValues, List<Integer> ids, int offSet);

    public Set<Integer> getObjectIdsFromSearch(InterMineAPI im, String searchString, int offset,
                                               Map<String, String> facetValues, List<Integer> ids);

}
