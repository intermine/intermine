package org.intermine.webservice.server.search;

import java.util.List;

import org.intermine.web.search.KeywordSearchResult;

public interface QuickSearchResultProcessor {

    List<String> formatResult(KeywordSearchResult result, boolean hasNext);
}
