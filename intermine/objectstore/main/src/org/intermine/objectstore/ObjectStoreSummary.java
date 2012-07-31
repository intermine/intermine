package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
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

/**
 * A summary of the data in an ObjectStore
 *
 * @author Richard Smith
 * @author Kim Rutherford
 * @author Mark Woodbridge
 */
public class ObjectStoreSummary
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreSummary.class);

    private final Map<String, Integer> classCountsMap = new HashMap<String, Integer>();
    private final Map<String, List<Object>> fieldValuesMap = new HashMap<String, List<Object>>();
    protected final Map<String, Set<String>> emptyFieldsMap = new HashMap<String, Set<String>>();
    protected final Map<String, Set<String>> emptyAttributesMap =
        new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> nonEmptyFieldsMap = new HashMap<String, Set<String>>();
    // This should be overwritten by MAX_FIELD_VALUES from properties
    protected int maxValues = DEFAULT_MAX_VALUES;

    static final String NULL_FIELDS_SUFFIX = ".nullFields";
    static final String CLASS_COUNTS_SUFFIX = ".classCount";
    static final String FIELDS_SUFFIX = ".fieldValues";
    static final String EMPTY_ATTRIBUTES_SUFFIX = ".emptyAttributes";
    static final String NULL_MARKER = "___NULL___";
    static final String FIELD_DELIM = "$_^";
    static final String MAX_FIELD_VALUES = "max.field.values";

    /**
     * The default number of values to make available for UI dropdowns - attributes with more values
     * will not become dropdowns.
     */
    public static final int DEFAULT_MAX_VALUES = 200;

    /**
     * Construct a summary from by running queries in the ObjectStore.
     *
     * @param os the objectstore
     * @param configuration the configuration for summarization
     * @throws ClassNotFoundException if a class cannot be found
     * @throws ObjectStoreException if an error occurs accessing the data
     */
    public ObjectStoreSummary(ObjectStore os, Properties configuration)
        throws ClassNotFoundException, ObjectStoreException {

        // 1. get counts of each class
        // 2. count unique values for each field of each class
        //    - avoid counting unique fields where class count is less than cutoff
        // 3. for fields with fewer unique values than cutoff, create dropdowns
        // 4. Always empty refs/cols per class
        // 5. Always empty attributes per class

        Model model = os.getModel();

        // classCounts - number of objects of each type in the database
        LOG.info("Collecting class counts...");
        for (ClassDescriptor cld : model.getTopDownLevelTraversal()) {
            nonEmptyFieldsMap.put(cld.getName(), new HashSet<String>());

            if (!classCountsMap.containsKey(cld.getName())) {
                int classCount = countClass(os, cld.getType());
                LOG.info("Adding class count: " + cld.getUnqualifiedName() + " = " + classCount);
                classCountsMap.put(cld.getName(), new Integer(classCount));

                // if this class is empty all subclasses MUST be empty as well
                if (classCount == 0) {
                    for (ClassDescriptor subCld : model.getAllSubs(cld)) {
                        if (!classCountsMap.containsKey(subCld.getName())) {
                            classCountsMap.put(subCld.getName(), new Integer(classCount));
                        }
                    }
                }
            }
        }

        // fieldValues - find all attributes with few unique values for populating dropdowns,
        // also look for any attributes that are empty.
        LOG.info("Summarising field values...");
        String maxValuesString = (String) configuration.get(MAX_FIELD_VALUES);
        maxValues =
            (maxValuesString == null ? DEFAULT_MAX_VALUES : Integer.parseInt(maxValuesString));

        // always empty references and collections
        LOG.info("Looking for empty collections and references...");
        Set<String> ignoreFields = getIgnoreFields((String) configuration.get("ignore.counts"));
        if (ignoreFields.size() > 0) {
            LOG.warn("Not counting ignored fields: " + ignoreFields);
        }

        Set<String> doneFields = new HashSet<String>();
        for (ClassDescriptor cld : model.getBottomUpLevelTraversal()) {

            int classCount = classCountsMap.get(cld.getName()).intValue();
            if (classCount == 0) {
                continue;
            }

            for (AttributeDescriptor att : cld.getAllAttributeDescriptors()) {
                String fieldName = att.getName();
                if ("id".equals(fieldName)) {
                    continue;
                }

                String clsFieldName = cld.getName() + "." + fieldName;
                if (doneFields.contains(clsFieldName) || ignoreFields.contains(clsFieldName)) {
                    continue;
                }

                Results results = getFieldSummary(cld, fieldName, os);
                if (results.size() <= maxValues) {
                    List<Object> fieldValues = new ArrayList<Object>();
                    for (Object resRow: results) {
                        Object fieldValue = ((ResultsRow<?>) resRow).get(0);
                        fieldValues.add(fieldValue == null ? null : fieldValue.toString());
                    }
                    if (fieldValues.size() == 1 && fieldValues.get(0) == null) {
                        Set<String> emptyAttributes = emptyAttributesMap.get(cld.getName());
                        if (emptyAttributes == null) {
                            emptyAttributes = new HashSet<String>();
                            emptyAttributesMap.put(cld.getName(), emptyAttributes);
                        }
                        emptyAttributes.add(fieldName);
                    }
                    Collections.sort(fieldValues, new Comparator<Object>() {
                        @Override
                        public int compare(Object arg0, Object arg1) {
                            if (arg0 == null) {
                                return arg1 == null ? 0 : 1;
                            }
                            if (arg1 == null) {
                                return arg0 == null ? 0 : -1;
                            }
                            return arg0.toString().compareTo(arg1.toString());
                        }
                    });
                    fieldValuesMap.put(clsFieldName, fieldValues);
                    LOG.info("Adding " + fieldValues.size() + " values for "
                            + cld.getUnqualifiedName() + "." + fieldName);

                } else {
                    LOG.info("Too many values for " + cld.getUnqualifiedName() + "." + fieldName);
                    // all superclasses must also have too many values for this field
                    for (ClassDescriptor superCld : cld.getAllSuperDescriptors()) {
                        if (cld.equals(superCld)
                                || superCld.getType().equals(InterMineObject.class)) {
                            continue;
                        }
                        String superClsField = superCld.getName() + "." + fieldName;
                        if (!doneFields.contains(superClsField)
                                && (superCld.getAttributeDescriptorByName(fieldName,
                                        true) != null)) {
                            LOG.info("Pushing too many values from " + cld.getUnqualifiedName()
                                    + "." + fieldName + " to " + superCld.getUnqualifiedName());
                            doneFields.add(superClsField);
                        }
                    }
                }
            }
        }



        // This is faster as a bottom up traversal, though this may save fewer queres the saved
        // queries would take longer. If a ref/col is not empty it must not be empty in all parents.
        Set<String> notEmptyFields = new HashSet<String>();
        for (ClassDescriptor cld: model.getBottomUpLevelTraversal()) {
            int classCount = classCountsMap.get(cld.getName()).intValue();
            if (classCount == 0) {
                continue;
            }

            Set<ReferenceDescriptor> refsAndCols = new HashSet<ReferenceDescriptor>();
            refsAndCols.addAll(cld.getAllReferenceDescriptors());
            refsAndCols.addAll(cld.getAllCollectionDescriptors());
            for (ReferenceDescriptor ref : refsAndCols) {
                String fieldName = ref.getName();
                String clsFieldName = cld.getName() + "." + fieldName;

                if (ignoreFields.contains(fieldName)) {
                    continue;
                }

                if (notEmptyFields.contains(clsFieldName)) {
                    LOG.info("Skipping " + clsFieldName + " - already know it's not empty");
                    continue;
                }

                boolean refIsEmpty = isReferenceEmpty(cld, ref, os);
                if (refIsEmpty) {
                    addToEmptyFields(cld.getName(), ref.getName());
                    LOG.info("Adding empty field " + cld.getUnqualifiedName() + "." + fieldName);
                } else {
                    // this isn't empty, so CAN'T be empty for any super classes
                    for (ClassDescriptor superCld : cld.getAllSuperDescriptors()) {
                        if (cld.equals(superCld)
                                || superCld.getType().equals(InterMineObject.class)) {
                            continue;
                        }
                        String superClsField = superCld.getName() + "." + fieldName;

                        if (!notEmptyFields.contains(superClsField)) {
                            if ((superCld.getReferenceDescriptorByName(fieldName, true) != null)
                                    || (superCld.getCollectionDescriptorByName(fieldName,
                                            true) != null)) {
                                LOG.info("Pushing not empty ref/col from "
                                        + cld.getUnqualifiedName() + "." + fieldName + " to "
                                        + superCld.getUnqualifiedName());
                                notEmptyFields.add(superClsField);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Construct a summary from a properties object.
     *
     * @param properties the properties
     */
    public ObjectStoreSummary(Properties properties) {
        for (Map.Entry<Object, Object> entry: properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();
            if (key.endsWith(CLASS_COUNTS_SUFFIX)) {
                String className = key.substring(0, key.lastIndexOf("."));
                classCountsMap.put(className, Integer.valueOf(value));
            } else if (key.endsWith(FIELDS_SUFFIX)) {
                String classAndFieldName = key.substring(0, key.lastIndexOf("."));
                List<Object> fieldValues =
                    new ArrayList<Object>(Arrays.asList(StringUtil.split(value, FIELD_DELIM)));
                for (int j = 0; j < fieldValues.size(); j++) {
                    if (fieldValues.get(j).equals(NULL_MARKER)) {
                        fieldValues.set(j, null);
                    }
                }
                fieldValuesMap.put(classAndFieldName, fieldValues);
            } else if (key.endsWith(NULL_FIELDS_SUFFIX)) {
                String className = key.substring(0, key.lastIndexOf("."));
                List<String> fieldNames = Arrays.asList(StringUtil.split(value, FIELD_DELIM));
                emptyFieldsMap.put(className, new TreeSet<String>(fieldNames));
            }  else if (key.endsWith(EMPTY_ATTRIBUTES_SUFFIX)) {
                String className = key.substring(0, key.lastIndexOf("."));
                List<String> attributeNames = Arrays.asList(StringUtil.split(value, FIELD_DELIM));
                emptyAttributesMap.put(className, new TreeSet<String>(attributeNames));
            } else if (key.equals(MAX_FIELD_VALUES)) {
                this.maxValues = Integer.parseInt(value);
            }
        }
    }

    /**
     * Return the configured maximum number of values to show in a dropdown.
     * @return the maximum number of values to show in a dropdown
     */
    public int getMaxValues() {
        return maxValues;
    }

    /**
     * Get the number of instances of a particular class in the ObjectStore.
     *
     * @param className the class name to look up
     * @return the count of the instances of the class
     */
    public int getClassCount(String className) {
        Integer countInteger = classCountsMap.get(className);
        if (countInteger == null) {
            throw new RuntimeException("cannot find class count for: " + className);
        }
        return countInteger.intValue();
    }

    /**
     * Get a list of the possible values (as Strings) for a given field in a given class.
     *
     * @param className the class to search for
     * @param fieldName the field name to search for
     * @return a list of the possible values for the class and field, or null if the summary isn't
     * available (because, for example, there are too many possible values)
     */
    public List<Object> getFieldValues(String className, String fieldName) {
        return fieldValuesMap.get(className + "." + fieldName);
    }

    /**
     * Get a list of the reference and collection names that, for a given class, are always
     * null or empty.
     *
     * @param className the class name to look up
     * @return Set of null reference and empty collection names
     */
    public Set<String> getNullReferencesAndCollections(String className) {
        if (emptyFieldsMap.containsKey(className)) {
            return Collections.unmodifiableSet(emptyFieldsMap.get(className));
        }
        return Collections.emptySet();
    }

    /**
     * Get a list of reference and collection names that are always null or empty.
     *
     * @return Set of null references and empty collection names mapped to class names
     */
    public Map<String, Set<String>> getAllNullReferencesAndCollections() {
        return Collections.unmodifiableMap(emptyFieldsMap);
    }

    /**
     * Get a list of the attributes that, for a given class, are always null or empty.
     *
     * @param className the class name to look up
     * @return Set of null attribute names
     */
    public Set<String> getNullAttributes(String className) {
        if (emptyAttributesMap.containsKey(className)) {
            return Collections.unmodifiableSet(emptyAttributesMap.get(className));
        }
        return Collections.emptySet();
    }

    /**
     * Get a list of the attributes that are always null or empty.
     *
     * @return Set of null attribute names mapped to class names
     */
    public Map<String, Set<String>> getAllNullAttributes() {
        return Collections.unmodifiableMap(emptyAttributesMap);
    }

    /**
     * Convert this summary to a properties object
     * @return the properties
     */
    public Properties toProperties() {
        Properties properties = new Properties();
        properties.put(MAX_FIELD_VALUES, "" + maxValues);
        for (Map.Entry<String, Integer> entry: classCountsMap.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            properties.put(key + CLASS_COUNTS_SUFFIX, value.toString());
        }
        for (Map.Entry<String, List<Object>> entry: fieldValuesMap.entrySet()) {
            String key = entry.getKey();
            List<Object> value = entry.getValue();
            StringBuffer sb = new StringBuffer();
            for (Iterator<Object> j = value.iterator(); j.hasNext();) {
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
        // emptyFieldsMap contains empty references and collections
        writeEmptyMapToProperties(properties, NULL_FIELDS_SUFFIX, emptyFieldsMap);

        // emptyAttributesMap contains empty attributes only
        writeEmptyMapToProperties(properties, EMPTY_ATTRIBUTES_SUFFIX, emptyAttributesMap);

        return properties;
    }

    private void writeEmptyMapToProperties(Properties properties, String keySuffix,
            Map<String, Set<String>> emptyMap) {
        for (Map.Entry<String, Set<String>> entry: emptyMap.entrySet()) {
            String key = entry.getKey();
            List<String> value = new  ArrayList<String>(entry.getValue());
            Collections.sort(value, new Comparator<Object>() {
                @Override
                public int compare(Object arg0, Object arg1) {
                    if (arg0 == null) {
                        return arg1 == null ? 0 : 1;
                    }
                    if (arg1 == null) {
                        return arg0 == null ? 0 : -1;
                    }
                    return arg0.toString().compareTo(arg1.toString());
                }
            });

            if (value.size() > 0) {
                String fields = StringUtil.join(value, FIELD_DELIM);
                properties.put(key + keySuffix, fields);
            }
        }
    }

    private Results getFieldSummary(ClassDescriptor cld, String fieldName, ObjectStore os) {
        Query q = new Query();
        q.setDistinct(true);
        QueryClass qc = new QueryClass(cld.getType());
        q.addToSelect(new QueryField(qc, fieldName));
        q.addFrom(qc);
        Results results = os.execute(q);
        return results;
    }

    /**
     * Look for empty fields and collections on all instances of a particular class.
     *
     * @param cld the class of objects to be examined
     * @param ref a reference or collection descriptor for the the class under cld
     * @param os the objectstore
     * @return true if the reference or collection is empty
     */
    private boolean isReferenceEmpty(ClassDescriptor cld, ReferenceDescriptor ref,
            ObjectStore os) {
        long startTime = System.currentTimeMillis();

        // This is much faster using a sub query and SubQueryExistsConstraint than just selecting
        // one row from the joined tables.  Probably because all queries have to be ordered for
        // batching to work.

        LOG.info("Querying for empty: " + cld.getUnqualifiedName() + "." + ref.getName());
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qc1 = new QueryClass(cld.getType());
        QueryClass qc2 = new QueryClass(ref.getReferencedClassDescriptor().getType());

        q.addFrom(qc1);
        q.addFrom(qc2);

        q.addToSelect(qc2);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryReference qd;
        if (ref instanceof CollectionDescriptor) {
            qd = new QueryCollectionReference(qc1, ref.getName());
        } else {
            qd = new QueryObjectReference(qc1, ref.getName());
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

        Results results = os.execute(q2, 1, false, false, false);
        boolean empty = !results.iterator().hasNext();

        LOG.info("Query for empty " + cld.getUnqualifiedName() + "." + ref.getName() + " took "
                + (System.currentTimeMillis() - startTime) + "ms.");
        return empty;
    }
    private void addToEmptyFields(String clsName, String fieldName) {
        Set<String> emptyFields = emptyFieldsMap.get(clsName);
        if (emptyFields == null) {
            emptyFields = new HashSet<String>();
            emptyFieldsMap.put(clsName, emptyFields);
        }
        emptyFields.add(fieldName);
    }

    private int countClass(ObjectStore os, Class<? extends FastPathObject> cls)
        throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.addToSelect(qc);
        q.addFrom(qc);
        return os.count(q, ObjectStore.SEQUENCE_IGNORE);
    }

    private static Set<String> getIgnoreFields(String config) {
        Set<String> retval = new HashSet<String>();
        if (config != null) {
            config = config.trim();
            for (String field :config.split(" ")) {
                retval.add(field);
            }
        }
        return retval;
    }

}
