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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.web.search.KeywordSearchResult;

/**
 * A result processor that outputs results as plain text.
 * @author Alex Kalderimis
 *
 */
public class QuickSearchTextProcessor implements QuickSearchResultProcessor
{

    private final String nl;
    private static final String TAB = "    ";

    /** @param separator The client's line separator **/
    public QuickSearchTextProcessor(String separator) {
        this.nl = separator;
    }

    @Override
    public List<String> formatResult(KeywordSearchResult result, boolean hasNext) {
        List<String> ret = new ArrayList<String>();
        Map<String, Object> data = new HashMap<String, Object>(result.getFieldValues());
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("# %s:%d (%.3f) - ",
                result.getType(), result.getId(), result.getScore()));
        sb.append(nl);
        for (Entry<String, Object> kv: data.entrySet()) {
            sb.append(TAB);
            sb.append(kv.getKey());
            sb.append(":").append(TAB);
            sb.append(kv.getValue());
            sb.append(nl);
        }
        sb.append(nl);
        ret.add(sb.toString());
        return ret;
    }

}
