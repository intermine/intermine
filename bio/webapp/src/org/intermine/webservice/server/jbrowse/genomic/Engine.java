package org.intermine.webservice.server.jbrowse.genomic;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import static java.lang.String.format;
import static org.intermine.pathquery.Constraints.eq;
import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;

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
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
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

/**
 * An adaptor for running JBrowse queries against a genomic database.
 *
 * <p>
 * This engine is written in such a way that it does not need reference to the compiled model
 * classes to be deployed; in order to do that it makes a number of assumptions about the shape of
 * the core model, namely it expects that:
 * </p>
 * <ul>
 *  <li>SequenceFeatures have a <code>chromosomeLocation</code> reference, which can be used in
 *  range queries</li>
 *  <li>SequenceFeatures have an organism reference which has a <code>taxonId :: integer</code>
 *  field.</li>
 *  <li>SequenceFeatures have name, symbol, primaryIdentifer, and score fields.</li>
 * </ul>
 * @author Alex Kalderimis
 *
 */
public class Engine extends CommandRunner
{

    private static final Logger LOG = Logger.getLogger(CommandRunner.class);

    private final Model model;
    private static final Map<Command, Map<String, Object>> STATS_CACHE =
            new CacheMap<Command, Map<String, Object>>("jbrowse.genomic.engine.STATS_CACHE");

    /**
     * constructor
     * @param api The API
     */
    public Engine(InterMineAPI api) {
        super(api);
        this.model = api.getModel();
    }

    @Override
    public void stats(Command command) {
        Map<String, Object> stats;
        Query q = getStatsQuery(command);
        // Stats can be expensive to calculate, so they are independently cached.
        synchronized (STATS_CACHE) {
            stats = STATS_CACHE.get(command);
            if (stats == null) {
                stats = new HashMap<String, Object>();
                try {
                    List<?> results = getAPI().getObjectStore().execute(q, 0, 1, false, false,
                            ObjectStore.SEQUENCE_IGNORE);
                    List<?> row = (List<?>) results.get(0);
                    stats.put("featureDensity", row.get(0));
                    stats.put("featureCount",   row.get(1));
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("Error getting statistics.", e);
                }
                LOG.debug("caching " + stats);
                STATS_CACHE.put(command, stats);
            }
        }
        sendMap(stats);
    }

    private void sendMap(Map<String, Object> map) {
        Iterator<Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> e = it.next();
            onData(e, it.hasNext());
        }
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

    private static List<Segment> sliceUp(int n, Segment segment) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be greater than 0");
        }
        if (segment == null || segment.getWidth() == null) {
            throw new IllegalArgumentException("segment must be non null with defined width");
        }
        List<Segment> subsegments = new ArrayList<Segment>();
        int sliceWidth = segment.getWidth() / n;
        int inital = Math.max(0, segment.getStart());
        int end = segment.getEnd();
        for (int i = inital; i < end; i += sliceWidth) {
            subsegments.add(segment.subsegment(i, Math.min(end, i + sliceWidth)));
        }
        return subsegments;
    }

    private static final class PathQueryCounter implements Callable<Integer>
    {
        final PathQuery pq;
        final ObjectStore os;

        PathQueryCounter(PathQuery pq, ObjectStore os) {
            this.pq = pq.clone();
            this.os = os;
        }

        @Override
        public Integer call() throws Exception {
            Query q = pathQueryToOSQ(pq);
            return os.count(q, ObjectStore.SEQUENCE_IGNORE);
        }
    }

    private static Map<MultiKey, Integer> maxima = new ConcurrentHashMap<MultiKey, Integer>();

    /**
     * @param command command to run
     */
    @Override
    public void densities(Command command) {
        final int nSlices = getNumberOfSlices(command);
        List<PathQuery> segmentQueries = getSliceQueries(command, nSlices);
        List<Future<Integer>> pending = countInParallel(segmentQueries);
        List<Integer> results = new ArrayList<Integer>();

        int max = 0, sum = 0;
        for (Future<Integer> future: pending) {
            try {
                Integer r = future.get();
                if (r != null && r > max) {
                    max = r;
                }
                sum += r;
                results.add(r);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        double mean = Double.valueOf(sum) / results.size();

        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Number> binStats = new HashMap<String, Number>();
        Integer currentMax = 0;
        if (command.getSegment() != Segment.NEGATIVE_SEGMENT) {
            Integer bpb = command.getSegment().getWidth() / nSlices;
            binStats.put("basesPerBin", bpb);
            MultiKey maxKey = new MultiKey(// Key by domain, type, ref-seq and band size
                    command.getDomain(),
                    command.getType("SequenceFeature"),
                    command.getSegment().getSection(),
                    bpb);
            currentMax = maxima.get(maxKey);
            if (currentMax == null || max > currentMax) {
                maxima.put(maxKey, Integer.valueOf(max));
            }
        }
        binStats.put("max", (currentMax != null && max < currentMax) ? currentMax : max);
        binStats.put("mean", mean);

        result.put("bins", results);
        result.put("stats", binStats);
        sendMap(result);
    }

    //------------ PRIVATE METHODS --------------------//

    private static int getNumberOfSlices(Command command) {
        int defaultNum = 10;
        String bpb = command.getParameter("basesPerBin");
        if (command == null
                || bpb == null
                || command.getSegment() == null
                || command.getSegment().getWidth() == null) {
            return defaultNum;
        }
        int width = command.getSegment().getWidth();
        int numBPB = Integer.valueOf(bpb);
        return width / numBPB;
    }

    private List<PathQuery> getSliceQueries(Command command, final int nSlices) {
        if (command.getSegment() == Segment.NEGATIVE_SEGMENT) {
            return Collections.emptyList();
        }
        List<Segment> slices = sliceUp(nSlices, command.getSegment());
        List<PathQuery> segmentQueries = new ArrayList<PathQuery>();
        for (Segment s: slices) {
            segmentQueries.add(getSFPathQuery(command, s));
        }
        return segmentQueries;
    }

    private List<Future<Integer>> countInParallel(List<PathQuery> segmentQueries) {
        if (segmentQueries.isEmpty()) {
            return Collections.emptyList();
        }
        ExecutorService executor = Executors.newFixedThreadPool(segmentQueries.size());
        List<Future<Integer>> pending = new ArrayList<Future<Integer>>();
        for (PathQuery pq: segmentQueries) {
            Callable<Integer> counter = new PathQueryCounter(pq, getAPI().getObjectStore());
            pending.add(executor.submit(counter));
        }
        executor.shutdown();
        return pending;
    }

    private PathQuery getSFPathQuery(Command command) {
        return getSFPathQuery(command, command.getSegment());
    }

    private PathQuery getSFPathQuery(Command command, Segment segment) {
        PathQuery pq = new PathQuery(model);
        String type = command.getType("SequenceFeature");
        pq.addView(String.format("%s.id", type));
        pq.addConstraint(eq(String.format("%s.organism.taxonId", type), command.getDomain()));
        if (segment != Segment.GLOBAL_SEGMENT) {
            pq.addConstraint(makeRangeConstraint(type, segment));
        }
        return pq;
    }

    private static ConstraintSet constrainToOrganism(QueryClass features, QueryClass organisms,
            String taxonId) {
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
    private Query getStatsQuery(Command command) {

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
        Query subqTwo = new Query();
        QueryEvaluable length;
        Segment seg = command.getSegment();
        if (seg.getWidth() == null || seg == Segment.GLOBAL_SEGMENT) {
            QueryClass chromosomes = new QueryClass(
                    model.getClassDescriptorByName("Chromosome").getType());

            subqTwo.addFrom(chromosomes);
            subqTwo.addFrom(organisms);

            length = new QueryFunction(new QueryField(chromosomes, "length"), QueryFunction.SUM);
            if (seg.getStart() != null) {
                subqTwo.addToSelect(new QueryExpression(length, QueryExpression.SUBTRACT,
                        new QueryValue(seg.getStart())));
            } else if (seg.getEnd() != null) {
                subqTwo.addToSelect(new QueryExpression(new QueryValue(seg.getStart()),
                        QueryExpression.SUBTRACT, length));
            } else {
                subqTwo.addToSelect(length);
            }

            ConstraintSet cs = constrainToOrganism(chromosomes, organisms, command.getDomain());
            if (seg.getSection() != null) {
                cs.addConstraint(new SimpleConstraint(
                        new QueryField(chromosomes, "primaryIdentifier"),
                        ConstraintOp.EQUALS,
                        new QueryValue(seg.getSection())));
            }

            subqTwo.setConstraint(cs);
        } else {
            length = new QueryValue(seg.getWidth());
            subqTwo.addToSelect(length);
        }

        // A query that returns one row, with the density and feature count.
        Query q = new Query();
        q.addFrom(countQ);
        q.addFrom(subqTwo);
        q.addToSelect(new QueryExpression(
            new QueryCast(new QueryField(countQ, count), Double.class),
            QueryExpression.DIVIDE,
            new QueryCast(new QueryField(subqTwo, length), Double.class)));
        q.addToSelect(new QueryField(countQ, count));

        return q;
    }

    private static PathConstraintRange makeRangeConstraint(String type, Segment seg) {
        return new PathConstraintRange(String.format("%s.chromosomeLocation", type),
                ConstraintOp.OVERLAPS, Collections.singleton(seg.toRangeString()));
    }

    // Run a query and get the list of objects it returns.
    private List<Object> getResults(Query q) {
        return getAPI().getObjectStore().executeSingleton(q);
    }

    private static Map<String, Object> makeReferenceFeature(FastPathObject fpo, Integer start,
            Integer end) {
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

            String name, symbol, primId;

            try {
                name = (String) fpo.getFieldValue("name");
                symbol = (String) fpo.getFieldValue("symbol");
                primId = (String) fpo.getFieldValue("primaryIdentifier");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Expected a BioEntity, got a "
                        + fpo.getClass().getName());
            }

            feature.put("name", (name != null) ? name : ((symbol != null) ? symbol : primId));
            feature.put("symbol", symbol);
            // uniqueID is not displayed to the user.
            // Use primaryID where available - fall-back to object-id
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
            /* This section has been changed to
             *  - avoid a loop caused by exon resulting as parents of mRNA.
             *    similar issue for transposon fragment
             *    TODO: check PopulateChildFeatures.
             *  - exclude Exon and CDS as children of Gene
             */
            if (includeSubfeatures) {
                List<Map<String, Object>> subFeatures = new ArrayList<Map<String, Object>>();
                @SuppressWarnings("unchecked")
                Collection<FastPathObject> childFeatures = (Collection<FastPathObject>)
                        fpo.getFieldValue("childFeatures");
                // there are exons parents of mRNA -> loop
                if (childFeatures != null
                        && !feature.get("type").toString().contains("Exon")
                        && !feature.get("type").toString().contains("TransposonFragment")) {
                    for (FastPathObject child: childFeatures) {
                        LOG.debug("CF " + feature.get("type") + " p of -> "
                                + child.getClass().getSimpleName());
                        // don't consider introns
                        if (!child.getClass().getSimpleName().startsWith("Intron")) {
                            // and don't consider exons or CDS as subfeatures of gene
                            // not(A and B) and not(A and C) = (notA or notB) and (notA or notC) =
                            // notA or (notB and notC)
                            if (!feature.get("type").toString().endsWith(".Gene")
                                    || (!child.getClass().getSimpleName().startsWith("Exon")
                                    && !child.getClass().getSimpleName().startsWith("CDS"))) {
                                subFeatures.add(makeFeatureWithSubFeatures(child));
                            }
                        }
                    }
                }
                String soType = getSOType(feature);
                feature.put("type", soType);
                feature.put("subfeatures", subFeatures);
            }
            return feature;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error reading results", e);
        }
    }

    /**
     * @param feature
     * @return the feature.type, SO style
     */
    private String getSOType(Map<String, Object> feature) {
        String path = "org.intermine.model.bio.";
        String camelName = feature.get("type").toString().replace(path, "");

        if (camelName.contentEquals("MiRNA")) {
            return "miRNA";
        }
        if (camelName.contentEquals("SnRNA")) {
            return "snRNA";
        }
        if (camelName.contentEquals("SnoRNA")) {
            return "snoRNA";
        }
        if (camelName.contentEquals("NcRNA")) {
            return "ncRNA";
        }
        if (camelName.contentEquals("LncRNA")) {
            return "lncRNA";
        }
        if (camelName.contentEquals("AntisenseLncRNA")) {
            return "antisense_lncRNA";
        }
        if (camelName.contentEquals("MiRNAPrimaryTranscript")) {
            return "miRNA_primary_transcript";
        }

        StringBuffer so = new StringBuffer();
        int i = 0;
        for (String w : camelName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
            if (i > 0) {
                so.append("_");
            }
            so.append(w);
            i++;
        }
        
        if (so.toString().contentEquals("CDS")) {
            return so.toString();
        }

        String sosmall = so.toString().toLowerCase();
        if (sosmall.contains("rna")) {
            return sosmall.replace("rna", "RNA");
        }
        if (sosmall.contains("orf")) {
            return sosmall.replace("orf", "ORF");
        }
        if (sosmall.contains("utr")) {
            return sosmall.replace("utr", "UTR");
        }
        // default
        return sosmall;
    }

    private Query getReferenceQuery(Command command) {
        PathQuery pq = new PathQuery(model);
        String type = command.getType("Chromosome");
        pq.addView(format("%s.sequence.id", type));
        pq.addConstraint(eq(format("%s.organism.taxonId", type), command.getDomain()));
        pq.addConstraint(eq(format("%s.primaryIdentifier", type),
                command.getSegment().getSection()));
        return pathQueryToOSQ(pq);
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
