package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;
import java.util.Arrays;

import org.intermine.util.StringUtil;

/**
 * A summary of an ObjectStore.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreSummary
{
    private final Map classCountsMap = new HashMap();
    private final Map fieldValuesMap = new HashMap();

    static final String CLASS_COUNTS_SUFFIX = ".classCount";
    static final String FIELDS_SUFFIX = ".fieldValues";

    /**
     * Create a new ObjectStoreSummary object.
     * @param properties the Properties object to retrieve the summary from
     */
    public ObjectStoreSummary(Properties properties) {
        Iterator keyIterator = properties.keySet().iterator();
        while (keyIterator.hasNext()) {
            String key = (String) keyIterator.next();

            if (key.endsWith(CLASS_COUNTS_SUFFIX)) {
                String className = key.substring(0, key.length() - CLASS_COUNTS_SUFFIX.length());
                Integer count = Integer.valueOf((String) properties.get(key));
                classCountsMap.put(className, count);
            } else if (key.endsWith(FIELDS_SUFFIX)) {
                String classAndFieldName = key.substring(0, key.length() - FIELDS_SUFFIX.length());
                String className =
                    classAndFieldName.substring(0, classAndFieldName.lastIndexOf("."));
                String fieldName =
                    classAndFieldName.substring(classAndFieldName.lastIndexOf(".") + 1);
                String fieldValuesString = (String) properties.get(key);
                String[] fieldValues =
                    StringUtil.split(fieldValuesString, ObjectStoreSummaryGenerator.FIELD_DELIM);

                List fieldValuesList = new ArrayList();

                for (int i = 0; i < fieldValues.length; i++) {
                    if (fieldValues[i].equals(ObjectStoreSummaryGenerator.NULL_MARKER)) {
                        fieldValuesList.add(null);                        
                    } else {
                        fieldValuesList.add(fieldValues[i]);
                    }
                }
                
                fieldValuesMap.put(getFieldValuesKey(className, fieldName), fieldValuesList);
            }
        }
    }

    private String getFieldValuesKey(String className, String fieldName) {
        return className + "." + fieldName;
    }
    
    /**
     * Get the number of instances of a particular class in the ObjectStore.
     * @param className the class name to look up
     * @return the count of the instances of the class
     */
    public int getClassCount(String className) {
        return ((Integer) classCountsMap.get(className)).intValue();
    }

    /**
     * Get a list of the possible values (as Strings) for a given field in a given class.
     * @param className the class to search for
     * @param fieldName the field name to search for
     * @return a list of the possible values for the class and field, or null if the summary isn't
     * available (because, for example, there are too many possible values)
     */
    public List getFieldValues(String className, String fieldName) {
        return (List) fieldValuesMap.get(getFieldValuesKey(className, fieldName));
    }
}
