package org.intermine.web.autocompletion;

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
import java.util.Vector;

public class LuceneObjectClass {

    private String className = null;
    private List<String> fieldNames = null;
    private HashMap<String, List<String>> values = null;
    
    public LuceneObjectClass(String className) {
        this.className = className;
        fieldNames = new Vector<String>();
        values = new HashMap<String, List<String>>();
    }
    
    public boolean addField(String fieldName) {
        if (!fieldNames.contains(fieldName)) {
            fieldNames.add(fieldName);
            return true;
        }
        return false;
    }
    
    public boolean addValueToField(String fieldName, String value) {
        if (fieldNames.contains(fieldName) && values.get(fieldName) == null) {
            List<String> vec = new Vector<String>();
            vec.add(value);
            values.put(fieldName, vec);
            return true;
        }
        else if (fieldNames.contains(fieldName) && values.get(fieldName) != null
                && values.get(fieldName).contains(value)  == false) {
            List<String> vec = values.get(fieldName);
            vec.add(value);
            values.put(fieldName, vec);
            return true;
        }
        return false;
    }
    
    public List<String> getFieldNames() {
        return fieldNames;
    }
    
    public String getFieldName(int index) {
        return fieldNames.get(index);
    }
    
    public String getClassName() {
        return className;
    }
    
    public int getSizeFields() {
        return fieldNames.size();
    }
    
    public int getSizeValuesForField(String fieldName) {
        return values.get(fieldName).size();
    }
    
    public List<String> getValuesForField(String fieldName) {
        return values.get(fieldName);
    }
    
    public String getValuesForField(String fieldName, int index) {
        return values.get(fieldName).get(index);
    }
    
    public int getSizeValues() {
        int size = 0;
        for (int i = 0; i < fieldNames.size(); i++) {
           if (size < values.get(fieldNames.get(i)).size()) {
              size = values.get(fieldNames.get(i)).size();
           }
        }
        return size;
    }
}
