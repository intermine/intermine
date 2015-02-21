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
import org.intermine.api.query.MainHelper;
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
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.ObjectPipe;

/**
 * thread to fetch all intermineobjects (with exceptions) from database, create
 * a lucene document for them, add references (if applicable) and put the final
 * document in the indexing queue
 * @author nils
 * @author Alex Kalderimis
 */
public class InterMineObjectFetcher extends Thread
{
    private static final Store STORE_FIELD = Field.Store.NO;

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
    final Map<ClassDescriptor, String[]> specialReferences;
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
            Map<ClassDescriptor, String[]> specialReferences,
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
            LOG.debug("Fetching all InterMineObjects...");

            Set<ClassDescriptor> seenClasses = new HashSet<ClassDescriptor>();
            Map<String, InterMineResultsContainer> referenceResults =
                    new HashMap<String, InterMineResultsContainer>();

            try {
                //query all objects except the ones we are ignoring
                Query q = new Query();
                QueryClass qc = new QueryClass(InterMineObject.class);
                q.addFrom(qc);
                q.addToSelect(qc);

                QueryField qf = new QueryField(qc, "class");
                q.setConstraint(new BagConstraint(qf, ConstraintOp.NOT_IN, ignoredClasses));

                LOG.debug("QUERY: " + q.toString());

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
                LOG.debug("COMPLETED index with " + i + " records.  Fields: " + doneMessage);
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
            Set<ClassDescriptor> seenClasses,
            Map<String, InterMineResultsContainer> referenceResults)
        throws PathException, ObjectStoreException, IllegalAccessException {
        long objectParseStart = System.currentTimeMillis();
        long objectParseTime = 0L;
        Model model = os.getModel();

        // Construct a set of all the class-descriptors relevant to this object.
        Set<ClassDescriptor> classDescs = model.getClassDescriptorsForClass(object.getClass());

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
            if (seenClasses.contains(cld) || !references.containsKey(cld)) {
                continue;
            }

            // query all references that we need
            for (String reference : references.get(cld)) {
                // do not count this towards objectParseTime
                objectParseTime += (System.currentTimeMillis() - objectParseStart);
                queryReference(referenceResults, reference);
                // start counting objectParseTime again
                objectParseStart = System.currentTimeMillis();
            }

            seenClasses.add(cld);
        }

        addAllReferences(
            object, referenceResults, doc,
            referenceFacetFields, references
        );

        objectParseTime += (System.currentTimeMillis() - objectParseTime);
        return doc;
    }

    private void queryReference(
            Map<String, InterMineResultsContainer> referenceResults,
            String reference) throws PathException, ObjectStoreException {
        LOG.debug("Querying reference " + reference);
        Query queryReference = getOSQuery(reference);
        Results resultsc = os.execute(queryReference, 1000, true, false, true);
        ((ObjectStoreInterMineImpl) os).goFaster(queryReference);
        referenceResults.put(reference, new InterMineResultsContainer(resultsc));
        LOG.debug("Querying reference " + reference + " done -- "
                + resultsc.size() + " results");
    }

    private void addAllReferences(InterMineObject imo,
            Map<String, InterMineResultsContainer> referenceResults,
            Document doc,
            Map<String, KeywordSearchFacetData> referenceFacetFields,
            Map<ClassDescriptor, Set<String>> references)
            throws IllegalAccessException {
        // find all references and add them
        for (Set<String> refSet: references.values()) {
            for (String reference: refSet) {
                InterMineResultsContainer resultsContainer = referenceResults.get(reference);
                //step through the reference results (ordered) while ref.id = obj.id
                ListIterator<ResultsRow<InterMineObject>> rows = resultsContainer.getIterator();
                ROWS: while (rows.hasNext()) {
                    // ResultRow has two columns: 0 = id, 1 = InterMineObject
                    ResultsRow<InterMineObject> next = rows.next();

                    // It is possible that the inner loop iterator "lags behind" the
                    // current object's id. See:
                    // https://github.com/intermine/intermine/issues/473
                    // so we advance up to the current object if we are behind it.
                    while (next.get(0).getId().compareTo(imo.getId()) < 0) {
                        continue ROWS;
                    }

                    // reference is not for the current object?
                    // (ie. this object doesn't have this ref)
                    if (!next.get(0).equals(imo)) {
                        // rewind one step, if we can
                        if (rows.hasPrevious()) {
                            rows.previous();
                        }

                        break ROWS;
                    }

                    // add reference to doc
                    InterMineObject ref = next.get(1);
                    addObjectToDocument(ref, reference, doc);

                    //check if this reference contains an attribute we need for a facet
                    KeywordSearchFacetData referenceFacet = referenceFacetFields.get(reference);
                    if (referenceFacet != null) {
                        handleReferenceFacet(doc, reference, ref, referenceFacet);
                    }
                }
            }
        }
    }

    private void handleReferenceFacet(Document doc, String reference,
            InterMineObject ref, KeywordSearchFacetData referenceFacet)
            throws IllegalAccessException {
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

    private Map<ClassDescriptor, Set<String>> determineReferences(
            Set<ClassDescriptor> classDescs,
            HashMap<String, KeywordSearchFacetData> referenceFacetFields) {

        Map<ClassDescriptor, Set<String>> references = new HashMap<ClassDescriptor, Set<String>>();

        for (Entry<ClassDescriptor, String[]> specialClass: specialReferences.entrySet()) {
            ClassDescriptor hasRefs = specialClass.getKey();
            for (ClassDescriptor cld: classDescs) {
                if (hasRefs.equals(cld)) {
                    Set<String> refs = new HashSet<String>();
                    references.put(cld, refs);
                    for (String fieldExpr : specialClass.getValue()) {
                        // TODO: avoid the stringification - we have the cld, so we don't need
                        // to roundtrip through the path resolution mechanism.
                        String fullReference = cld.getUnqualifiedName() + "." + fieldExpr;
                        refs.add(fullReference);

                        //check if this reference returns a field we are
                        //faceting by. if so, add it to referenceFacetFields
                        for (KeywordSearchFacetData facet : facets) {
                            for (String field : facet.getFields()) {
                                if (field.startsWith(fieldExpr + ".")
                                        && !field.substring(fieldExpr.length() + 1)
                                                .contains(".")) {
                                    referenceFacetFields.put(fullReference, facet);
                                }
                            }
                        }
                    }

                    break;
                }
            }
        }

        return references;
    }

    private int iterateOverObjects(long time, long objectParseTime,
            Set<ClassDescriptor> seenClasses,
            Map<String, InterMineResultsContainer> referenceResults,
            Results results, ListIterator<ResultsRow<InterMineObject>> it)
        throws PathException, ObjectStoreException, IllegalAccessException {
        int i = 0;
        int size = results.size();
        LOG.debug("Query returned " + size + " results");

        //iterate over objects
        while (it.hasNext()) {
            ResultsRow<InterMineObject> row = it.next();

            if (i % 10000 == 1) {
                LOG.debug("IMOFetcher: fetched " + i + " of " + size + " in "
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
        // results - everything else uses STORE_FIELD
        doc.add(new Field("id", object.getId().toString(),
                Field.Store.YES,
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
        if ("InterMineObject".equals(cld.getUnqualifiedName())) {
            return;
        }
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
            LOG.debug("decomposedClassesCache: No entry for " + baseClass + ", adding...");
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

    private Query getOSQuery(String pathString) throws PathException {
        Path path = new Path(os.getModel(), pathString);
        if (path.endIsAttribute()) {
            throw new RuntimeException("The path must be a reference, not an attribute");
        }
        if (path.isRootPath()) {
            throw new RuntimeException("The path must be a reference, not a root path");
        }
        Path rootPath = path.getPrefix();
        while (!rootPath.isRootPath()) {
            rootPath = rootPath.getPrefix();
        }

        PathQuery pq = new PathQuery(os.getModel());
        pq.addViews(
            rootPath.append("id").toString(),
            path.append("id").toString()
        );
        pq.addOrderBy(rootPath.append("id").toString(), OrderDirection.ASC);

        return MainHelper.makeSimpleQuery(pq);
    }
}
