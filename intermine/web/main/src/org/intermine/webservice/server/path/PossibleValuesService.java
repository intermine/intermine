package org.intermine.webservice.server.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONObject;

public class PossibleValuesService extends WebService {

    private static final int DEFAULT_BATCH_SIZE = 5000;

    public PossibleValuesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return JSONP_FORMAT;
        } else {
            return JSON_FORMAT;
        }
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON() && !formatIsCount()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"values\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        }
        return attributes;
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String pathString = request.getParameter("path");

        Map<String, Object> attributes = getHeaderAttributes();
        output.setHeaderAttributes(attributes);

        if (StringUtils.isEmpty(pathString)) {
            throw new BadRequestException("No path provided");
        }

        Model model = im.getModel();

        Path path;
        try {
            path = new Path(model, pathString);
        } catch (PathException e) {
           throw new BadRequestException("Bad path given: " + pathString, e);
        }

        Query q = new Query();

        QueryClass qc = new QueryClass(path.getPrefix().getEndType());
        q.addFrom(qc);

        QueryField qf1 = new QueryField(qc, path.getLastElement());
        q.addToSelect(qf1);

        QueryFunction qf = new QueryFunction();
        q.addToSelect(qf);
        q.addToGroupBy(qf1);

        if (formatIsCount()) {
            int count = im.getObjectStore().count(q, ObjectStore.SEQUENCE_IGNORE);
            output.addResultItem(Arrays.asList(String.valueOf(count)));
        } else {

            Results results = im.getObjectStore().execute(q, DEFAULT_BATCH_SIZE, true, true, false);
            Iterator<Object> iter = results.iterator();

            while (iter.hasNext()) {
                List row = (List) iter.next();
                Map<String, Object> jsonMap = new HashMap<String, Object>();
                jsonMap.put("value", row.get(0));
                jsonMap.put("count", row.get(1));
                JSONObject jo = new JSONObject(jsonMap);
                List<String> forOutput = new ArrayList<String>();
                forOutput.add(jo.toString());
                if (iter.hasNext()) {
                    forOutput.add("");
                }
                output.addResultItem(forOutput);
            }
        }

    }

}
