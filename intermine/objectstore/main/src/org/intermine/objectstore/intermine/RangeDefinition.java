package org.intermine.objectstore.intermine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class RangeDefinition {

    // TODO add HashSet of Ranges
    Set<Map<String, String>> ranges = new HashSet<Map<String, String>>();
    // TODO range types should be an emum of strings

    // define range columns in database as:
    // tableName    rangeColName    rangeType   startColName    endColName
    public RangeDefinition() {

    }


    public RangeDefinition(String input) throws JSONException {
        if (input != null) {
            JSONObject j = new JSONObject(input);
            JSONArray rangesArray = j.getJSONArray("ranges");
            for (int i = 0; i < rangesArray.length(); i++) {
                JSONObject rangeObj = rangesArray.getJSONObject(i);

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

    public String toJson() throws JSONException {
        JSONObject j = new JSONObject();
        JSONArray a = new JSONArray(ranges);
        j.put("ranges", a);
        return j.toString();
    }

    private class Range
    {
        Map<String, String> values = new HashMap<String, String>();

        public Range(String tableName, String rangeColName, String rangeType, String startCol,
                String endCol) {
            values.put("tableName", tableName);
            values.put("rangeColName", rangeColName);
            values.put("rangeType", rangeType);
            values.put("startCol", startCol);
            values.put("endCol", endCol);
        }

        // create range from json input
        public Range(String input) {
            //
        }

        public JSONObject toJson() {
            return new JSONObject(values);
        }
    }
}
