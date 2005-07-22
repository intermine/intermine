package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.StringUtil;

/**
 * A summary of the data in an ObjectStore
 *
 * @author Kim Rutherford
 * @author Mark Woodbridge
 */
public class ObjectStoreSummary
{
    private final Map classCountsMap = new HashMap();
    private final Map fieldValuesMap = new HashMap();

    static final String CLASS_COUNTS_SUFFIX = ".classCount";
    static final String FIELDS_SUFFIX = ".fieldValues";
    static final String NULL_MARKER = "___NULL___";
    static final String FIELD_DELIM = "$_^";

    /**
     * Construct a summary from an objectstore
     * @param os the objectstore
     * @param configuration the configuration for summarization
     * @throws ClassNotFoundException if a class cannot be found
     * @throws ObjectStoreException if an error occurs accessing the data
     */
    public ObjectStoreSummary(ObjectStore os, Properties configuration)
        throws ClassNotFoundException, ObjectStoreException {
        //classCounts
        for (Iterator i = os.getModel().getClassDescriptors().iterator(); i.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            Query q = new Query();
            QueryClass qc = new QueryClass(Class.forName(cld.getName()));
            q.addToSelect(new QueryField(qc, "id"));
            q.addFrom(qc);
            classCountsMap.put(cld.getName(), new Integer(os.count(q, os.getSequence())));
        }
        //fieldValues
        String maxValuesString = (String) configuration.get("max.field.values");
        int maxValues =
            (maxValuesString == null ? Integer.MAX_VALUE : Integer.parseInt(maxValuesString));
        for (Iterator i = configuration.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.endsWith(".fields")) {
                continue;
            }
            String className = key.substring(0, key.lastIndexOf("."));
            ClassDescriptor cld = os.getModel().getClassDescriptorByName(className);
            if (cld == null) {
                throw new RuntimeException("a class mentioned in ObjectStore summary properties "
                                           + "file (" + className + ") is not in the model");
            }
            List fieldNames = Arrays.asList(value.split(" "));
            processFields(cld, fieldNames, os, maxValues);
            for (Iterator j = os.getModel().getAllSubs(cld).iterator(); j.hasNext();) {
                processFields((ClassDescriptor) j.next(), fieldNames, os, maxValues);
            }
        }
    }

    /**
     * Construct a summary from a properties object
     * @param properties the properties
     */
    public ObjectStoreSummary(Properties properties) {
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.endsWith(CLASS_COUNTS_SUFFIX)) {
                String className = key.substring(0, key.lastIndexOf("."));
                classCountsMap.put(className, Integer.valueOf(value));
            } else if (key.endsWith(FIELDS_SUFFIX)) {
                String classAndFieldName = key.substring(0, key.lastIndexOf("."));
                List fieldValues = Arrays.asList(StringUtil.split(value, FIELD_DELIM));
                for (int j = 0; j < fieldValues.size(); j++) {
                    if (fieldValues.get(j).equals(NULL_MARKER)) {
                        fieldValues.set(j, null);
                    }
                }
                fieldValuesMap.put(classAndFieldName, fieldValues);
            }
        }
    }

    /**
     * Get the number of instances of a particular class in the ObjectStore.
     * @param className the class name to look up
     * @return the count of the instances of the class
     */
    public int getClassCount(String className) {
        Integer countInteger = (Integer) classCountsMap.get(className);
        if (countInteger == null) {
            throw new RuntimeException("cannot find class count for: " + className);
        } else {
            return countInteger.intValue();
        }
    }

    /**
     * Get a list of the possible values (as Strings) for a given field in a given class.
     * @param className the class to search for
     * @param fieldName the field name to search for
     * @return a list of the possible values for the class and field, or null if the summary isn't
     * available (because, for example, there are too many possible values)
     */
    public List getFieldValues(String className, String fieldName) {
        return (List) fieldValuesMap.get(className + "." + fieldName);
    }

    /**
     * Convert this summary to a properties object
     * @return the properties
     */
    public Properties toProperties() {
        Properties properties = new Properties();
        for (Iterator i = classCountsMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            Integer value = (Integer) entry.getValue();
            properties.put(key + CLASS_COUNTS_SUFFIX, value.toString());
        }
        for (Iterator i = fieldValuesMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            List value = (List) entry.getValue();
            StringBuffer sb = new StringBuffer();
            for (Iterator j = value.iterator(); j.hasNext();) {
                String s = (String) j.next();
                if (s == null) {
                    sb.append(NULL_MARKER);
                } else {
                    sb.append(s);
                }
                if (j.hasNext()) {
                    sb.append(FIELD_DELIM);
                }
            }
            properties.put(key + FIELDS_SUFFIX, sb.toString());
        }
        return properties;
    }

    /**
     * Get the possible field values all instances of a specified class
     * @param cld the class descriptor for the class
     * @param fieldNames the fields to consider
     * @param os the object store to retrieve data from
     * @param maxValues only store information for fields with fewer than maxValues values
     * @throws ClassNotFoundException if the class cannot be found
     * @throws ObjectStoreException if an error occurs retrieving data
     */
    protected void processFields(ClassDescriptor cld, List fieldNames, ObjectStore os,
                                 int maxValues)
        throws ClassNotFoundException, ObjectStoreException {
        for (Iterator i = fieldNames.iterator(); i.hasNext();) {
            String fieldName = (String) i.next();
            Query q = new Query();
            q.setDistinct(true);
            QueryClass qc = new QueryClass(Class.forName(cld.getName()));
            q.addToSelect(new QueryField(qc, fieldName));
            q.addFrom(qc);
            Results results = os.execute(q);
            if (results.size() > maxValues) {
                continue;
            }
            List fieldValues = new ArrayList();
            for (Iterator j = results.iterator(); j.hasNext();) {
                Object fieldValue = ((ResultsRow) j.next()).get(0);
                fieldValues.add(fieldValue == null ? null : fieldValue.toString());
            }
            fieldValuesMap.put(cld.getName() + "." + fieldName, fieldValues);
        }
    }
}
