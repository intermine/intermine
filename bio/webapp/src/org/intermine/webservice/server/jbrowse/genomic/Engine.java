package org.intermine.webservice.server.jbrowse.genomic;

import static java.lang.String.format;
import static org.intermine.pathquery.Constraints.eq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CacheMap;
import org.intermine.util.DynamicUtil;
import org.intermine.webservice.server.jbrowse.Command;
import org.intermine.webservice.server.jbrowse.CommandRunner;
import org.intermine.webservice.server.jbrowse.Segment;

import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;

/**
 * An adaptor for running JBrowse queries against a genomic database.
 *
 * <p>
 * This engine is written in such a way that it does not need reference to the compiled model classes
 * to be deployed; in order to do that it makes a number of assumptions about the shape of the core model,
 * namely it expects that:
 * </p>
 * <ul>
 *  <li>SequenceFeatures have a <code>chromosomeLocation</code> reference, which can be used in range queries</li>
 *  <li>SequenceFeatures have an organism reference which has a <code>taxonId :: integer</code> field.</li>
 *  <li>SequenceFeatures have name, symbol, primaryIdentifer, and score fields.</li>
 * </ul>
 * @author Alex Kalderimis
 *
 */
public class Engine extends CommandRunner {

    private static final Logger LOG = Logger.getLogger(CommandRunner.class);

    private final Model model;
    private static final Map<Command, Map<String, Object>> STATS_CACHE =
            new CacheMap<Command, Map<String, Object>>("jbrowse.genomic.engine.STATS_CACHE");

    public Engine(InterMineAPI api) {
        super(api);
        this.model = api.getModel();
    }

    @Override
    public void reference(Command command) {
        Query q = getReferenceQuery(command);
        Segment seg = command.getSegment();
        Integer start = (seg.getStart() == null) ? 0 : seg.getStart();
        Integer end = seg.getEnd();
        Iterator<Object> it = getResults(q).iterator();

        while (it.hasNext()) {
            FastPathObject fpo = (FastPathObject) it.next();
            onData(makeReferenceFeature(fpo, start, end), it.hasNext());
        }
    }

    @Override
    public void features(Command command) {
        if (command.getSegment() != Segment.NEGATIVE_SEGMENT) {
            Query q = getFeatureQuery(command);
            Iterator<Object> it = getResults(q).iterator();

            while (it.hasNext()) {
                FastPathObject fpo = (FastPathObject) it.next();
                onData(makeFeatureWithSubFeatures(fpo), it.hasNext());
            }
        }
    }

    //------------ PRIVATE METHODS --------------------//

    private PathQuery getSFPathQuery(Command command) {
        return getSFPathQuery(command, command.getSegment());
    }


    private ConstraintSet constrainToOrganism(QueryClass features, QueryClass organisms, String taxonId) {
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(
            new QueryObjectReference(features, "organism"),
            ConstraintOp.CONTAINS, organisms));
        cs.addConstraint(new SimpleConstraint(
            new QueryField(organisms, "taxonId"),
            ConstraintOp.EQUALS,
            new QueryValue(Integer.valueOf(taxonId))));
        return cs;
    }

    // A Query that produces a single row: (featureDensity :: double, featureCount :: integer)
    @Override
    protected Query getStatsQuery(Command command) {

        String featureType = command.getType("SequenceFeature");
        ClassDescriptor seqf = model.getClassDescriptorByName("SequenceFeature");
        ClassDescriptor fcd = model.getClassDescriptorByName(featureType);
        // Check type conditions.
        if (fcd == null) {
            throw new RuntimeException(featureType + " is not in the model.");
        }
        if (fcd != seqf && !fcd.getAllSuperDescriptors().contains(seqf)) { 
            throw new RuntimeException(featureType + " is not a sequence feature");
        }

        QueryClass organisms = new QueryClass(model.getClassDescriptorByName("Organism").getType());

        // A query to get the feature-count for the current domain.
        Query countQ = pathQueryToOSQ(getSFPathQuery(command));
        QueryEvaluable count = new QueryFunction();
        countQ.clearSelect();
        countQ.addToSelect(count);

        // A query to get the size of the domain.
        Query subq_2 = new Query();
        QueryEvaluable length;
        Segment seg = command.getSegment();
        if (seg.getWidth() == null || seg == Segment.GLOBAL_SEGMENT) {
            QueryClass chromosomes = new QueryClass(model.getClassDescriptorByName("Chromosome").getType());

            subq_2.addFrom(chromosomes);
            subq_2.addFrom(organisms);

            length = new QueryFunction(new QueryField(chromosomes, "length"), QueryFunction.SUM);
            if (seg.getStart() != null) {
                subq_2.addToSelect(new QueryExpression(length, QueryExpression.SUBTRACT, new QueryValue(seg.getStart())));
            } else if (seg.getEnd() != null) {
                subq_2.addToSelect(new QueryExpression(new QueryValue(seg.getStart()), QueryExpression.SUBTRACT, length));
            } else {
                subq_2.addToSelect(length);
            }

            ConstraintSet cs = constrainToOrganism(chromosomes, organisms, command.getDomain());
            if (seg.getSection() != null) {
                cs.addConstraint(new SimpleConstraint(
                        new QueryField(chromosomes, "primaryIdentifier"),
                        ConstraintOp.EQUALS,
                        new QueryValue(seg.getSection())));
            }

            subq_2.setConstraint(cs);
        } else {
            length = new QueryValue(seg.getWidth());
            subq_2.addToSelect(length);
        }

        // A query that returns one row, with the density and feature count.
        Query q = new Query();
        q.addFrom(countQ);
        q.addFrom(subq_2);
        q.addToSelect(new QueryExpression(
            new QueryCast(new QueryField(countQ, count), Double.class),
            QueryExpression.DIVIDE,
            new QueryCast(new QueryField(subq_2, length), Double.class)));
        q.addToSelect(new QueryField(countQ, count));

        return q;
    }

    private PathConstraintRange makeRangeConstraint(String type, Segment seg) {
        return new PathConstraintRange(format("%s.chromosomeLocation", type),
                ConstraintOp.OVERLAPS, Collections.singleton(seg.toRangeString()));
    }

    // Run a query and get the list of objects it returns.
    private List<Object> getResults(Query q) {
        return getAPI().getObjectStore().executeSingleton(q);
    }

    private Map<String, Object> makeReferenceFeature(FastPathObject fpo, Integer start, Integer end) {
        CharSequence cs;
        try {
            cs = (CharSequence) fpo.getFieldValue("residues");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not fetch reference sequence.", e);
        }
        int featureLength = cs.length();
        Integer featEnd = (end == null) ? featureLength : Math.min(end, featureLength);
        CharSequence subSequence = cs.subSequence(start, featEnd);

        Map<String, Object> refFeature = new HashMap<String, Object>();
        refFeature.put("start", start);
        refFeature.put("end", featEnd);
        refFeature.put("seq", String.valueOf(subSequence));
        return refFeature;
    }

    private Map<String, Object> makeFeatureWithSubFeatures(FastPathObject fpo) {
        return makeFeature(fpo, true);
    }

    private Map<String, Object> makeFeature(FastPathObject fpo, boolean includeSubfeatures) {
        try {
            Map<String, Object> feature = new HashMap<String, Object>();
            try {
                feature.put("type", DynamicUtil.getSimpleClassName(fpo));
            } catch (Exception e) {
                feature.put("type", fpo.getClass().getSimpleName());
            }
            FastPathObject sot = null;
            try {
                sot = (FastPathObject) fpo.getFieldValue("sequenceOntologyTerm");
            } catch (IllegalAccessException e) {
                // Not all BioEntities have SO terms. ignore.
            }
            if (sot != null) feature.put("type", sot.getFieldValue("name"));

            String name, symbol, primId;

            try {
                name = (String) fpo.getFieldValue("name");
                symbol = (String) fpo.getFieldValue("symbol");
                primId = (String) fpo.getFieldValue("primaryIdentifier");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Expected a BioEntity, got a " + fpo.getClass().getName());
            }

            feature.put("name", (name != null) ? name : ((symbol != null) ? symbol : primId));
            feature.put("symbol", symbol);
            // uniqueID is not displayed to the user. Use primaryID where available - fall-back to object-id
            feature.put("uniqueID", (primId != null) ? primId : fpo.getFieldValue("id"));
            feature.put("score", fpo.getFieldValue("score"));
            try {
                feature.put("description", fpo.getFieldValue("description"));
            } catch (IllegalAccessException e) {
                // Ignore.
            }
            FastPathObject chrLoc = (FastPathObject) fpo.getFieldValue("chromosomeLocation");
            if (chrLoc != null) {
                // Convert Base -> Interbase Co-ords: start - 1
                feature.put("start",  ((Integer) chrLoc.getFieldValue("start")) - 1);
                feature.put("end",    chrLoc.getFieldValue("end"));
                feature.put("strand", chrLoc.getFieldValue("strand"));
            }
            if (includeSubfeatures) {
                List<Map<String, Object>> subFeatures = new ArrayList<Map<String, Object>>();
                Collection<FastPathObject> childFeatures = (Collection<FastPathObject>) fpo.getFieldValue("childFeatures");
                if (childFeatures != null) {
                    for (FastPathObject child: childFeatures) {
                        subFeatures.add(makeFeatureWithSubFeatures(child));
                    }
                }
                feature.put("subfeatures", subFeatures);
            }
            return feature;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error reading results", e);
        }
    }

    private Query getReferenceQuery(Command command) {
        PathQuery pq = new PathQuery(model);
        String type = command.getType("Chromosome");
        pq.addView(format("%s.sequence.id", type));
        pq.addConstraint(eq(format("%s.organism.taxonId", type), command.getDomain()));
        pq.addConstraint(eq(format("%s.primaryIdentifier", type), command.getSegment().getSection()));
        return pathQueryToOSQ(pq);
    }

    @Override
    protected PathQuery getFeaturePathQuery(Command command, Segment segment) {
        PathQuery pq = new PathQuery(model);
        String type = command.getType("SequenceFeature");
        pq.addView(String.format("%s.id", type));
        pq.addConstraint(eq(String.format("%s.organism.taxonId", type), command.getDomain()));
        if (segment != Segment.GLOBAL_SEGMENT)
            pq.addConstraint(makeRangeConstraint(type, segment));
        return pq;
    }

}
