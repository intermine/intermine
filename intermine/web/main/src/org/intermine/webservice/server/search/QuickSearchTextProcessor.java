package org.intermine.webservice.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.web.search.KeywordSearchResult;

public class QuickSearchTextProcessor implements QuickSearchResultProcessor {

    private final String NL;
    private final static String TAB = "    ";

    public QuickSearchTextProcessor(String separator) {
        this.NL = separator;
    }

    @Override
    public List<String> formatResult(KeywordSearchResult result, boolean hasNext) {
        List<String> ret = new ArrayList<String>();
        Map<String, Object> data = new HashMap<String, Object>(result.getFieldValues());
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("# %s:%d (%.3f) - ",
                result.getType(), result.getId(), result.getScore()));
        sb.append(NL);
        for (Entry<String, Object> kv: data.entrySet()) {
            sb.append(TAB);
            sb.append(kv.getKey());
            sb.append(":").append(TAB);
            sb.append(kv.getValue());
            sb.append(NL);
        }
        sb.append(NL);
        ret.add(sb.toString());
        return ret;
    }

}
