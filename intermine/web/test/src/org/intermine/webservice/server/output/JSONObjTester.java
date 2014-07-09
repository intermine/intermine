/**
 * 
 */
package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Alexis Kalderimis
 *
 */
public class JSONObjTester {

    /**
     * Compare two JSONObjects for equality
     * @param left The reference JSONObject (this is referred to as "expected" in any messages)
     * @param right The candidate object to check.
     * @return a String with messages explaining the problems if there are any, otherwise null (equal)
     * @throws JSONException
     */
    public static String getProblemsComparing(JSONObject left, Object right) throws JSONException {
        List<String> problems = new ArrayList<String>();
        
        if (left == null ) {
            if (right != null) {
                return "Expected null, but got " + right;
            } else {
                return null;
            }
        }
        
        if (! (right instanceof JSONObject)) {
            if (right == null) {
                return "Didn't expect null, but got null";
            } else {
                return "Expected a JSONObject, is got a " + right.getClass();
            }
        }
        JSONObject rjo = (JSONObject) right;
        Set<String> leftNames = new HashSet<String>(Arrays.asList(JSONObject.getNames(left)));
        Set<String> rightNames = new HashSet<String>(Arrays.asList(JSONObject.getNames(rjo)));
        if (! leftNames.equals(rightNames)) {
            problems.add("Expected the keys " + 
                    leftNames + ", but got these: " + rightNames);
        }
        
        for (String name : leftNames) {
            Object leftValue = left.get(name);
            String problem = null;
            try {
                if (leftValue == null) {
                    if ( rjo.get(name) != null ) {
                        problem = "Expected null, but got " + rjo.get(name);
                    }
                } else if (leftValue instanceof JSONObject) {
                    problem = getProblemsComparing((JSONObject) leftValue, rjo.get(name));
                } else if (leftValue instanceof JSONArray) {
                    problem = getProblemsComparing((JSONArray) leftValue, rjo.get(name));
                } else {
                    if (! leftValue.toString().equals(rjo.get(name).toString())) {
                        problem = "Expected " + leftValue + " but got " + rjo.get(name); 
                    }
                }
            } catch (Throwable e) {
                problem = e.toString();
            }
            if (problem != null) {
                problems.add("Problem with " + name + ": " + problem);
            }
        }
        
        if (problems.isEmpty()) {
            return null;
        } 
        return problems.toString();
    }
    
    /**
     * Compare two JSONArrays for equality
     * @param left The reference array (referred to as "expected" in any messages)
     * @param right The candidate object to check.
     * @return a String with messages explaining the problems if there are any, otherwise null (equal)
     * @throws JSONException
     */
    public static String getProblemsComparing(JSONArray left, Object right) throws JSONException {
        List<String> problems = new ArrayList<String>();
        
        if (left == null ) {
            if (right != null) {
                return "Expected null, but got " + right;
            } else {
                return null;
            }
        }
        
        if (! (right instanceof JSONArray)) {
            return "Expected a JSONArray, but got a " + right.getClass();
        }
        
        JSONArray rja = (JSONArray) right;
        if (left.length() != rja.length()) {
            problems.add("Expected the size of this array to be " + left.length() + 
                    " but got " + rja.length());
        }
        for (int index = 0; index < left.length(); index++) {
            Object leftMember = left.get(index);
            String problem = null;
            try {
                if (leftMember== null) {
                    if ( rja.get(index) != null ) {
                        problem = "Expected null, but got " + rja.get(index);
                    }
                } else if (leftMember instanceof JSONObject) {
                    problem = getProblemsComparing((JSONObject) leftMember, rja.get(index));
                } else if (leftMember instanceof JSONArray) {
                    problem = getProblemsComparing((JSONArray) leftMember, rja.get(index));
                } else {
                    if (! leftMember.toString().equals(rja.get(index).toString())) {
                        problem = "Expected " + leftMember + 
                            " but got " + rja.get(index);
                    }
                }
            } catch (Throwable e) {
                problem = e.toString();
            }
            if (problem != null) {
                problems.add("Problem with index " + index + ": " + problem);
            }
        }
        
        if (problems.isEmpty()) {
            return null;
        } 
        return problems.toString();
        
    }

    
}
