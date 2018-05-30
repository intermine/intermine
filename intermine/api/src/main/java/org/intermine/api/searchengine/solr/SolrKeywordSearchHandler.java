package org.intermine.api.searchengine.solr;

import org.intermine.api.searchengine.KeywordSearchHandler;
import org.intermine.api.searchengine.SearchResults;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;

/**
 * Solr implementation of KeywordSearchHandler
 *
 * @author arunans23
 */

public class SolrKeywordSearchHandler implements KeywordSearchHandler
{

	@Override
	public SearchResults doFilteredSearch(String queryString) {
		return null;
	}
    
}
