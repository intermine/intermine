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

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Generate a Properties object that can be passed to the ObjectStoreSummary constructor.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreSummaryGenerator
{
    static final String FIELDS_SUFFIX = ".fields";

    /**
     * Get the number of instances of a particular class in the ObjectStore.
     * @param os the ObjectStore to query
     * @param className the class name to look up
     * @throws ObjectStoreException if an error occurs during the database query
     * @throws ClassNotFoundException if the className doesn't refer to a known class
     * @return the count of the instances of the class
     */
    private static int getClassCount(ObjectStore os, String className)
        throws ObjectStoreException, ClassNotFoundException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Class.forName(className));
        q.addToSelect(new QueryField(qc, "id"));
        q.addFrom(qc);

        int count = os.count(q, os.getSequence());
        return count;
    }

    /**
     * Count all the objects of each class and put the results in the given Properties objects.
     * The properties will look like: org.flymine.model.Gene.count = 12345
     */
    private static void getAllClassCounts(ObjectStore os, Properties properties)
        throws ObjectStoreException {
        Model model = os.getModel();
        Set classDescriptors = model.getClassDescriptors();
        Iterator classDescriptorIterator = classDescriptors.iterator();

        while (classDescriptorIterator.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) classDescriptorIterator.next();
            String className = cd.getName();
            try {
                int count = getClassCount(os, className);
                properties.put(className + ObjectStoreSummary.CLASS_COUNTS_SUFFIX , count + "");
            } catch (ClassNotFoundException e) {
                throw new Error("internal error: " + e);
            }
        }
    }

    static final String FIELD_DELIM = "$_^";

    /**
     * Returns null if there are more than maxValues filed values.
     */
    private static String getFieldSummary(ObjectStore os, String className, String fieldName,
                                          int maxValues)
        throws ObjectStoreException, ClassNotFoundException { 
        Query q = new Query();
        q.setDistinct(true);
        QueryClass qc = new QueryClass(Class.forName(className));
        q.addToSelect(new QueryField(qc, fieldName));
        q.addFrom(qc);
        Results results = os.execute(q);
        if (results.size () > maxValues) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < results.size(); i++) {
            if (i != 0) {
                sb.append(FIELD_DELIM);
            }

            sb.append(((ResultsRow) results.get(i)).get(0));
        }

        return sb.toString();
    }

    private static void getFieldSummaries(ObjectStore os, Properties configurationProperties,
                                          Properties outputProperties)
        throws ObjectStoreException, ClassNotFoundException {
        Iterator configurationIterator = configurationProperties.keySet().iterator();

        Integer maxValuesInteger =
            Integer.valueOf((String) configurationProperties.get("max.field.values"));
            
        int maxValues;
            
        if (maxValuesInteger == null) {
            maxValues = Integer.MAX_VALUE;
        } else {
            maxValues = maxValuesInteger.intValue();
        }

        while (configurationIterator.hasNext()) {
            String key = (String) configurationIterator.next();
            if (!key.endsWith(FIELDS_SUFFIX)) {
                continue;
            }
            
            String className = key.substring(0, key.length() - FIELDS_SUFFIX.length());
            String fields = (String) configurationProperties.get(key);
            String[] parts = fields.split("[\t ]");

            ClassDescriptor cld = os.getModel().getClassDescriptorByName(className);

            Set allClds = new HashSet();
            allClds.add(cld);
            allClds.addAll(os.getModel().getAllSubs(cld));

            for (int partIndex = 0; partIndex < parts.length; partIndex++) {
                String fieldName = parts[partIndex];

                Iterator allCldsIterator = allClds.iterator();

                while (allCldsIterator.hasNext()) {
                    String thisCldClassName =
                        ((ClassDescriptor) allCldsIterator.next()).getName();

                    processFields(os, thisCldClassName, fieldName, maxValues, outputProperties);
                }
            }
        }
    }

    private static void processFields(ObjectStore os, String className, String fieldName,
                                      int maxValues, Properties outputProperties)
        throws ObjectStoreException, ClassNotFoundException {

        String fieldSummary = getFieldSummary(os, className, fieldName, maxValues);
        if (fieldSummary != null) {
            outputProperties.put(className + "." + fieldName
                                 + ObjectStoreSummary.FIELDS_SUFFIX,
                                 fieldSummary);
        }
    }

    /**
     * For the given configuration Properties object return Properties object that summarises the
     * given ObjectStore.  The configurationProperties have this form:
     * org.intermine.model.SomeObject.fields = someField someOtherField
     * @param os the ObjectStore to summarise.
     * @param configurationProperties A Properties object describing the objects and fields to
     * summarise
     * @throws ObjectStoreException if there is a problem with the ObjectStore
     * @throws ClassNotFoundException if the class name in the configuration file doesn't refer to a
     * known class
     * @return a Properties object summarising the given ObjectStore
     */
    public static Properties getAsProperties(ObjectStore os, Properties configurationProperties)
        throws ObjectStoreException, ClassNotFoundException {
        Properties outputProperties = new Properties();

        getAllClassCounts(os, outputProperties);
        getFieldSummaries(os, configurationProperties, outputProperties);

        return outputProperties;
    }
}
