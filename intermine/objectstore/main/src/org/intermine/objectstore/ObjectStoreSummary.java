package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SubqueryExistsConstraint;
import org.intermine.util.StringUtil;

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.Logger;

/**
 * A summary of the data in an ObjectStore
 *
 * @author Kim Rutherford
 * @author Mark Woodbridge
 */
public class ObjectStoreSummary
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreSummary.class);
    
    private final Map classCountsMap = new HashMap();
    private final Map fieldValuesMap = new HashMap();
    protected final Map emptyFieldsMap = new HashMap();
    private final Map nonEmptyFieldsMap = new HashMap();

    static final String NULL_FIELDS_SUFFIX = ".nullFields";
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
        LOG.info("Collecting class counts...");
        for (Iterator i = os.getModel().getClassDescriptors().iterator(); i.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            nonEmptyFieldsMap.put(cld.getName(), new HashSet());
            Query q = new Query();
            QueryClass qc = new QueryClass(Class.forName(cld.getName()));
            q.addToSelect(new QueryField(qc, "id"));
            q.addFrom(qc);
            classCountsMap.put(cld.getName(), new Integer(os.count(q, ObjectStore.SEQUENCE_IGNORE)));
        }
        //fieldValues
        LOG.info("Summarising field values...");
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
            summariseField(cld, fieldNames, os, maxValues);
            for (Iterator j = os.getModel().getAllSubs(cld).iterator(); j.hasNext();) {
                summariseField((ClassDescriptor) j.next(), fieldNames, os, maxValues);
            }
        }
        // always empty references and collections
        LOG.info("Looking for empty collections and references...");
        Model model = os.getModel();
        for (Iterator iter = model.getClassDescriptors().iterator(); iter.hasNext();) {
            long startTime = System.currentTimeMillis();
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            lookForEmptyThings(cld, os);
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
            } else if (key.endsWith(NULL_FIELDS_SUFFIX)) {
                String className = key.substring(0, key.lastIndexOf("."));
                List fieldNames = Arrays.asList(StringUtil.split(value, FIELD_DELIM));
                emptyFieldsMap.put(className, new TreeSet(fieldNames));
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
     * Get a list of the reference and collection names that, for a given class, are always
     * null or empty.
     * @param className the class name to look up
     * @return Set of null reference and empty collection names
     */
    public Set getNullReferencesAndCollections(String className) {
        return (Set) emptyFieldsMap.get(className);
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
        for (Iterator i = emptyFieldsMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            Set value = (Set) entry.getValue();
            if (value.size() > 0) {
                String fields = StringUtil.join(value, FIELD_DELIM);
                properties.put(key + NULL_FIELDS_SUFFIX, fields);
            }
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
    protected void summariseField(ClassDescriptor cld, List fieldNames, ObjectStore os,
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
    
    /**
     * Look for empty fields and collections on all instanceof of a particular class. This method
     * will recurse into subclasses in order to improve performance.
     *
     * @param cld the class of objects to be examined
     * @param os the ObjectStore
     * @throws ObjectStoreException if an error occurs retrieving data
     * @throws ClassNotFoundException if the class cannot be found
     */
    protected void lookForEmptyThings(ClassDescriptor cld, ObjectStore os)
        throws ObjectStoreException, ClassNotFoundException {
        Set emptyFields = (Set) emptyFieldsMap.get(cld.getName());
        if (emptyFields == null) {
            Iterator subIter = cld.getSubDescriptors().iterator();
            while (subIter.hasNext()) {
                ClassDescriptor sub = (ClassDescriptor) subIter.next();
                lookForEmptyThings(sub, os);
            }
            emptyFields = new TreeSet();
            emptyFieldsMap.put(cld.getName(), emptyFields);
            lookForEmptyThings(cld, emptyFields, (Set) nonEmptyFieldsMap.get(cld.getName()), os);
        }
    }

    /**
     * Look for empty fields and collections on all instances of a particular class.
     * @param cld the class of objects to be examined
     * @param nullFieldNames output set of null/empty references/collections
     * @param nonNullFieldNames set of non-null/empty references/collections
     * @param os the objectstore
     * @throws ObjectStoreException if an error occurs retrieving data
     * @throws ClassNotFoundException if the class cannot be found
     */
    protected void lookForEmptyThings(ClassDescriptor cld, Set nullFieldNames,
            Set nonNullFieldNames, ObjectStore os) throws ObjectStoreException,
              ClassNotFoundException {
        long startTime = System.currentTimeMillis();
        int skipped = 0;
        Iterator iter = IteratorUtils.chainedIterator(cld.getAllCollectionDescriptors().iterator(),
                cld.getAllReferenceDescriptors().iterator());
        while (iter.hasNext()) {
            ReferenceDescriptor desc = (ReferenceDescriptor) iter.next();
            if (nonNullFieldNames.contains(desc.getName())) {
                skipped++;
            } else {
                Query q = new Query();
                q.setDistinct(false);
                
                QueryClass qc1 = new QueryClass(Class.forName(cld.getName()));
                QueryClass qc2 = new QueryClass(Class.forName(desc.getReferencedClassName()));
                
                q.addFrom(qc1);
                q.addFrom(qc2);
                
                q.addToSelect(qc2);
                
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                QueryReference qd;
                if (desc instanceof CollectionDescriptor) {
                    qd = new QueryCollectionReference(qc1, desc.getName());
                } else {
                    qd = new QueryObjectReference(qc1, desc.getName());
                }
                ContainsConstraint gdc = new ContainsConstraint(qd, ConstraintOp.CONTAINS, qc2);
                cs.addConstraint(gdc);
                
                q.setConstraint(cs);
                
                Query q2 = new Query();
                q2.setDistinct(false);
                q2.addToSelect(new QueryValue(new Integer(1)));
                
                ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
                cs2.addConstraint(new SubqueryExistsConstraint(ConstraintOp.EXISTS, q));
                q2.setConstraint(cs2);
                
                Results results = os.execute(q2);
                results.setBatchSize(1);
                results.setNoExplain();
                results.setNoOptimise();
                if (results.iterator().hasNext()) {
                    LOG.debug("\t\t" + cld.getName() + "." + desc.getName() + "");
                    Stack s = new Stack();
                    s.push(cld);
                    while (!s.empty()) {
                        ClassDescriptor c = (ClassDescriptor) s.pop();
                        Set nonNull = (Set) nonEmptyFieldsMap.get(c.getName());
                        nonNull.add(desc.getName());
                        Iterator superIter = c.getSuperDescriptors().iterator();
                        while (superIter.hasNext()) {
                            s.push(superIter.next());
                        }
                    }
                } else {
                    LOG.debug("\t\t" + cld.getName() + "." + desc.getName() + " - EMPTY");
                    nullFieldNames.add(desc.getName());
                }
            }
        }
        LOG.info(cld.getName() + " skipped " + skipped + " fields, took "
                + (System.currentTimeMillis() - startTime) + " ms");
    }

}
