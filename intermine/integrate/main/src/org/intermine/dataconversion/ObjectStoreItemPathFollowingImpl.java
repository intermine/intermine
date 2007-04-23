package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStorePassthruImpl;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CacheHoldingArrayList;
import org.intermine.util.CacheMap;
import org.intermine.util.StringUtil;

import org.apache.log4j.Logger;

/**
 * Provides an implementation of an objectstore that fetches additional items which will be required
 * by a DataTranslator, and provides a method to obtain those items from a cache.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreItemPathFollowingImpl extends ObjectStorePassthruImpl
{
    /**
     * This String should be used to construct item path rules that refer to the Item identifier
     * field, rather than the object field called "identifier". The test used to determine
     * whether a path rule refers to this String is by Object reference equality (==).
     */
    public static final String IDENTIFIER = "IDENTIFIER";
    /**
     * This String should be used to construct item path rules that refer to the Item className
     * field, rather than the object field called "className". The test used to determine
     * whether a path rule refers to this String is by Object reference equality (==).
     */
    public static final String CLASSNAME = "CLASSNAME";
    private static final Logger LOG = Logger.getLogger(ObjectStoreItemPathFollowingImpl.class);

    Map descriptiveCache = Collections.synchronizedMap(new CacheMap(
                "ObjectStoreItemPathFollowingImpl DescriptiveCache"));
    Map classNameToDescriptors = null;
    Set stackTraces = new HashSet();

    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreItemPathFollowingImpl(ObjectStore os) {
        super(os);
    }

    /**
     * Creates an instance, from another ObjectStore instance, with a path description.
     *
     * @param os an ObjectStore object to use
     * @param classNameToDescriptors a Map from className to Sets of ItemPrefetchDescriptor objects
     */
    public ObjectStoreItemPathFollowingImpl(ObjectStore os, Map classNameToDescriptors) {
        super(os);
        this.classNameToDescriptors = classNameToDescriptors;
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q) {
        return new Results(q, this, getSequence());
    }

    /**
     * {@inheritDoc}
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        List retvalList = os.execute(q, start, limit, optimise, explain, sequence);
        if (classNameToDescriptors == null) {
            return retvalList;
        } else {
            CacheHoldingArrayList retval;
            if (retvalList instanceof CacheHoldingArrayList) {
                retval = (CacheHoldingArrayList) retvalList;
            } else {
                retval = new CacheHoldingArrayList(retvalList);
            }
            if (retval.size() > 0) {
                if ((q.getSelect().size() == 1)
                        && (q.getSelect().get(0) instanceof QueryClass)) {
                    if (Item.class.equals(((QueryClass) q.getSelect().get(0)).getType())) {
                        fetchRelated(retval);
                    }
                }
            }
            return retval;
        }
    }

    private int misses = 0;
    private int ops = 0;

    /**
     * This method takes a description of Items to fetch, and returns a List of such Items.
     * The description is a Set of FieldNameAndValue objects.
     *
     * @param description a Set of FieldNameAndValue objects
     * @return a List of Item objects
     */
    public List getItemsByDescription(Set description) {
        List retval = (List) descriptiveCache.get(description);
        ops++;
        if (retval == null) {
            misses++;
            Query q = new Query();
            QueryClass item = new QueryClass(Item.class);
            q.addFrom(item);
            q.addToSelect(item);
            q.setDistinct(false);
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            Iterator descrIter = description.iterator();
            while (descrIter.hasNext()) {
                FieldNameAndValue f = (FieldNameAndValue) descrIter.next();
                if (f.isReference() && (f.getFieldName() == IDENTIFIER)) {
                    QueryField qf = new QueryField(item, "identifier");
                    String[] refids = StringUtil.split(f.getValue(), " ");
                    List refidList = new ArrayList();
                    for (int i = 0; i < refids.length; i++) {
                        refidList.add(refids[i]);
                    }
                    BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, refidList);
                    cs.addConstraint(bc);
                } else if (((f.getFieldName() == IDENTIFIER) || (f.getFieldName() == CLASSNAME))
                        && (!f.isReference())) {
                    QueryField qf = new QueryField(item, (f.getFieldName() == IDENTIFIER
                                ? "identifier" : "className"));
                    SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                            new QueryValue(f.getValue()));
                    cs.addConstraint(c);
                } else {
                    QueryClass attribute = new QueryClass(f.isReference() ? Reference.class
                            : Attribute.class);
                    q.addFrom(attribute);
                    QueryCollectionReference r = new QueryCollectionReference(item,
                            (f.isReference() ? "references" : "attributes"));
                    ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS,
                            attribute);
                    cs.addConstraint(cc);
                    QueryField qf = new QueryField(attribute, "name");
                    SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                            new QueryValue(f.getFieldName()));
                    cs.addConstraint(c);
                    qf = new QueryField(attribute, (f.isReference() ? "refId" : "value"));
                    c = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(f.getValue()));
                    cs.addConstraint(c);
                }
            }
            q.setConstraint(cs);
            //LOG.debug("Fetching Items by description: " + description + ", query = "
            //        + q.toString());
            retval = new SingletonResults(q, os, os.getSequence());
            ((SingletonResults) retval).setBatchSize(1000);
            ((SingletonResults) retval).setNoExplain();
            ((SingletonResults) retval).setNoOptimise();
            descriptiveCache.put(description, retval);
            try {
                Exception e = new Exception("Prefetch cache miss");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.fillInStackTrace();
                e.printStackTrace(pw);
                pw.close();
                sw.close();
                String stackTrace = sw.toString();
                if (!stackTraces.contains(stackTrace)) {
                    stackTraces.add(stackTrace);
                    LOG.warn(stackTrace);
                }
            } catch (IOException e) {
                // Shouldn't be possible
            }
        }
        if (ops % 100000 == 0) {
            LOG.info("getItemsByDescription: ops = " + ops + ", misses = " + misses
                    + " cache size: " + descriptiveCache.size());
        }
        return retval;
    }

    /**
     * This method fetches the related Items to the given List of items, and places them in the
     * cache and in the holder part of the given list.
     *
     * @param batch the List of items
     */
    private void fetchRelated(CacheHoldingArrayList batch) {
        // preform deep clone on classNameToDescriptors map -> need deepClone() method on all
        // enclosed stuff
        Map classNameToDescriptorsLocal = cloneClassNameToDescriptors();
        Map descriptorToConstraints = new HashMap();
        Iterator iter = batch.iterator();
        while (iter.hasNext()) {
            Item item = (Item) ((ResultsRow) iter.next()).get(0);
            Set descriptors = (Set) classNameToDescriptorsLocal.get(item.getClassName());
            if (descriptors != null) {
                Iterator descIter = descriptors.iterator();
                while (descIter.hasNext()) {
                    ItemPrefetchDescriptor desc = (ItemPrefetchDescriptor) descIter.next();
                    try {
                        // causes getConstraint to be called on ItemPrefetchConstraints in
                        // descriptors, for ItemPrefetchConstraintDynamic this sets up an
                        // internal map for that item that needs to be accessed later
                        Set constraint = desc.getConstraint(item);
                        Set constraints = (Set) descriptorToConstraints.get(desc);
                        if (constraints == null) {
                            constraints = new HashSet();
                            descriptorToConstraints.put(desc, constraints);
                        }
                        constraints.add(constraint);
                    } catch (IllegalArgumentException e) {
                        // Alright - the item didn't match the prefetch info. It's only a hint.
                        /*StringWriter sw = new StringWriter();
                        try {
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            pw.close();
                            sw.close();
                        } catch (IOException e2) {
                            LOG.error("IOException while creating stack trace");
                        }
                        LOG.warn("IllegalArgumentException while finding constraint for " + desc
                                + " applied to " + item + " - Exception: " + sw.toString());*/
                    }
                }
            }
        }

        // Now, we have a Map from descriptor to a set of constraints, where a constraint is a Set
        // of FieldNameAndValue objects. We can put this data onto a queue and process it that way,
        // because processing an element will involve creating more descriptor-setofconstraints
        // pairs.

        LinkedList queue = new LinkedList();
        Iterator entryIter = descriptorToConstraints.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            ItemPrefetchDescriptor desc = (ItemPrefetchDescriptor) entry.getKey();
            Set constraints = (Set) entry.getValue();
            DescriptorAndConstraints dac = new DescriptorAndConstraints(desc, constraints);
            queue.add(dac);
        }

        // Now we have a queue with DescriptorAndConstraints objects. Each of these will result in
        // a single query being executed, which may result in extra DescriptorAndConstraints objects
        // being put onto the queue.

        while (!queue.isEmpty()) {
            long start = (new Date()).getTime();
            DescriptorAndConstraints dac = (DescriptorAndConstraints) queue.removeFirst();
            Iterator conIter = dac.constraints.iterator();
            while (conIter.hasNext()) {
                Set constraint = (Set) conIter.next();
                List results = (List) descriptiveCache.get(constraint);
                if (results != null) {
                    batch.addToHolder(results);
                    conIter.remove();
                }
            }
            //LOG.debug(dac.descriptor + " -> (size = " + originalSize + " -> "
            //        + dac.constraints.size() + ") " + dac.constraints);

            // So, we have a constraints Set, with constraints that have all been generated from the
            // same ItemPrefetchDescriptor. So, they can all be fetched in the same query.


            if (dac.constraints.size() > 0) {
                Query q = buildQuery(dac);
                //LOG.debug(dac.descriptor + " -> " + q.toString());

                Map constraintToList = new HashMap();
                conIter = dac.constraints.iterator();
                while (conIter.hasNext()) {
                    constraintToList.put(conIter.next(), new ArrayList());
                }

                long afterQuery = (new Date()).getTime();
                SingletonResults results = new SingletonResults(q, os, os.getSequence());
                results.setBatchSize(10000);
                results.setNoExplain();
                results.setNoOptimise();
                long afterExecute = 0;
                Iterator resIter = results.iterator();
                while (resIter.hasNext()) {
                    if (afterExecute == 0) {
                        afterExecute = (new Date()).getTime();
                    }
                    Item item = (Item) resIter.next();
                    Set constraints = dac.descriptor.getConstraintFromTarget(item);
                    Iterator constraintIter = constraints.iterator();
                    while (constraintIter.hasNext()) {
                        Set constraint = (Set) constraintIter.next();
                        List conResults = (List) constraintToList.get(constraint);
                        if (conResults == null) {
                            LOG.error("conResults is null. constraintToList = \"" + constraintToList
                                    + "\", constraint = \"" + constraint);
                        }
                        conResults.add(item);
                    }
                }

                conIter = constraintToList.entrySet().iterator();
                while (conIter.hasNext()) {
                    Map.Entry conEntry = (Map.Entry) conIter.next();
                    Set constraint = (Set) conEntry.getKey();
                    List conResults = (List) conEntry.getValue();
                    batch.addToHolder(conResults);
                    descriptiveCache.put(constraint, conResults);
                    //LOG.debug("Created cache lookup for " + constraint + " to " + conResults);
                }

                // Now follow the path on...
                Iterator descIter = dac.descriptor.getPaths().iterator();
                while (descIter.hasNext()) {
                    ItemPrefetchDescriptor descriptor = (ItemPrefetchDescriptor) descIter.next();
                    Set constraints = new HashSet();
                    resIter = results.iterator();
                    while (resIter.hasNext()) {
                        Item item = (Item) resIter.next();
                        try {
                            Set constraint = descriptor.getConstraint(item);
                            constraints.add(constraint);
                        } catch (IllegalArgumentException e) {
                            /*StringWriter sw = new StringWriter();
                            try {
                                PrintWriter pw = new PrintWriter(sw);
                                e.printStackTrace(pw);
                                pw.close();
                                sw.close();
                            } catch (IOException e2) {
                                LOG.error("IOException while creating stack trace");
                            }
                            LOG.warn("IllegalArgumentException while finding constraint for "
                                    + descriptor + " applied to " + item + " - Exception: "
                                    + sw.toString());*/
                        }
                    }
                    queue.add(new DescriptorAndConstraints(descriptor, constraints));
                }

                long now = (new Date()).getTime();
                LOG.debug("Prefetched " + results.size() + " Items. Took " + (afterQuery - start)
                         + " ms to build query, " + (afterExecute - afterQuery) + " ms to execute, "
                         + (now - afterExecute) + " ms to process results for " + dac.descriptor);
            }
        }
    }

    private Query buildQuery(DescriptorAndConstraints dac) {
        // We can use the first constraint to set up the query
        Set constraint = (Set) dac.constraints.iterator().next();
        Query q = new Query();
        QueryClass itemClass = new QueryClass(Item.class);
        q.addFrom(itemClass);
        q.addToSelect(itemClass);
        q.setDistinct(false);
        ConstraintSet mainCs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(mainCs);
        Map references = new HashMap();
        Map attributes = new HashMap();
        QueryField identifier = new QueryField(itemClass, "identifier");
        QueryField className = new QueryField(itemClass, "className");
        Iterator descrIter = constraint.iterator();
        while (descrIter.hasNext()) {
            FieldNameAndValue f = (FieldNameAndValue) descrIter.next();
            if (f.isReference()) {
                if (f.getFieldName() == IDENTIFIER) {
                    if (dac.descriptor.isStatic(f)) {
                        String[] refids = StringUtil.split(f.getValue(), " ");
                        List refidList = new ArrayList();
                        for (int i = 0; i < refids.length; i++) {
                            refidList.add(refids[i]);
                        }
                        BagConstraint bc = new BagConstraint(identifier,
                                ConstraintOp.IN, refidList);
                        mainCs.addConstraint(bc);
                    }
                } else {
                    QueryClass reference = new QueryClass(Reference.class);
                    q.addFrom(reference);
                    QueryCollectionReference r = new QueryCollectionReference(itemClass,
                            "references");
                    ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS,
                            reference);
                    mainCs.addConstraint(cc);
                    QueryField qf = new QueryField(reference, "name");
                    mainCs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                new QueryValue(f.getFieldName())));
                    if (dac.descriptor.isStatic(f)) {
                        qf = new QueryField(reference, "refId");
                        mainCs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                    new QueryValue(f.getValue())));
                    } else {
                        references.put(f.getFieldName(), reference);
                    }
                }
            } else {
                if (f.getFieldName() == IDENTIFIER) {
                    if (dac.descriptor.isStatic(f)) {
                        mainCs.addConstraint(new SimpleConstraint(identifier,
                                    ConstraintOp.EQUALS, new QueryValue(f.getValue())));
                    }
                } else if (f.getFieldName() == CLASSNAME) {
                    if (dac.descriptor.isStatic(f)) {
                        mainCs.addConstraint(new SimpleConstraint(className,
                                    ConstraintOp.EQUALS, new QueryValue(f.getValue())));
                    }
                } else {
                    QueryClass attribute = new QueryClass(Attribute.class);
                    q.addFrom(attribute);
                    QueryCollectionReference r = new QueryCollectionReference(itemClass,
                            "attributes");
                    ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS,
                            attribute);
                    mainCs.addConstraint(cc);
                    QueryField qf = new QueryField(attribute, "name");
                    mainCs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                new QueryValue(f.getFieldName())));
                    if (dac.descriptor.isStatic(f)) {
                        qf = new QueryField(attribute, "value");
                        mainCs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                    new QueryValue(f.getValue())));
                    } else {
                        attributes.put(f.getFieldName(), attribute);
                    }
                }
            }
        }
        ConstraintSet orCs = new ConstraintSet(ConstraintOp.OR);
        mainCs.addConstraint(orCs);

        // Now we have the framework of the query. Each constraint must use the correct
        // number of attributes and references.

        Iterator conIter = dac.constraints.iterator();
        while (conIter.hasNext()) {
            constraint = (Set) conIter.next();
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            descrIter = constraint.iterator();
            while (descrIter.hasNext()) {
                FieldNameAndValue f = (FieldNameAndValue) descrIter.next();
                if (!dac.descriptor.isStatic(f)) {
                    if (f.isReference()) {
                        if (f.getFieldName() == IDENTIFIER) {
                            String[] refids = StringUtil.split(f.getValue(), " ");
                            List refidList = new ArrayList();
                            for (int i = 0; i < refids.length; i++) {
                                refidList.add(refids[i]);
                            }
                            BagConstraint bc = new BagConstraint(identifier, ConstraintOp.IN,
                                    refidList);
                            cs.addConstraint(bc);
                        } else {
                            QueryClass reference = (QueryClass) references.get(f.getFieldName());
                            QueryField qf = new QueryField(reference, "refId");
                            cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                        new QueryValue(f.getValue())));
                        }
                    } else {
                        if (f.getFieldName() == IDENTIFIER) {
                            cs.addConstraint(new SimpleConstraint(identifier,
                                        ConstraintOp.EQUALS, new QueryValue(f.getValue())));
                        } else if (f.getFieldName() == CLASSNAME) {
                            cs.addConstraint(new SimpleConstraint(className,
                                        ConstraintOp.EQUALS, new QueryValue(f.getValue())));
                        } else {
                            QueryClass attribute = (QueryClass) attributes.get(f.getFieldName());
                            QueryField qf = new QueryField(attribute, "value");
                            cs.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS,
                                        new QueryValue(f.getValue())));
                        }
                    }
                }
            }
            orCs.addConstraint(cs);
        }
        // We have a query.
        return q;
    }

    // create a deep clone of classNameToDescriptors map to prevent memory leaking
    private Map cloneClassNameToDescriptors() {
        Map clone = new HashMap();
        Iterator iter = classNameToDescriptors.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String clsName = (String) entry.getKey();
            Set cloneDescSet = new HashSet();
            Iterator descIter = ((Set) entry.getValue()).iterator();
            while (descIter.hasNext()) {
                ItemPrefetchDescriptor descriptor
                    = ((ItemPrefetchDescriptor) descIter.next()).deepClone();
                cloneDescSet.add(descriptor);
            }
            clone.put(clsName, cloneDescSet);
        }
        return clone;
    }

    private static class DescriptorAndConstraints
    {
        ItemPrefetchDescriptor descriptor;
        Set constraints;

        public DescriptorAndConstraints(ItemPrefetchDescriptor descriptor, Set constraints) {
            this.descriptor = descriptor;
            this.constraints = constraints;
        }
    }
}
