package org.intermine.webservice.server.query;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class QueryStore {
    private static final List<String> queries = new ArrayList<String>();
    private static final Map<String, Integer> indexes = new HashMap<String, Integer>();

    public static int putQuery(String xml) {
        Integer id = indexes.get(xml);
        if (id != null) {
            return id;
        }
        try {
            PathQueryBinding.unmarshalPathQuery(new StringReader(xml), PathQuery.USERPROFILE_VERSION);
        } catch (Exception e) {
            String message = "XML is not well formatted.";
            throw new BadRequestException(message, e);
        }
        id = Integer.valueOf(queries.size());
        queries.add(xml);
        indexes.put(xml, id);
        return id.intValue();
    }

    public static String getQuery(String key) {
        Integer id;
        try {
            id = Integer.valueOf(key, 10);
        } catch (NumberFormatException e) {
            throw new BadRequestException("qid must be an integer", e);
        }
        try {
            return queries.get(id);
        } catch (IndexOutOfBoundsException e) {
            throw new BadRequestException("qid " + key + " not in QueryStore");
        }
    }

}
