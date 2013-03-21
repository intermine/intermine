package org.intermine.webservice.server.query;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class QueryStore {
    private static List<String> queries = new ArrayList<String>(1024);

    public static int putQuery(String xml) {
        try {
            PathQueryBinding.unmarshalPathQuery(new StringReader(xml), PathQuery.USERPROFILE_VERSION);
        } catch (Exception e) {
            String message = "XML is not well formatted.";
            throw new BadRequestException(message, e);
        }
        queries.add(xml);
        return queries.size() - 1;
    }

    public static String getQuery(String key) {
        int idx;
        try {
            idx = Integer.parseInt(key, 10);
        } catch (NumberFormatException e) {
            throw new BadRequestException("qid must be an integer", e);
        }
        String ret = queries.get(idx);
        if (ret == null) {
            throw new BadRequestException("qid " + key + " not in QueryStore");
        }
        return ret;
    }

}
