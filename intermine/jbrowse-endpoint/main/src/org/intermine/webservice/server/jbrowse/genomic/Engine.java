package org.intermine.webservice.server.jbrowse.genomic;

import static java.lang.String.format;
import static org.intermine.pathquery.Constraints.eq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.query.MainHelper;
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
import org.intermine.webservice.server.jbrowse.Command;
import org.intermine.webservice.server.jbrowse.CommandRunner;
import org.intermine.webservice.server.jbrowse.Segment;

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

    private final Model model;
    private static final Map<Command, Map<String, Object>> STATS_CACHE =
            new CacheMap("jbrowse.genomic.engine.STATS_CACHE");

    public Engine(InterMineAPI api) {
        super(api);
        this.model = api.getModel();
    }

    @Override
    public Map<String, Object> stats(Command command) {
        Query q = getStatsQuery(command);
        Map<String, Object> stats;
        // Stats can be expensive to calculate, so they are independently cached.
        synchronized(STATS_CACHE) {
            stats = STATS_CACHE.get(command);
            if (stats == null) {
                stats = new HashMap<String, Object>();
                try {
                    List<?> results = getAPI().getObjectStore().execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
                    List<?> row = (List<?>) results.get(0);
                    stats.put("featureDensity", row.get(0));
                    stats.put("featureCount",   row.get(1));
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("Error getting statistics.", e);
                }
                STATS_CACHE.put(command, stats);
            }
        }
        return new HashMap<String, Object>(stats);
    }

    @Override
    public Map<String, Object> reference(Command command) {
        Query q = getReferenceQuery(command);
        Segment seg = command.getSegment();
        Integer start = (seg.getStart() == null) ? 0 : seg.getStart();
        Integer end = seg.getEnd();

        List<Map<String, Object>> features = new ArrayList<Map<String, Object>>();
        for (Object o: getResults(q)) {
            FastPathObject fpo = (FastPathObject) o;
            features.add(makeReferenceFeature(fpo, start, end));
        }
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("features", features);
        return result;
    }

    @Override
    public Map<String, Object> features(Command command) {
        Query q = getFeatureQuery(command);
        List<Map<String, Object>> features = new ArrayList<Map<String, Object>>();
        for (Object o: getResults(q)) {
            FastPathObject fpo = (FastPathObject) o;
            features.add(makeFeatureWithSubFeatures(fpo));
        }
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("features", features);
        return result;
    }

    //------------ PRIVATE METHODS --------------------//

    // A Query that produces a single row: (featureDensity :: double, featureCount :: integer)
    private Query getStatsQuery(Command command) {

        // A query to get the feature-count for the current domain.
        PathQuery pq = new PathQuery(model);
        String type = command.getType("SequenceFeature");
        pq.addView(String.format("%s.id", type));
        pq.addConstraint(eq(String.format("%s.organism.taxonId", type), command.getDomain()));
        if (command.getSegment() != Segment.GLOBAL_SEGMENT)
            pq.addConstraint(makeRangeConstraint(type, command.getSegment()));
        Query subq_1 = pathQueryToOSQ(pq);
        Query countQ = new Query();
        countQ.addFrom(subq_1);
        QueryEvaluable count = new QueryFunction();
        countQ.addToSelect(count);

        // A query to get the size of the domain.
        Query subq_2 = new Query();
        QueryEvaluable length;
        if (command.getSegment() == Segment.GLOBAL_SEGMENT) {
            QueryClass chromosomes = new QueryClass(model.getClassDescriptorByName("Chromosome").getType());
            QueryClass organisms   = new QueryClass(model.getClassDescriptorByName("Organism").getType());
            subq_2.addFrom(chromosomes);
            subq_2.addFrom(organisms);
            length = new QueryFunction(new QueryField(chromosomes, "length"), QueryFunction.SUM);
            subq_2.addToSelect(length);
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            cs.addConstraint(new ContainsConstraint(
                new QueryObjectReference(chromosomes, "organism"),
                ConstraintOp.CONTAINS, organisms));
            cs.addConstraint(new SimpleConstraint(
                new QueryField(organisms, "taxonId"),
                ConstraintOp.EQUALS,
                new QueryValue(Integer.valueOf(command.getDomain()))));
            subq_2.setConstraint(cs);
        } else {
            Segment seg = command.getSegment();
            length = new QueryValue(seg.getEnd() - seg.getStart());
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
        return new PathConstraintRange(String.format("%s.chromosomeLocation", type),
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
        Integer featEnd = (end == null) ? cs.length() : Math.min(end, cs.length());
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

    private Map<String, Object> makeFeatureWithoutSubFeatures(FastPathObject fpo) {
        return makeFeature(fpo, false);
    }

    private Map<String, Object> makeFeature(FastPathObject fpo, boolean includeSubfeatures) {
        try {
            Map<String, Object> feature = new HashMap<String, Object>();
            FastPathObject sot = (FastPathObject) fpo.getFieldValue("sequenceOntologyTerm");
            if (sot != null) feature.put("type", sot.getFieldValue("name"));
            String name   = (String) fpo.getFieldValue("name");
            String symbol = (String) fpo.getFieldValue("symbol");
            String primId = (String) fpo.getFieldValue("primaryIdentifier");
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
                feature.put("start",  chrLoc.getFieldValue("start"));
                feature.put("end",    chrLoc.getFieldValue("end"));
                feature.put("strand", chrLoc.getFieldValue("strand"));
            }
            if (includeSubfeatures) {
                List<Map<String, Object>> subFeatures = new ArrayList<Map<String, Object>>();
                Collection<?> locFs = (Collection<?>) fpo.getFieldValue("locatedFeatures");
                if (locFs != null) {
                    for (Object o: locFs) {
                        subFeatures.add(makeFeatureWithoutSubFeatures((FastPathObject) o));
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

    private Query pathQueryToOSQ(PathQuery pq) {
        Query q;
        try {
            q = MainHelper.makeQuery(pq, new HashMap(), new HashMap(), null, new HashMap());
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Error generating query.", e);
        }
        return q;
    }

    private Query getFeatureQuery(Command command) {
        PathQuery pq = new PathQuery(model);
        String type = command.getType("SequenceFeature");
        pq.addView(format("%s.id", type));
        pq.addConstraint(Constraints.eq(format("%s.organism.taxonId", type), command.getDomain()));
        pq.addConstraint(makeRangeConstraint(type, command.getSegment()));
        return pathQueryToOSQ(pq);
    }

}
