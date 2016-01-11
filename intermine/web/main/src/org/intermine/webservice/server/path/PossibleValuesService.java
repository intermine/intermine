package org.intermine.webservice.server.path;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.split;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A service for retrieving the possible values for a path in the data model.
 * @author Alex Kalderimis
 *
 */
public class PossibleValuesService extends JSONService
{

    private static final String TYPE_CONSTRAINTS_SHOULD_BE_JSON =
            "The value of 'typeConstraints' should be a json string";
    private static final String TYPE_CONSTRAINTS_ARE_STRINGS =
            "The typeConstraints object may only have strings as values";
    private static final int DEFAULT_BATCH_SIZE = 5000;

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
    protected Format getDefaultFormat() {
        return Format.OBJECTS;
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
            case OBJECTS:
            case JSON:
            case TEXT:
                return true;
            default:
                return false;
        }
    }

    private boolean count = false;
    private Path path;

    @Override
    protected void initState() {
        super.initState();
        count = WebServiceRequestParser.isCountRequest(request);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attrs = super.getHeaderAttributes();
        if (count) {
            attrs.put(JSONFormatter.KEY_INTRO, "\"count\":");
        } else {
            attrs.put(JSONFormatter.KEY_INTRO, "\"results\":[");
            attrs.put(JSONFormatter.KEY_OUTRO, "]");
        }
        return attrs;
    }

    @Override
    protected void postInit() {
        super.postInit();

        String pathString = getRequiredParameter("path").trim();

        addOutputInfo("path", pathString);

        String typeConstraintStr = getOptionalParameter("typeConstraints", "{}");
        Map<String, String> typeMap = new HashMap<String, String>();

        JSONObject typeJO;
        try {
            typeJO = new JSONObject(typeConstraintStr);
        } catch (JSONException e) {
            throw new BadRequestException(TYPE_CONSTRAINTS_SHOULD_BE_JSON);
        }

        @SuppressWarnings("unchecked")
        Iterator<String> it = (Iterator<String>) typeJO.keys();
        while (it.hasNext()) {
            String name = it.next();
            String subType;
            try {
                subType = typeJO.getString(name);
            } catch (JSONException e) {
                throw new BadRequestException(TYPE_CONSTRAINTS_ARE_STRINGS);
            }
            typeMap.put(name, subType);
        }

        try {
            if (typeMap.isEmpty()) {
                path = new Path(model, pathString);
            } else {
                path = new Path(model, pathString, typeMap);
            }
        } catch (PathException e) {
            throw new BadRequestException("Bad path given: " + pathString, e);
        }

    }

    @Override
    protected void execute() throws Exception {

        Query q = new Query();

        addOutputInfo("class", path.getLastClassDescriptor().getUnqualifiedName());
        addOutputInfo("field", path.getLastElement());

        String type = ((AttributeDescriptor) path.getEndFieldDescriptor()).getType();
        String[] parts = split(type, '.');
        addOutputInfo("type", parts[parts.length - 1]);

        QueryClass qc = new QueryClass(path.getPrefix().getEndType());
        q.addFrom(qc);

        QueryField qf1 = new QueryField(qc, path.getLastElement());
        q.addToSelect(qf1);

        QueryFunction qf = new QueryFunction();
        q.addToSelect(qf);
        q.addToGroupBy(qf1);

        int total = im.getObjectStore().count(q, ObjectStore.SEQUENCE_IGNORE);

        if (count) {
            addResultValue(total, false);
        } else {
            addOutputInfo("count", Integer.toString(total));

            Results results = im.getObjectStore().execute(q, DEFAULT_BATCH_SIZE, true, true, false);
            Iterator<Object> iter = results.iterator();

            while (iter.hasNext()) {
                @SuppressWarnings("rawtypes")
                List row = (List) iter.next();
                Map<String, Object> jsonMap = new HashMap<String, Object>();
                jsonMap.put("value", row.get(0));
                jsonMap.put("count", row.get(1));
                addResultItem(jsonMap, iter.hasNext());
            }
        }

    }

}
