package org.intermine.webservice.server.search;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.web.search.KeywordSearchResult;

/**
 * The type of objects that can handle keyword search results.
 * @author Alex Kalderimis
 *
 */
public interface QuickSearchResultProcessor
{

    /**
     * They take a results, and return one or more strings.
     * @param result The result
     * @param hasNext Whether there are more of them
     * @return Some strings.
     */
    List<String> formatResult(KeywordSearchResult result, boolean hasNext);
}
