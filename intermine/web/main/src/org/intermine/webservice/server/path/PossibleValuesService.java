package org.intermine.webservice.server.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.commons.lang.ArrayUtils.reverse;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.metadata.AttributeDescriptor;
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
    private Map<String, String> kvPairs = new HashMap<String, String>();
    private static Logger logger 
        = Logger.getLogger(PossibleValuesService.class);

    /**
     * A service for providing column summary information. This information is
     * designed to be used in forms that provide autocomplete or drop-down options
     * to suggest possible values. It expects a path (such as Employee.name, or
     * CEO.company.departments.employees.name), and lists all possible values
     * that path may represent, along with their frequency. The values list is sorted
     * by the value, not the frequency.
     *
     * @param im A reference to the InterMine API bundle.
     */
    public PossibleValuesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return JSONP_OBJ_FORMAT;
        } else {
            return JSON_OBJ_FORMAT;
        }
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("path", "UNKNOWN");
        kvPairs.put("path", "UNKNOWN");
        if (formatIsCount()) {
            attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
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

        if (isEmpty(pathString)) {
            throw new BadRequestException("No path provided");
        } 
        attributes.put("path", pathString);
        kvPairs.put("path", pathString);

        String typeConstraintStr = request.getParameter("typeConstraints");
        Map<String, String> typeMap = new HashMap<String, String>();
        if (!isEmpty(typeConstraintStr)) {
            logger.debug(typeConstraintStr);
            JSONObject typeJO = new JSONObject(typeConstraintStr);
            Iterator<String> it = (Iterator<String>) typeJO.keys();
            while (it.hasNext()) {
                String name = it.next();
                String subType = typeJO.getString(name);
                typeMap.put(name, subType);
            }
        }

        Model model = im.getModel();

        Path path;
        try {
            if (typeMap.isEmpty()) {
                path = new Path(model, pathString);
            } else {
                path = new Path(model, pathString, typeMap);
            }
        } catch (PathException e) {
           throw new BadRequestException("Bad path given: " + pathString, e);
        }

        Query q = new Query();

        attributes.put("class", 
                path.getLastClassDescriptor().getUnqualifiedName());
        attributes.put("field", path.getLastElement());
        String type = 
          ((AttributeDescriptor) path.getEndFieldDescriptor()).getType();
        String[] parts = split(type, '.');
        reverse(parts);
        attributes.put("type", parts[0]);

        QueryClass qc = new QueryClass(path.getPrefix().getEndType());
        q.addFrom(qc);

        QueryField qf1 = new QueryField(qc, path.getLastElement());
        q.addToSelect(qf1);

        QueryFunction qf = new QueryFunction();
        q.addToSelect(qf);
        q.addToGroupBy(qf1);

        int count = im.getObjectStore().count(q, ObjectStore.SEQUENCE_IGNORE);

        if (formatIsCount()) {
            output.addResultItem(Arrays.asList(String.valueOf(count)));
        } else {
            attributes.put("count", count);

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
                    // Standard hack to ensure commas
                    forOutput.add("");
                }
                output.addResultItem(forOutput);
            }
        }

    }

}
