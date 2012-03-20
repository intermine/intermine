package org.intermine.webservice.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.web.search.KeywordSearchResult;
import org.json.JSONObject;

public class QuickSearchJSONProcessor implements QuickSearchResultProcessor {

    @Override
    public List<String> formatResult(KeywordSearchResult result, boolean hasNext) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("type", result.getType());
        data.put("id", result.getId());
        data.put("relevance", result.getScore());
        data.put("fields", result.getFieldValues());
        JSONObject jo = new JSONObject(data);
        List<String> ret = new ArrayList<String>();
        ret.add(jo.toString());
        if (hasNext) {
            ret.add("");
        }
        return ret;
    }

}
