package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.lucene.document.Field.Store;
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
import org.intermine.pathquery.Path;
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
    private static final Store STORE_FIELD = Field.Store.YES;

    private static final String CATEGORY = "Category";

    private static final Set<String> INDEXABLE_TYPES = new HashSet<String>(Arrays.asList(
        "java.lang.String", "java.lang.Integer", "int", "java.lang.Long", "long"
    ));

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
     * Construct a document fetcher thread.
     * @param os The object store
     * @param classKeys The class keys
     * @param indexingQueue The indexing queue to report object we find on.
     * @param config The configuration.
     */
    public InterMineObjectFetcher(
            ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys,
            ObjectPipe<Document> indexingQueue,
            Configuration config) {
        this(os, classKeys, indexingQueue,
                config.getIgnoredClasses(), config.getIgnoredFields(),
                config.getSpecialReferences(), config.getClassBoost(), config.getFacets(),
                config.getAttributePrefixes());
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

    @SuppressWarnings("unchecked")
    private Document handleObject(
            InterMineObject object,
            HashSet<Class<? extends InterMineObject>> seenClasses,
            HashMap<String, InterMineResultsContainer> referenceResults)
        throws PathException, ObjectStoreException, IllegalAccessException {
        long objectParseStart = System.currentTimeMillis();
        long objectParseTime = 0L;
        Model model = os.getModel();

        // Construct a set of all the class-descriptors relevant to this object.
        Set<Class<?>> clazzes = Util.decomposeClass(object.getClass());
        Set<ClassDescriptor> classDescs = new HashSet<ClassDescriptor>();
        for (Class<?> clazz: clazzes) {
            classDescs.add(model.getClassDescriptorByName(clazz.getName()));
        }

        // create base doc for object
        Document doc = createDocument(object, classDescs);

        HashMap<String, KeywordSearchFacetData> referenceFacetFields =
                new HashMap<String, KeywordSearchFacetData>();

        // find all references associated with this object or
        // its superclasses
        Map<ClassDescriptor, Set<String>> references = determineReferences(
                classDescs, referenceFacetFields);

        // if we have not seen an object of this class before, query references
        for (ClassDescriptor cld: classDescs) {
            Class<? extends FastPathObject> clazz = cld.getType();
            if (!seenClasses.contains(clazz)) {
                LOG.info("Getting references for new class: " + clazz);

                // query all references that we need
                for (String reference : references.get(cld)) {
                    // do not count this towards objectParseTime
                    objectParseTime += (System.currentTimeMillis() - objectParseStart);
                    queryReference(referenceResults, reference);
                    // start counting objectParseTime again
                    objectParseStart = System.currentTimeMillis();
                }

                seenClasses.add((Class<? extends InterMineObject>) clazz);
            }
        }


        addAllReferences(
            object, referenceResults, doc,
            referenceFacetFields, references
        );

        objectParseTime += (System.currentTimeMillis() - objectParseTime);
        return doc;
    }

    private void queryReference(
            HashMap<String, InterMineResultsContainer> referenceResults,
            String reference) throws PathException, ObjectStoreException {
        LOG.debug("Querying reference " + reference);
        Query queryReference = getPathQuery(reference);
        Results resultsc = os.execute(queryReference, 1000, true, false,
                true);
        ((ObjectStoreInterMineImpl) os).goFaster(queryReference);
        referenceResults.put(reference, new InterMineResultsContainer(resultsc));
        LOG.debug("Querying reference " + reference + " done -- "
                + resultsc.size() + " results");
    }

    private void addAllReferences(InterMineObject imo,
            HashMap<String, InterMineResultsContainer> referenceResults,
            Document doc,
            HashMap<String, KeywordSearchFacetData> referenceFacetFields,
            Map<ClassDescriptor, Set<String>> references)
            throws IllegalAccessException {
        // find all references and add them
        for (Set<String> refSet: references.values()) {
            for (String reference: refSet) {
                InterMineResultsContainer resultsContainer = referenceResults.get(reference);
                //step through the reference results (ordered) while ref.id = obj.id
                ListIterator<ResultsRow<InterMineObject>> rows = resultsContainer.getIterator();
                while (rows.hasNext()) {
                    @SuppressWarnings("rawtypes")
                    // ResultRow has two columns: 0 = id, 1 = InterMineObject
                    ResultsRow next = rows.next();

                    // It is possible that the inner loop iterator "lags behind" the
                    // current object's id. See:
                    // https://github.com/intermine/intermine/issues/473
                    // so we advance up to the current object if we are behind it.
                    while (rows.hasNext() && ((Integer) next.get(0)).compareTo(imo.getId()) < 0) {
                        next = rows.next();
                    }

                    // reference is not for the current object?
                    if (!next.get(0).equals(imo.getId())) {
                        // go back one step, if we can
                        if (rows.hasPrevious()) {
                            rows.previous();
                        }

                        break;
                    }

                    // add reference to doc
                    InterMineObject ref = (InterMineObject) next.get(1);
                    addObjectToDocument(ref, reference, doc);

                    //check if this reference contains an attribute we need for a facet
                    KeywordSearchFacetData referenceFacet = referenceFacetFields.get(reference);
                    if (referenceFacet != null) {
                        //handle PATH facets FIXME: UNTESTED!
                        if (referenceFacet.getType() == KeywordSearchFacetType.PATH) {
                            String virtualPathField =
                                    "path_" + referenceFacet.getName().toLowerCase();
                            for (String field : referenceFacet.getFields()) {
                                if (field.startsWith(reference + ".")) {
                                    String facetAttribute =
                                            field.substring(field.lastIndexOf('.') + 1);
                                    Object facetValue = ref.getFieldValue(facetAttribute);

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
                                                    STORE_FIELD,
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
                            Object facetValue = ref.getFieldValue(facetAttribute);

                            if (facetValue instanceof String
                                    && !StringUtils.isBlank((String) facetValue)) {
                                doc.add(new Field(referenceFacet.getField(),
                                        (String) facetValue, STORE_FIELD,
                                        Field.Index.NOT_ANALYZED_NO_NORMS));
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<ClassDescriptor, Set<String>> determineReferences(
            Set<ClassDescriptor> classDescs,
            HashMap<String, KeywordSearchFacetData> referenceFacetFields) {
        Map<ClassDescriptor, Set<String>> references = new HashMap<ClassDescriptor, Set<String>>();
        for (Entry<Class<? extends InterMineObject>, String[]> specialClass
                : specialReferences.entrySet()) {
            Class<? extends InterMineObject> superType = specialClass.getKey();
            for (ClassDescriptor cld: classDescs) {
                Class<?> type = cld.getType();
                Set<String> refs = new HashSet<String>();
                references.put(cld, refs);
                if (superType.isAssignableFrom(type)) {
                    for (String reference : specialClass.getValue()) {
                        // TODO: avoid the stringification - we have the cld, so we don't need
                        // to roundtrip through the path resolution mechanism.
                        String fullReference = cld.getUnqualifiedName() + "." + reference;
                        refs.add(fullReference);

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
        return references;
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

    private Float getMaximumBoost(Collection<ClassDescriptor> classDescriptors) {
        Float boost = null;
        for (ClassDescriptor cld: classDescriptors) {
            Float thisBoost = classBoost.get(cld);
            if (thisBoost == null) {
                continue;
            }
            if (boost == null || thisBoost.compareTo(boost) > 0) {
                boost = thisBoost;
            }
        }
        return boost;
    }

    private Document
    createDocument(InterMineObject object, Collection<ClassDescriptor> classDescriptors) {
        Document doc = new Document();

        Float boost = getMaximumBoost(classDescriptors);
        if (boost != null) {
            doc.setBoost(boost.floatValue());
        }

        // id has to be stored so we can fetch the actual objects for the
        // results
        doc.add(new Field("id", object.getId().toString(),
                STORE_FIELD,
                Field.Index.NOT_ANALYZED_NO_NORMS));

        // Documents have one classname per cld, and multiple Categories
        for (ClassDescriptor cld: classDescriptors) {
            addToDocument(doc, "classname", cld.getUnqualifiedName(), 1F, true);
            addCategory(doc, cld);
            for (ClassDescriptor superCld: cld.getAllSuperDescriptors()) {
                addCategory(doc, superCld);
            }

            addObjectToDocument(object, cld, doc);
        }

        return doc;
    }

    private void addCategory(Document doc, ClassDescriptor cld) {
        doc.add(new Field(CATEGORY, cld.getUnqualifiedName(),
                STORE_FIELD,
                Field.Index.NOT_ANALYZED_NO_NORMS));
    }

    private void addObjectToDocument(
            InterMineObject object,
            ClassDescriptor classDescriptor,
            Document doc) {
        addObjectToDocument(object, classDescriptor, doc, "");
    }

    private void addObjectToDocument(InterMineObject object, String ref, Document doc) {
        addObjectToDocument(object, null, doc, ref);
    }

    private void addObjectToDocument(
            InterMineObject object, ClassDescriptor cld, Document doc, String prefix) {
        Collection<String> keyFields;

        // if we know the class, get a list of key fields
        if (cld == null) {
            keyFields = Collections.emptyList();
        } else {
            keyFields = ClassKeyHelper.getKeyFieldNames(classKeys, cld.getUnqualifiedName());
        }

        Set<ObjectValueContainer> attributes =
                getAttributeMapForObject(object, prefix);
        for (ObjectValueContainer a : attributes) {
            LOG.info("Adding " + a.getLuceneName() + " = " + a.getValue() + " to document");
            addToDocument(doc, a.getLuceneName(), a.getValue(), a.getBoost(), false);

            // index all key fields as raw data with a higher boost, favors
            // "exact matches"
            if (keyFields.contains(a.getName())) {
                addToDocument(doc, a.getLuceneName(), a.getValue(), (2 * a.getBoost()), true);
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

    private Set<ObjectValueContainer> getAttributeMapForObject(FastPathObject obj, String ns) {
        Model model = os.getModel();
        Set<ObjectValueContainer> values = new HashSet<ObjectValueContainer>();
        Vector<ClassAttributes> decomposedClassAttributes =
                getClassAttributes(model, obj.getClass());

        Set<String> fieldsToIgnore = getIgnorableFields(obj);
        for (ClassAttributes classAttributes : decomposedClassAttributes) {
            for (AttributeDescriptor att : classAttributes.getAttributes()) {
                try {
                    // some fields are configured to ignore
                    final String attrName = att.getName();
                    if (fieldsToIgnore != null && fieldsToIgnore.contains(attrName)) {
                        continue;
                    }

                    // only index strings and integral numbers
                    if (!INDEXABLE_TYPES.contains(att.getType())) {
                        continue;
                    }
                    Object value = obj.getFieldValue(attrName);
                    // Skip null values.
                    if (value == null) {
                        continue;
                    }
                    String string = String.valueOf(value);
                    String className = classAttributes.getClassName();

                    if (!string.startsWith("http://")) {
                        values.add(new ObjectValueContainer(ns, className, attrName, string));
                    }

                    String prefix =
                        getAttributePrefix(className, attrName);
                    if (prefix != null) {
                        String unPrefixed = string.substring(prefix.length());
                        values.add(new ObjectValueContainer(ns, className, attrName, unPrefixed));
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
        if (StringUtils.isBlank(fieldName) || StringUtils.isBlank(value)) {
            return null;
        }
        String normed = value.toLowerCase();

        Field f = raw
            ? new Field(fieldName + "_raw", normed, STORE_FIELD, Field.Index.NOT_ANALYZED)
            : new Field(fieldName, normed, STORE_FIELD, Field.Index.ANALYZED);

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

        Path path = new Path(os.getModel(), pathString);
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
