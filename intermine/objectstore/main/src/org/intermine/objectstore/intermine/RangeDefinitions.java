package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Hold definitions of Postgres range columns in the database. A range is defined by the table it
 * is in, the columns of the table included in the range (start and end), the name of the range
 * column itself and the type of range used (e.g. int8range). These definitions are required by
 * the SqlGenerator to determine whether it can use range columns for overlap queries.
 * @author rns
 *
 */
public class RangeDefinitions
{
    Set<Map<String, String>> ranges = new HashSet<Map<String, String>>();

    /**
     * Construct an empty holder for range definitions.
     */
    public RangeDefinitions() {
    }

    /**
     * Construct with a JSON string defining a list of ranges. The JSON will usually have been
     * written by the toJSON method of this class. JSON example:
     *   {'ranges': [{"tableName": "location",
     *                "rangeColName": "intermine_locrange",
     *                "rangeType": "int8range",
     *                "startCol": "start",
     *                "endCol": "end"}
     *              ]}}
     * @param input a JSON string defining range columns
     */
    public RangeDefinitions(String input) {
        if (input != null) {
            try {
                JSONObject j = new JSONObject(input);
                JSONArray rangesArray = j.getJSONArray("ranges");
                for (int i = 0; i < rangesArray.length(); i++) {
                    JSONObject rangeObj = rangesArray.getJSONObject(i);
                    HashMap<String, String> range = new HashMap<String, String>();
                    Iterator<String> keyIter = rangeObj.keys();
                    while (keyIter.hasNext()) {
                        String key = keyIter.next();
                        range.put(key,  stripPrefix(rangeObj.getString(key)));
                    }
                    ranges.add(range);
                }
            } catch (JSONException e) {
                throw new IllegalArgumentException("Failed to parse JSON range definition: "
                        + input + " ERROR: " + e.getMessage());
            }
        }
    }

    /**
     * Add a definition of a new range column. tableName, startCol and endCol can be specified
     * with or without the "intermine_" prefix (if they have them), the prefixes will be removed
     * for comparisons.
     * @param tableName database table name
     * @param rangeColName name of the range column in the table
     * @param rangeType Postgres type of the range, e.g. int8range
     * @param startCol column in table that is the start of the range
     * @param endCol column in table that is the end of the range
     */
    public void addRange(String tableName, String rangeColName, String rangeType,
            String startCol, String endCol) {
        //Range range = new Range(tableName, rangeColName, rangeType, startCol, endCol);
        HashMap<String, String> range = new HashMap<String, String>();
        range.put("tableName", tableName);
        range.put("rangeColName", rangeColName);
        range.put("rangeType", rangeType);
        range.put("startCol", startCol);
        range.put("endCol", endCol);

        ranges.add(range);
    }

    /**
     * Given a table name and columns in the table for start field and end field return any ranges
     * that have added to the table that work on these columns. The inputs can be given with or
     * without an "intermine_" prefix (it they have them)
     * @param tableName the database table to look for ranges in
     * @param startField table column that is the start of the range
     * @param endField table column that is the end of the range.
     * @return a range definition if found or null
     */
    public Map<String, String> getRange(String tableName, String startField, String endField) {
        for (Map<String, String> range : ranges) {
            if (tableName.toLowerCase().equals(range.get("tableName").toLowerCase())
                    && stripPrefix(startField.toLowerCase())
                        .equals(range.get("startCol").toLowerCase())
                    && stripPrefix(endField.toLowerCase())
                        .equals(range.get("endCol").toLowerCase())) {
                return range;
            }
        }
        return null;
    }

    private String stripPrefix(String s) {
        String imPrefix = "intermine_";
        return s.startsWith(imPrefix) ? s.substring(imPrefix.length()) : s;
    }

    /**
     * Return a JSON representation of configured ranges.
     * @return a JSON string representing the configure ranges
     */
    public String toJson() {
        try {
            JSONObject j = new JSONObject();
            JSONArray a = new JSONArray(ranges);
            j.put("ranges", a);
            return j.toString();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to write RangeDefitions to JSON"
                    + ranges + " ERROR: " + e.getMessage());
        }
    }

    /**
     * Return a JSON representation of configured ranges.
     * @return a JSON string representing the configure ranges
     */
    public String toString() {
        return toJson();
    }
}
