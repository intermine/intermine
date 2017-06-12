package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathException;
import org.intermine.util.ObjectPipe;

/**
 * thread to fetch all intermineobjects (with exceptions) from database, create
 * a lucene document for them, add references (if applicable) and put the final
 * document in the indexing queue
 * @author nils
 */
public class InterMineObjectFetcher extends Thread
{
    private static final Logger LOG = Logger.getLogger(InterMineObjectFetcher.class);

    final ObjectStore os;
    final Map<String, List<FieldDescriptor>> classKeys;
    final ObjectPipe<Document> indexingQueue;
    final Set<Class<? extends InterMineObject>> ignoredClasses;
    final Map<Class<? extends InterMineObject>, Set<String>> ignoredFields;
    final Map<Class<? extends InterMineObject>, String[]> specialReferences;
    final Map<ClassDescriptor, Float> classBoost;
    final Vector<KeywordSearchFacetData> facets;

    final Map<Integer, Document> documents = new HashMap<Integer, Document>();
    final Set<String> fieldNames = new HashSet<String>();
    private Set<String> normFields = new HashSet<String>();
    final Map<Class<?>, Vector<ClassAttributes>> decomposedClassesCache =
            new HashMap<Class<?>, Vector<ClassAttributes>>();
    private Map<String, String> attributePrefixes = null;

    Field idField = null;
    Field categoryField = null;

    private volatile Exception error;

    /**
     * initialize the documentfetcher thread
     * @param os
     *            intermine objectstore
     * @param classKeys
     *            classKeys from InterMineAPI, map of classname to all key field
     *            descriptors
     * @param indexingQueue
     *            queue shared with indexer
     * @param ignoredClasses
     *            classes that should not be indexed (as specified in config +
     *            subclasses)
     * @param ignoredFields fields to ignore
     * @param specialReferences
     *            map of classname to references to index in additional to
     *            normal attributes
     * @param classBoost
     *            apply per-class doc boost as specified here (all other classes
     *            get 1.0)
     * @param facets
     *            fields used for faceting - will be indexed untokenized in
     *            addition to the normal indexing
     * @param attributePrefixes prefixes to be ignored
     */
    public InterMineObjectFetcher(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys,
            ObjectPipe<Document> indexingQueue,
            Set<Class<? extends InterMineObject>> ignoredClasses,
            Map<Class<? extends InterMineObject>, Set<String>> ignoredFields,
            Map<Class<? extends InterMineObject>, String[]> specialReferences,
            Map<ClassDescriptor, Float> classBoost, Vector<KeywordSearchFacetData> facets,
            Map<String, String> attributePrefixes) {
        super();

        this.os = os;
        this.classKeys = classKeys;
        this.indexingQueue = indexingQueue;
        this.ignoredClasses = ignoredClasses;
        this.ignoredFields = ignoredFields;
        this.specialReferences = specialReferences;
        this.classBoost = classBoost;
        this.facets = facets;
        this.attributePrefixes = attributePrefixes;
    }

    /**
     * get list of fields contained in the fetched documents
     * @return fields
     */
    public Set<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * fetch objects from database, create documents and add them to the queue
     */
    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            long time = System.currentTimeMillis();
            long objectParseTime = 0;
            LOG.info("Fetching all InterMineObjects...");

            HashSet<Class<? extends InterMineObject>> seenClasses =
                    new HashSet<Class<? extends InterMineObject>>();
            HashMap<String, InterMineResultsContainer> referenceResults =
                    new HashMap<String, InterMineResultsContainer>();

            try {
                //query all objects except the ones we are ignoring
                Query q = new Query();
                QueryClass qc = new QueryClass(InterMineObject.class);
                q.addFrom(qc);
                q.addToSelect(qc);

                QueryField qf = new QueryField(qc, "class");
                q.setConstraint(new BagConstraint(qf, ConstraintOp.NOT_IN, ignoredClasses));

                LOG.info("QUERY: " + q.toString());

                Results results = os.execute(q, 1000, true, false, true);

                @SuppressWarnings("rawtypes")
                ListIterator<ResultsRow<InterMineObject>> it = (ListIterator) results
                    .listIterator();
                int i = iterateOverObjects(time, objectParseTime, seenClasses,
                        referenceResults, results, it);
                StringBuilder doneMessage = new StringBuilder();
                for (String fieldName : fieldNames) {
                    if (doneMessage.length() > 0) {
                        doneMessage.append(", ");
                    }
                    doneMessage.append(fieldName);
                    if (normFields.contains(fieldName)) {
                        doneMessage.append(" NO_NORMS");
                    }
                }
                LOG.info("COMPLETED index with " + i + " records.  Fields: " + doneMessage);
            } finally {
                for (InterMineResultsContainer resultsContainer : referenceResults.values()) {
                    ((ObjectStoreInterMineImpl) os).releaseGoFaster(resultsContainer.getResults()
                            .getQuery());
                }
            }
        } catch (Exception e) {
            LOG.warn("Error occurred during processing", e);
            setException(e);
        }

        //notify main thread that we're done
        indexingQueue.finish();
    }

    private void setException(Exception e) {
        this.error = e;
    }

    /**
     * Get the error that occurred during processing, if any.
     * @return The error.
     */
    public Exception getException() {
        return error;
    }

    private Document handleObject(
            InterMineObject object,
            HashSet<Class<? extends InterMineObject>> seenClasses,
            HashMap<String, InterMineResultsContainer> referenceResults)
        throws PathException, ObjectStoreException, IllegalAccessException {
        long objectParseStart = System.currentTimeMillis();
        long objectParseTime = 0L;
        Set<Class<?>> objectClasses = Util.decomposeClass(object.getClass());
        Class<?> objectTopClass = objectClasses.iterator().next();
        ClassDescriptor classDescriptor =
                os.getModel().getClassDescriptorByName(objectTopClass.getName());

        // create base doc for object
        Document doc = createDocument(object, classDescriptor);
        HashSet<String> references = new HashSet<String>();
        HashMap<String, KeywordSearchFacetData> referenceFacetFields =
                new HashMap<String, KeywordSearchFacetData>();

        // find all references associated with this object or
        // its superclasses
        for (Entry<Class<? extends InterMineObject>, String[]> specialClass
                : specialReferences.entrySet()) {
            for (Class<?> objectClass : objectClasses) {
                if (specialClass.getKey().isAssignableFrom(objectClass)) {
                    for (String reference : specialClass.getValue()) {
                        String fullReference =
                                classDescriptor.getUnqualifiedName() + "."
                                        + reference;
                        references.add(fullReference);

                        //check if this reference returns a field we are
                        //faceting by. if so, add it to referenceFacetFields
                        for (KeywordSearchFacetData facet : facets) {
                            for (String field : facet.getFields()) {
                                if (field.startsWith(reference + ".")
                                        && !field.substring(reference.length() + 1)
                                                .contains(".")) {
                                    referenceFacetFields.put(fullReference, facet);
                                }
                            }
                        }
                    }
                }
            }
        }

        // if we have not seen an object of this class before, query references
        if (!seenClasses.contains(object.getClass())) {
            LOG.info("Getting references for new class: " + object.getClass());

            // query all references that we need
            for (String reference : references) {
                // LOG.info("Querying reference " + reference);

                Query queryReference = getPathQuery(reference);

                // do not count this towards objectParseTime
                objectParseTime += (System.currentTimeMillis() - objectParseStart);

                Results resultsc = os.execute(queryReference, 1000, true, false,
                        true);
                ((ObjectStoreInterMineImpl) os).goFaster(queryReference);
                referenceResults.put(reference, new InterMineResultsContainer(
                        resultsc));
                LOG.info("Querying reference " + reference + " done -- "
                        + resultsc.size() + " results");

                // start counting objectParseTime again
                objectParseStart = System.currentTimeMillis();
            }

            seenClasses.add(object.getClass());
        }

        addReferences(object, references, referenceResults, referenceFacetFields, doc);

        objectParseTime += (System.currentTimeMillis() - objectParseTime);
        return doc;
    }

    /**
     * Add object references to search document.
     */
    private void addReferences(
            InterMineObject object,
            HashSet<String> references,
            HashMap<String, InterMineResultsContainer> referenceResults,
            HashMap<String, KeywordSearchFacetData> referenceFacetFields,
            Document doc)
        throws IllegalAccessException {

        // find all references and add them
        for (String reference : references) {
            InterMineResultsContainer resultsContainer =
                    referenceResults.get(reference);
            //step through the reference results (ordered) while ref.id = obj.id
            while (resultsContainer.getIterator().hasNext()) {
                @SuppressWarnings("rawtypes")
                ResultsRow next = resultsContainer.getIterator().next();

                // It is possible that the inner loop iterator "lags behind" the
                // current object's id. See:
                // https://github.com/intermine/intermine/issues/473
                while (resultsContainer.getIterator().hasNext()
                        && ((Integer) next.get(0)).compareTo(
                                object.getId()) == -1) {
                    next = resultsContainer.getIterator().next();
                }

                //reference is not for the current object?
                if (!next.get(0).equals(object.getId())) {
                    // go back one step
                    if (resultsContainer.getIterator().hasPrevious()) {
                        resultsContainer.getIterator().previous();
                    }

                    break;
                }

                // add reference to doc
                addObjectToDocument((InterMineObject) next.get(1), null, doc);

                //check if this reference contains an attribute we need for a facet
                KeywordSearchFacetData referenceFacet =
                        referenceFacetFields.get(reference);
                if (referenceFacet != null) {
                    //handle PATH facets FIXME: UNTESTED!
                    if (referenceFacet.getType() == KeywordSearchFacetType.PATH) {
                        String virtualPathField =
                                "path_" + referenceFacet.getName().toLowerCase();
                        for (String field : referenceFacet.getFields()) {
                            if (field.startsWith(reference + ".")) {
                                String facetAttribute =
                                        field.substring(field.lastIndexOf('.') + 1);
                                Object facetValue = ((InterMineObject) next.get(1))
                                    .getFieldValue(facetAttribute);

                                if (facetValue instanceof String
                                        && !StringUtils
                                                .isBlank((String) facetValue)) {
                                    Field f = doc.getField(virtualPathField);

                                    if (f != null) {
                                        f.setValue(f.stringValue() + "/"
                                                + facetValue);
                                    } else {
                                        doc.add(new Field(virtualPathField,
                                                (String) facetValue,
                                                Field.Store.NO,
                                                Field.Index.NOT_ANALYZED_NO_NORMS));
                                    }
                                }
                            }
                        }
                    } else {
                        //SINGLE/MULTI facet
                        //add attribute to document a second time, but unstemmed
                        //and with the field name corresponding to the facet name
                        String facetAttribute =
                                referenceFacet.getField()
                                        .substring(
                                                referenceFacet.getField()
                                                        .lastIndexOf('.') + 1);
                        Object facetValue = ((InterMineObject) next.get(1))
                            .getFieldValue(facetAttribute);

                        if (facetValue instanceof String
                                && !StringUtils.isBlank((String) facetValue)) {
                            doc.add(new Field(referenceFacet.getField(),
                                    (String) facetValue, Field.Store.NO,
                                    Field.Index.NOT_ANALYZED_NO_NORMS));
                        }
                    }
                }
            }
        }
    }

    private int iterateOverObjects(long time, long objectParseTime,
            HashSet<Class<? extends InterMineObject>> seenClasses,
            HashMap<String, InterMineResultsContainer> referenceResults,
            Results results, ListIterator<ResultsRow<InterMineObject>> it)
        throws PathException, ObjectStoreException, IllegalAccessException {
        int i = 0;
        int size = results.size();
        LOG.info("Query returned " + size + " results");

        //iterate over objects
        while (it.hasNext()) {
            ResultsRow<InterMineObject> row = it.next();

            if (i % 10000 == 1) {
                LOG.info("IMOFetcher: fetched " + i + " of " + size + " in "
                        + (System.currentTimeMillis() - time) + "ms total, "
                        + (objectParseTime) + "ms spent on parsing");
            }

            for (InterMineObject object : row) {
                Document doc = handleObject(object, seenClasses, referenceResults);

                // finally add doc to queue
                indexingQueue.put(doc);

            }

            i++;
        }
        return i;
    }

    private Document createDocument(InterMineObject object, ClassDescriptor classDescriptor) {
        Document doc = new Document();

        Float boost = classBoost.get(classDescriptor);
        if (boost != null) {
            doc.setBoost(boost.floatValue());
        }

        // id has to be stored so we can fetch the actual objects for the
        // results
        doc.add(new Field("id", object.getId().toString(), Field.Store.YES,
                Field.Index.NOT_ANALYZED_NO_NORMS));

        // special case for faceting
        doc.add(new Field("Category", classDescriptor.getUnqualifiedName(), Field.Store.NO,
                Field.Index.NOT_ANALYZED_NO_NORMS));

        addToDocument(doc, "classname", classDescriptor.getUnqualifiedName(), 1F, false);

        addObjectToDocument(object, classDescriptor, doc);

        return doc;
    }

    private void addObjectToDocument(InterMineObject object, ClassDescriptor classDescriptor,
            Document doc) {
        Collection<String> keyFields;

        // if we know the class, get a list of key fields
        if (classDescriptor != null) {
            keyFields =
                    ClassKeyHelper
                            .getKeyFieldNames(classKeys, classDescriptor.getUnqualifiedName());
        } else {
            keyFields = Collections.emptyList();
        }

        Set<ObjectValueContainer> attributes = getAttributeMapForObject(os.getModel(), object);
        for (ObjectValueContainer attribute : attributes) {
            addToDocument(doc, attribute.getLuceneName(), attribute.getValue(), 1F, false);

            // index all key fields as raw data with a higher boost, favors
            // "exact matches"
            if (keyFields.contains(attribute.getName())) {
                addToDocument(doc, attribute.getLuceneName(), attribute.getValue(), 2F, true);
            }
        }
    }

    private Set<String> getIgnorableFields(FastPathObject obj) {
        Set<String> ret = new HashSet<String>();
        for (Class<?> clazz: Util.decomposeClass(obj.getClass())) {
            if (ignoredFields.containsKey(clazz)) {
                ret.addAll(ignoredFields.get(clazz));
            }
        }
        return ret;
    }

    private Set<ObjectValueContainer> getAttributeMapForObject(Model model, FastPathObject obj) {
        Set<ObjectValueContainer> values = new HashSet<ObjectValueContainer>();
        Vector<ClassAttributes> decomposedClassAttributes =
                getClassAttributes(model, obj.getClass());

        Set<String> fieldsToIgnore = getIgnorableFields(obj);
        for (ClassAttributes classAttributes : decomposedClassAttributes) {
            for (AttributeDescriptor att : classAttributes.getAttributes()) {
                try {
                    // some fields are configured to ignore
                    if (fieldsToIgnore != null && fieldsToIgnore.contains(att.getName())) {
                        continue;
                    }
                    // only index strings and integers
                    if ("java.lang.String".equals(att.getType())
                            || "java.lang.Integer".equals(att.getType())) {
                        Object value = obj.getFieldValue(att.getName());

                        // ignore null values
                        if (value != null) {
                            String string = String.valueOf(value);

                            if (!string.startsWith("http://")) {
                                values.add(new ObjectValueContainer(classAttributes.getClassName(),
                                        att.getName(), string));
                            }

                            String prefix =
                                getAttributePrefix(classAttributes.getClassName(), att.getName());
                            if (prefix != null) {
                                String unPrefixedValue = string.substring(prefix.length());
                                values.add(new ObjectValueContainer(classAttributes.getClassName(),
                                        att.getName(), unPrefixedValue));
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Error introspecting an object: " + obj, e);
                }
            }
        }

        return values;
    }

    private String getAttributePrefix(String className, String attributeName) {
        if (attributePrefixes == null) {
            return null;
        }
        // for performance avoid joining strings in most cases
        Set<String> classesWithPrefix = new HashSet<String>();
        for (String clsAndAtt : attributePrefixes.keySet()) {
            String clsWithPrefix = clsAndAtt.substring(0, clsAndAtt.indexOf('.'));
            classesWithPrefix.add(clsWithPrefix);
        }

        if (classesWithPrefix.contains(className)) {
            StringBuilder clsAndAttribute = new StringBuilder();
            clsAndAttribute.append(className).append('.').append(attributeName);
            return attributePrefixes.get(clsAndAttribute.toString());
        }
        return null;
    }

    private Field addToDocument(Document doc, String fieldName, String value, float boost,
            boolean raw) {
        if (!StringUtils.isBlank(fieldName) && !StringUtils.isBlank(value)) {
            Field f;

            if (!raw) {
                f = new Field(fieldName, value.toLowerCase(), Field.Store.NO, Field.Index.ANALYZED);
            } else {
                f = new Field(fieldName + "_raw", value.toLowerCase(), Field.Store.NO,
                    Field.Index.NOT_ANALYZED);
            }

            f.setBoost(boost);

            // if we haven't set a boost and this is short field we can switch off norms
            if (boost == 1F && value.indexOf(' ') == -1) {
                f.setOmitNorms(true);
                f.setOmitTermFreqAndPositions(true);
                if (!normFields.contains(f.name())) {
                    normFields.add(f.name());
                }
            }
            // if this is a single word then we don't need positional information of terms in the
            // string.  NOTE - this may affect the boost applied to class keys.
//            if (raw || value.indexOf(' ') == -1) {
//                f.setOmitNorms(true);
//                f.setOmitTermFreqAndPositions(true);
//                if (!normFields.contains(f.name())) {
//                    normFields.add(f.name());
//                }
//            }

            doc.add(f);
            fieldNames.add(f.name());

            return f;
        }

        return null;
    }

    // simple caching of attributes
    private Vector<ClassAttributes> getClassAttributes(Model model, Class<?> baseClass) {
        Vector<ClassAttributes> attributes = decomposedClassesCache.get(baseClass);

        if (attributes == null) {
            LOG.info("decomposedClassesCache: No entry for " + baseClass + ", adding...");
            attributes = new Vector<ClassAttributes>();

            for (Class<?> cls : Util.decomposeClass(baseClass)) {
                ClassDescriptor cld = model.getClassDescriptorByName(cls.getName());
                attributes.add(new ClassAttributes(cld.getUnqualifiedName(), cld
                        .getAllAttributeDescriptors()));
            }

            decomposedClassesCache.put(baseClass, attributes);
        }

        return attributes;
    }

    private Query getPathQuery(String pathString) throws PathException {
        Query q = new Query();
        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

        org.intermine.pathquery.Path path =
                new org.intermine.pathquery.Path(os.getModel(), pathString);
        List<ClassDescriptor> classDescriptors = path.getElementClassDescriptors();
        List<String> fields = path.getElements();

        ClassDescriptor parentClassDescriptor = null;
        QueryClass parentQueryClass = null;

        for (int i = 0; i < classDescriptors.size(); i++) {
            ClassDescriptor classDescriptor = classDescriptors.get(i);

            Class<?> classInCollection = classDescriptor.getType();

            QueryClass queryClass = new QueryClass(classInCollection);
            q.addFrom(queryClass);

            if (i == 0) {
                // first class
                QueryField topId = new QueryField(queryClass, "id");
                q.addToSelect(topId);
                q.addToOrderBy(topId); // important for optimization in run()
            } else {
                if (parentClassDescriptor == null) {
                    continue;
                }
                String fieldName = fields.get(i - 1);
                if (parentClassDescriptor.getReferenceDescriptorByName(fieldName, true) != null) {
                    LOG.info(parentClassDescriptor.getType().getSimpleName() + " -> " + fieldName
                            + " (OBJECT)");
                    QueryObjectReference objectReference =
                            new QueryObjectReference(parentQueryClass, fieldName);
                    ContainsConstraint cc =
                            new ContainsConstraint(objectReference, ConstraintOp.CONTAINS,
                                    queryClass);
                    constraints.addConstraint(cc);
                } else if (parentClassDescriptor.getCollectionDescriptorByName(fieldName, true)
                        != null) {
                    LOG.info(parentClassDescriptor.getType().getSimpleName() + " -> " + fieldName
                            + " (COLLECTION)");
                    QueryCollectionReference collectionReference =
                            new QueryCollectionReference(parentQueryClass, fieldName);
                    ContainsConstraint cc =
                            new ContainsConstraint(collectionReference, ConstraintOp.CONTAINS,
                                    queryClass);
                    constraints.addConstraint(cc);
                } else {
                    LOG.warn("Unknown field '" + parentClassDescriptor.getUnqualifiedName()
                            + "'::'" + fieldName + "' in path '" + pathString + "'!");
                }
            }

            parentClassDescriptor = classDescriptor;
            parentQueryClass = queryClass;
        }

        q.setConstraint(constraints);
        q.addToSelect(parentQueryClass); // select last class

        return q;
    }
}
