package org.intermine.api.search;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A single item in a set or returned results formed by searching the web-searchable indices with
 * Lucene.
 * @author ajk59
 *
 */
public final class SearchResult
{

    private final WebSearchable ws;
    private final Float score;
    private final String highlightedDesc;
    private final List<String> tags = new ArrayList<String>();

    /**
     * Constructor.
     *
     * @param ws The web searchable found.
     * @param score A Lucene score to associate with it. If it is above 1,
     *              it will be normalised to 1.
     * @param highlightedDesc A description (potentially with highlighting).
     * @param tags tags to search for
     */
    SearchResult(WebSearchable ws, Float score, String highlightedDesc, Collection<String> tags) {
        this.ws = ws;
        // different versions of Lucene return non-normalized results, see:
        //  http://stackoverflow.com/questions/4642160/
        //  cap the top hits
        if (score != null && score > 1) {
            this.score = Float.valueOf(1);
        } else {
            this.score = score;
        }
        this.highlightedDesc = highlightedDesc;
        this.tags.addAll(tags);
    }

    /**
     * Get the web searchable item that was found.
     * @return The item included in the search results.
     */
    public WebSearchable getItem() {
        return ws;
    }

    /**
     * A list of between two and three items containing the information in this result.
     * @return A list.
     */
    public List<Object> asList() {
        List<Object> ret = new ArrayList<Object>();
        ret.add(ws.getName());
        ret.add(highlightedDesc);
        ret.add(score);
        ret.add(tags);
        return ret;
    }

    @Override
    public String toString() {
        return String.format(
            "SearchResult Object {\n\titem:\t%s:%s\n\tdesc:\t'%s'\n\tscore:\t%.3f\n\ttags:\t%s\n}",
                ws.getTagType(), ws.getName(), highlightedDesc, score, tags.toString());
    }
}
