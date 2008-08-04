package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * GridDataSet contains Data for gridWidget
 * @author Dominik Grimm
 */
public class GridDataSet 
{
    private List<String> sampleName = new Vector<String>();
    private List<String> valueList = new Vector<String>();
    private Map<String, List<String>> valueMap = new HashMap<String, List<String>>();
    private Map<String, Map<String, List<String>>> gridMap =
        new HashMap<String, Map<String, List<String>>>();
    
    /**
     * addValue
     * @param columnName = columnName
     * @param value = value for columnName
     */
    public void addValue(String columnName, String value) {
        if (!sampleName.contains(columnName)) {
            sampleName.add(columnName); 
            valueList = new Vector<String>();
            valueList.add(value);
            valueMap = new HashMap<String, List<String>>();
            valueMap.put("UP", valueList);
            gridMap.put(columnName, valueMap);
         } else {
             Map<String, List<String>> values = gridMap.get(columnName);
             List<String> tmpV = values.get("UP");
             tmpV.add(value);
             values.put("UP", tmpV);
             gridMap.put(columnName, values);
         }
    }
    
    /**
     * addValue
     * @param columnName = columnName
     * @param value = value for columnName
     * @param direction = Flag for up and down (true == up, false == down)
     */
    public void addValue(String columnName, String value, boolean direction) {
        if (!sampleName.contains(columnName)) {
            sampleName.add(columnName); 
            valueList = new Vector<String>();
            valueList.add(value);
            valueMap = new HashMap<String, List<String>>();
            if (direction) {
                valueMap.put("UP", valueList);
            } else {
                valueMap.put("DOWN", valueList);
            }
            gridMap.put(columnName, valueMap);
         } else {
             Map<String, List<String>> values = gridMap.get(columnName);
             if (direction) {
                 if (values.get("UP") == null) {
                     List<String> tmpV = new Vector<String>();
                     tmpV.add(value);
                     values.put("UP", tmpV);
                 } else {
                     List<String> tmpV = values.get("UP");
                     tmpV.add(value);
                     values.put("UP", tmpV);
                 }
             } else {
                 if (values.get("DOWN") == null) {
                     List<String> tmpV = new Vector<String>();
                     tmpV.add(value);
                     values.put("DOWN", tmpV);
                 } else {
                     List<String> tmpV = values.get("DOWN");
                     tmpV.add(value);
                     values.put("DOWN", tmpV);
                 }
             }
             gridMap.put(columnName, values);
         }
    }
    
    /**
     * getSampleNames
     * @return List with the columnNames
     */
    public List<String> getSampleNames() {
        return sampleName;
    }
    
    /**
     * getResults
     * @return map with columnNames and the values for each column
     */
    public Map<String, Map<String, List<String>>> getResults() {
        return gridMap;
    }
}
