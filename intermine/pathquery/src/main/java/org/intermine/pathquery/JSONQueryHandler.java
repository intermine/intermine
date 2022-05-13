package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Handle parsing JSON queries and template queries
 *
 * @author Julie Sullivan
 */
public final class JSONQueryHandler
{


    private JSONQueryHandler() {
        // don't instantiate
    }

    /**
     * Parse PathQuery from JSON
     * @param model the data model
     * @param jsonQueryString the query in JSON format
     * @return a Map from query name to PathQuery
     * @throws JSONException if poorly formatted JSON
     */
    public static PathQuery parse(Model model, String jsonQueryString) throws JSONException {
        return parse(null, model, jsonQueryString);
    }

    /**
     * Parse PathQuery from JSON. Handles template constraints too.
     *
     * @param model the data model
     * @param jsonQueryString the query in JSON format
     * @param templateQuery template associate with query. may be null.
     * @return a Map from query name to PathQuery
     * @throws JSONException if poorly formatted JSON
     */
    public static PathQuery parse(TemplateQuery templateQuery, Model model, String jsonQueryString)
        throws JSONException {
        JSONObject obj = new JSONObject(jsonQueryString);
        PathQuery query = new PathQuery(model);
        String name = "query";
        if (obj.has("name")) {
            name = obj.getString("name");
        }
        query.setTitle(name);

        // SELECT statement
        JSONArray viewPaths = obj.getJSONArray("select");
        for (int i = 0; i < viewPaths.length(); i++) {
            query.addView(viewPaths.getString(i));
        }

        // WHERE statement
        setConstraints(templateQuery, obj, query);

        // outer join status
        if (obj.has("joins")) {
            JSONArray outerJoins = obj.getJSONArray("joins");
            for (int i = 0; i < outerJoins.length(); i++) {
                query.setOuterJoinStatus(outerJoins.getString(i), OuterJoinStatus.OUTER);
            }
        }

        // constraint logic
        if (obj.has("constraintLogic")) {
            String constraintLogic = obj.getString("constraintLogic");
            query.setConstraintLogic(constraintLogic);
        }

        // description
        if (obj.has("description")) {
            String description = obj.getString("description");
            query.setDescription(description);
        }

        // order by
        if (obj.has("orderBy")) {
            JSONArray orderBys = obj.getJSONArray("orderBy");
            for (int i = 0; i < orderBys.length(); i++) {
                JSONObject orderBy = orderBys.getJSONObject(i);
                Iterator keys = orderBy.keys();
                while (keys.hasNext()) {
                    String orderPath = (String) keys.next();
                    String direction = orderBy.getString(orderPath);
                    if ("desc".equalsIgnoreCase(direction)) {
                        query.addOrderBy(orderPath, OrderDirection.DESC);
                    } else {
                        query.addOrderBy(orderPath, OrderDirection.ASC);
                    }
                }
            }
        }

        return query;
    }

    private static void setConstraints(TemplateQuery templateQuery, JSONObject obj,
        PathQuery query) throws JSONException {
        // WHERE statement
        if (obj.has("where")) {
            JSONArray constraints = obj.getJSONArray("where");
            for (int i = 0; i < constraints.length(); i++) {
                JSONObject constraintObj = constraints.getJSONObject(i);
                PathConstraint constraint = null;
                String path = constraintObj.getString("path");
                if (constraintObj.has("op")) {
                    String op = constraintObj.getString("op");
                    ConstraintOp constraintOp = ConstraintOp.getConstraintOp(
                            constraintObj.getString("op"));
                    String code = constraintObj.getString("code");

                    // for template queries only
                    if (templateQuery != null) {
                        if (constraintObj.has("editable")) {
                            String editable = constraintObj.getString("editable");
                            if ("true".equalsIgnoreCase(editable)) {
                                templateQuery.setEditable(constraint, true);
                            }
                        }
                        if (constraintObj.has("switchable")) {
                            String switchable = constraintObj.getString("switchable");
                            if ("on".equalsIgnoreCase(switchable)) {
                                templateQuery.setSwitchOffAbility(constraint, SwitchOffAbility.ON);
                            } else if ("off".equals(switchable)) {
                                templateQuery.setSwitchOffAbility(constraint, SwitchOffAbility.OFF);
                            }
                        }
                        if (constraintObj.has("description")) {
                            String description = constraintObj.getString("description");
                            templateQuery.setConstraintDescription(constraint, description);
                        }
                    }

                    String value = null;
                    JSONArray values = null;
                    JSONArray idArray = null;
                    if (constraintObj.has("value")) {
                        value = constraintObj.getString("value");
                    } else if (constraintObj.has("values"))  {
                        values = constraintObj.getJSONArray("values");
                    } else if (constraintObj.has("ids"))  {
                        // if the constraint doesn't have a list name, it will have a set of ids
                        idArray = constraintObj.getJSONArray("ids");
                    }

                    if ("IN".equals(op) || "NOT IN".equals(op)) {
                        if (StringUtils.isNotEmpty(value)) {
                            constraint = new PathConstraintBag(path, constraintOp, value);
                        } else if (idArray != null) {
                            List<Integer> ids = new ArrayList<Integer>();
                            for (int j = 0; j < idArray.length(); j++) {
                                String id = idArray.get(j).toString();
                                ids.add(Integer.parseInt(id));
                            }
                            constraint = new PathConstraintIds(path, constraintOp, ids);
                        }
                    } else if ("LOOKUP".equals(op)) {
                        String extraValue = null;
                        if (constraintObj.has("extraValue")) {
                            extraValue = constraintObj.getString("extraValue");
                        }
                        constraint = new PathConstraintLookup(path, value, extraValue);
                    } else if ("ONE OF".equals(op) || "NONE OF".equals(op)) {
                        List<String> oneOfValues = new ArrayList<String>();
                        for (int j = 0; j < values.length(); j++) {
                            oneOfValues.add(values.get(j).toString());
                        }
                        constraint = new PathConstraintMultiValue(path, constraintOp, oneOfValues);
                    } else if ("IS NULL".equals(op) || "IS NOT NULL".equals(op)) {
                        constraint = new PathConstraintNull(path, constraintOp);
                    } else if ("WITHIN".equals(op) || "OVERLAPS".equals(op)
                        || "DOES NOT OVERLAP".equals(op) || "OUTSIDE".equals(op)) {
                        List<String> ranges = new ArrayList<String>();
                        for (int j = 0; j < values.length(); j++) {
                            ranges.add(values.get(j).toString());
                        }
                        constraint = new PathConstraintRange(path, constraintOp, ranges);
                    } else if ("ISA".equals(op) || "ISNT".equals(op)) {
                        List<String> types = new ArrayList<String>();
                        for (int j = 0; j < values.length(); j++) {
                            types.add(values.get(j).toString());
                        }
                        constraint = new PathConstraintMultitype(path, constraintOp, types);
                    } else {
                        if (constraintObj.has("loopPath")) {
                            String loopPath = constraintObj.getString("loopPath");
                            constraint = new PathConstraintLoop(path, constraintOp, loopPath);
                        } else {
                            constraint = new PathConstraintAttribute(path, constraintOp, value);
                        }
                    }
                    query.addConstraint(constraint, code);
                } else if (constraintObj.has("type")) {
                    String type = constraintObj.getString("type");
                    // subclass
                    constraint = new PathConstraintSubclass(path, type);
                    query.addConstraint(constraint);
                }
            }
        }
    }

}
