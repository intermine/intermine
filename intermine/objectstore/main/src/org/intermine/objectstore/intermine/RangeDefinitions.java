package org.intermine.objectstore.intermine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class RangeDefinitions
{
    Set<Map<String, String>> ranges = new HashSet<Map<String, String>>();
    // TODO range types should be an emum of strings

    public RangeDefinitions() {
    }

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
                        range.put(key,  rangeObj.getString(key));
                    }
                    ranges.add(range);
                }
            } catch (JSONException e) {
                throw new IllegalArgumentException("Failed to parse JSON range definition: "
                        + input + " ERROR: " + e.getMessage());
            }
        }
    }

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

    public boolean rangeExists(String tableName, String startField, String endField) {
        return getRange(tableName, startField, endField) != null;
    }

    public Map<String, String> getRange(String tableName, String startField, String endField) {
        for (Map<String, String> range : ranges) {
            if (tableName.toLowerCase().equals(range.get("tableName").toLowerCase())
                    && startField.toLowerCase().equals(range.get("startCol").toLowerCase())
                    && endField.toLowerCase().equals(range.get("endCol").toLowerCase())) {
                return range;
            }
        }
        return null;
    }

    public String getRangeType(String tableName, String startField, String endField) {
        Map<String, String> range = getRange(tableName, startField, endField);
        if (range != null) {
            return range.get("rangeType");
        }
        return null;
    }

    public String getRangeColName(String tableName, String startField, String endField) {
        Map<String, String> range = getRange(tableName, startField, endField);
        if (range != null) {
            return range.get("rangeColName");
        }
        return null;
    }

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
}
