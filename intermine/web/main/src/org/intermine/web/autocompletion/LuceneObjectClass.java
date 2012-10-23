package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2012 FlyMine
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

/**
 * LuceneObjectClass contains data for the autocompletion
 * @author Dominik Grimm
 */
public class LuceneObjectClass
{

    private String className = null;
    private List<String> fieldNames = null;
    private HashMap<String, List<String>> values = null;

    /**
     * Constructor
     * @param className name of the class
     */
    public LuceneObjectClass(String className) {
        this.className = className;
        fieldNames = new Vector<String>();
        values = new HashMap<String, List<String>>();
    }

    /**
     * addField added field to the class
     * @param fieldName name of the field
     * @return true if the field already not exists
     */
    public boolean addField(String fieldName) {
        if (!fieldNames.contains(fieldName)) {
            fieldNames.add(fieldName);
            return true;
        }
        return false;
    }

    /**
     * addValueToField added a new value to one existing field
     * @param fieldName name of the field
     * @param value the value
     * @return true if the adding step was successful
     */
    public boolean addValueToField(String fieldName, String value) {
        if (fieldNames.contains(fieldName) && values.get(fieldName) == null) {
            List<String> vec = new Vector<String>();
            vec.add(value);
            values.put(fieldName, vec);
            return true;
        }
        else if (fieldNames.contains(fieldName) && values.get(fieldName) != null
                && !values.get(fieldName).contains(value)) {
            List<String> vec = values.get(fieldName);
            vec.add(value);
            values.put(fieldName, vec);
            return true;
        }
        return false;
    }

    /**
     * getFieldNames
     * @return returns a list of all fieldNames
     */
    public List<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * return the fieldName at position index
     * @param index position of the field
     * @return fieldName at position index
     */
    public String getFieldName(int index) {
        return fieldNames.get(index);
    }

    /**
     * getClassName
     * @return className name of the class
     */
    public String getClassName() {
        return className;
    }

    /**
     * getSizeFields
     * @return size number of fields in the class
     */
    public int getSizeFields() {
        return fieldNames.size();
    }

    /**
     * getSizeValuesForField
     * @param fieldName name of the field
     * @return size number of values in one specific field
     */
    public int getSizeValuesForField(String fieldName) {
        return values.get(fieldName).size();
    }

    /**
     * getValuesForField
     * @param fieldName name of the field
     * @return list of the values in field fieldName
     */
    public List<String> getValuesForField(String fieldName) {
        return values.get(fieldName);
    }

    /**
     * getValuesForField
     * @param fieldName name of the field
     * @param index of the value
     * @return value at index index in field fieldName
     */
    public String getValuesForField(String fieldName, int index) {
        return values.get(fieldName).get(index);
    }

    /**
     * getSizeValues
     * @return maximal number of values
     */
    public int getSizeValues() {
        int size = 0;
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            if (values.containsKey(fieldName)
                            && size < values.get(fieldName).size()) {
                size = values.get(fieldName).size();
            }
        }
        return size;
    }
}
