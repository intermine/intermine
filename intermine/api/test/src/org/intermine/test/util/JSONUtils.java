package org.intermine.test.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {

    public static Collection<String> jsonObjsAreEqual (
            JSONObject expected, JSONObject actual) throws JSONException {
        return jsonObjsAreEqual(expected, actual, "value");
    }
    public static Collection<String> jsonObjsAreEqual(
            JSONObject expected, JSONObject actual, String name) throws JSONException {
        if (expected == null) {
            throw new NullPointerException("Expected is null");
        }
        if (actual == null) {
            return Collections.singleton(name + " is null");
        }
        Set<String> problems = new HashSet<String>();

        List<String> l1 =  Arrays.asList(JSONObject.getNames(expected));
        Collections.sort(l1);
        List<String> l2 =  Arrays.asList(JSONObject.getNames(actual));
        Collections.sort(l2);
        Set<String> diff;
        diff = new HashSet<String>(l1);
        diff.removeAll(l2);
        if (diff.size() > 0) {
            problems.add("Expected the following properties in " + name + ": " + diff);
        }
        diff = new HashSet<String>(l2);
        diff.removeAll(l1);
        if (diff.size() > 0) {
            problems.add(name + " has the following extra properties: " + diff);
        }
        for (String key : l1) {
            Object val1 = expected.get(key);
            Object val2 = actual.get(key);
            String label = name + "." + key;
            if (val1 == null) {
                if (val2 != null) {
                    problems.add("Expected " + label + " to be null, but was " + val2);
                }
            } else if (val1 instanceof JSONArray) {
                if (!(val2 instanceof JSONArray)) {
                    problems.add("Expected " + label + " to be " + val1 + ", but was " + val2);
                } else {
                    problems.addAll(jsonArraysAreEqual((JSONArray) val1, (JSONArray) val2, label));
                }
            } else if (val1 instanceof JSONObject) {
                if (!(val2 instanceof JSONObject)) {
                    problems.add("Expected " + label + " to be " + val1 + ", but was " + val2);
                } else {
                    problems.addAll(jsonObjsAreEqual((JSONObject) val1, (JSONObject) val2, label));
                }
            } else {
                if (!val1.equals(val2)) {
                    problems.add("Expected " + label + " to be " + val1 + ", but was " + val2);
                }
            }
        }
        return problems;
    }
    private static Collection<? extends String> jsonArraysAreEqual(
            JSONArray expected, JSONArray actual, String name) throws JSONException {
        if (expected == null) {
            throw new NullPointerException("Expected is null");
        }
        if (actual == null) {
            return Collections.singleton(name + " is null");
        }
        Set<String> problems = new HashSet<String>();

        if (expected.length() != actual.length()) {
            problems.add("Expected " + expected.length() + " elements in " + name + ", but found " + actual.length());
        }
        int max = Math.max(expected.length(), actual.length());
        for (int i = 0; i < max; i++) {
            Object val1 = null;
            if (i < expected.length()) {
                val1 = expected.get(i);
            }
            Object val2 = null;
            if (i < actual.length()) {
                val2 = actual.get(i);
            }
            String label = name + "." + i;
            if (val1 == null) {
                if (val2 != null) {
                    problems.add("Expected " + label + " to be null, but was " + val2);
                }
            } else if (val1 instanceof JSONArray) {
                if (!(val2 instanceof JSONArray)) {
                    problems.add("Expected " + label + " to be " + val1 + ", but was " + val2);
                } else {
                    problems.addAll(jsonArraysAreEqual((JSONArray) val1, (JSONArray) val2, label));
                }
            } else if (val1 instanceof JSONObject) {
                if (!(val2 instanceof JSONObject)) {
                    problems.add("Expected " + label + " to be " + val1 + ", but was " + val2);
                } else {
                    problems.addAll(jsonObjsAreEqual((JSONObject) val1, (JSONObject) val2, label));
                }
            } else {
                if (!val1.equals(val2)) {
                    problems.add("Expected " + label + " to be " + val1 + ", but was " + val2);
                }
            }
        }
        return problems;
    }
}
