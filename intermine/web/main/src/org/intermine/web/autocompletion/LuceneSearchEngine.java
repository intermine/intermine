package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * LuceneSearchEngine for the autocompleter
 * @author Dominik Grimm
 * @author Michael Menden
 */
public class LuceneSearchEngine
{

    private IndexSearcher indexSearch;
    private Analyzer analyzer;

    /**
     * returns the indexSearcher so we can use IndexSearcher.doc(int) to get a
     * doc
     * @return IndexSearcher
     */
    public IndexSearcher getIndexSearch() {
        return indexSearch;
    }

    /**
     * LuceneSearchEngine constructor put the indexes to memory and creates an
     * keyword analyser
     * @param fileName
     *            of the Lucene Index files
     */
    public LuceneSearchEngine(String fileName) {
        try {
            RAMDirectory ram = new RAMDirectory(FSDirectory.open(new File(fileName)));

            indexSearch = new IndexSearcher(ram);

            analyzer = new KeywordAnalyzer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * LuceneSearchEngine constructor put the indexes to memory and creates an
     * keyword analyser
     * @param ram
     *            RAMDirectory
     */
    public LuceneSearchEngine(RAMDirectory ram) {
        try {
            indexSearch = new IndexSearcher(ram);

            analyzer = new KeywordAnalyzer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform the lucene search.
     * @param queryString
     *            the string for what you search in the indexes
     * @param toSearch
     *            the field in which you search
     * @return Hits list of documents (search results)
     * @throws IOException
     *             IOException
     * @throws ParseException
     *             IOException
     */
    public TopDocs performSearch(String queryString, String toSearch) throws IOException,
            ParseException {
        QueryParser parser = new QueryParser(Version.LUCENE_30, toSearch, analyzer);
        BooleanQuery.setMaxClauseCount(4096);

        if (!"".equals(queryString) && !queryString.trim().startsWith("*")) {

            Query query;

            if (queryString.endsWith(" ")) {
                queryString = queryString.substring(0, queryString.length() - 1);
            }

            String[] tmp;
            if (queryString.contains(" ")) {
                tmp = queryString.replaceAll(" +", " ").trim().split(" ");
                queryString = new String();

                for (int i = 0; i < tmp.length; i++) {
                    queryString += tmp[i];
                    if (i < tmp.length - 1) {
                        queryString += "* AND ";
                    }
                }
            }
            query = parser.parse(queryString + "*");

            return indexSearch.search(query, 500); // FIXME: hardcoded maximum
                                                   // number of results
        }

        return null;
    }

    /**
     * Perform the search but only return n results.
     * @param queryS
     *            the string for what you search in the indexes
     * @param toSearch
     *            the field in which you search
     * @param n
     *            first n results
     * @return array of ScoreDoc[] with n elements
     */
    public String[] fastSearch(String queryS, String toSearch, int n) {

        QueryParser parser = new QueryParser(Version.LUCENE_30, toSearch, analyzer);
        BooleanQuery.setMaxClauseCount(4096);
        String status = "true";
        String[] results = null;

        if (!"".equals(queryS) && !queryS.trim().startsWith("*")) {
            Query query = null;
            if (queryS.endsWith(" ")) {
                queryS = queryS.substring(0, queryS.length() - 1);
            }

            String[] tmp;
            if (queryS.contains(" ")) {
                tmp = queryS.replaceAll(" +", " ").trim().split(" ");
                queryS = new String();

                for (int i = 0; i < tmp.length; i++) {
                    queryS += tmp[i];
                    if (i < tmp.length - 1) {
                        queryS += "* AND ";
                    }
                }
            }

            try {
                query = parser.parse(queryS + "*");
                TopDocs topDoc = null;
                try {
                    topDoc = indexSearch.search(query, null, n);
                    ScoreDoc[] docs = topDoc.scoreDocs;

                    results = new String[docs.length + 1];
                    for (int i = 1; i < docs.length + 1; i++) {
                        try {
                            results[i] = indexSearch.doc(docs[i - 1].doc).get(toSearch);
                        } catch (IOException e) {
                            status = "No results! Please try again.";
                        }
                    }
                    results[0] = status;
                } catch (IOException e) {
                    status = "Please type in more characters to get results.";
                    results = new String[1];
                    results[0] = status;
                } catch (Throwable e1) {
                    status = "Please type in more characters to get results.";
                    results = new String[1];
                    results[0] = status;
                }
            } catch (ParseException e) {
                status = "No results! Please try again.";
            }

            return results;
        }

        return null;
    }
}
