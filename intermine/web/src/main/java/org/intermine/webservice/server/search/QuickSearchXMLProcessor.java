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

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;

/**
 * A result processor that outputs results as XML.
 * @author Alex Kalderimis
 *
 */
public class QuickSearchXMLProcessor implements QuickSearchResultProcessor
{

    private static final String TAG_NAME = "result";
    private static final String FIELD_TAG = "field";

    @Override
    public List<String> formatResult(KeywordSearchResult result, boolean hasNext) {
        Map<String, Object> data = new HashMap<String, Object>(result.getFieldValues());
        List<String> ret = new ArrayList<String>();
        StringBuffer sb = new StringBuffer(
                String.format("<%s type=\"%s\" id=\"%d\" score=\"%f\">",
                     TAG_NAME, result.getType(), result.getId(), result.getScore()));
        for (Entry<String, Object> kv: data.entrySet()) {
            sb.append(String.format("<%s name=\"%s\">", FIELD_TAG, escapeXml(kv.getKey())));
            sb.append(escapeXml(kv.getValue() + ""));
            sb.append(String.format("</%s>", FIELD_TAG));
        }
        sb.append(String.format("</%s>", TAG_NAME));

        ret.add(sb.toString());

        return ret;
    }

}
