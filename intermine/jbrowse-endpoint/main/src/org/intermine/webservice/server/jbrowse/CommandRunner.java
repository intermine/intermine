package org.intermine.webservice.server.jbrowse;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.HashSet;
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
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CacheMap;

/**
 *
 * @author Alex
 *
 */
public abstract class CommandRunner
{

    private InterMineAPI api;

    private Set<MapListener<String, Object>> listeners = new HashSet<MapListener<String, Object>>();

    /**
     * @param api InterMine API
     */
    public CommandRunner(InterMineAPI api) {
        this.api = api;
    }

    /**
     * @return api InterMine API
     */
    protected InterMineAPI getAPI() {
        return api;
    }

    /**
     * @param className class name
     * @param im InterMine API
     * @return command runner
     */
    public static CommandRunner getRunner(String className, InterMineAPI im) {
        CommandRunner runner;
        try {
            @SuppressWarnings("unchecked")
            Class<CommandRunner> runnerCls = (Class<CommandRunner>) Class.forName(className);
            Constructor<CommandRunner> ctr = runnerCls.getConstructor(InterMineAPI.class);
            runner = ctr.newInstance(im);
        } catch (ClassCastException e) {
            throw new RuntimeException("Configuration is incorrect.", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find " + className);
        } catch (SecurityException e) {
            throw new RuntimeException("Not allowed to access " + className);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot access constructor for " + className);
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate runner", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access runner", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot invoke constructor of " + className, e);
        }
        return runner;
    }

    private static final Map<Command, Map<String, Object>> STATS_CACHE =
            new CacheMap<Command, Map<String, Object>>("jbrowse.commandrunner.STATS_CACHE");

    /**
     * Calculate the statistics for the given command.
     * @param command The command to produce statistics for.
     */
    public void stats(Command command) {
        Map<String, Object> stats;
        Query q = getStatsQuery(command);
        // Stats can be expensive to calculate, so they are independently cached.
        synchronized (STATS_CACHE) {
            stats = STATS_CACHE.get(command);
            if (stats == null) {
                stats = new HashMap<String, Object>();
                try {
                    List<?> results = getAPI().getObjectStore()
                                      .execute(q, 0, 1, false, false, ObjectStore.SEQUENCE_IGNORE);
                    List<?> row = (List<?>) results.get(0);
                    stats.put("featureDensity", row.get(0));
                    stats.put("featureCount",   row.get(1));
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("Error getting statistics.", e);
                }
                STATS_CACHE.put(command, stats);
            }
        }
        sendMap(stats);
    }

    private static Map<MultiKey, Integer> maxima = new ConcurrentHashMap<MultiKey, Integer>();

    /**
     * Produce densities information for the given command.
     * @param command The command to produce density information for.
     */
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
            // Key by domain, type, ref-seq and band size
            MultiKey maxKey = new MultiKey(
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

    /** Get a list of queries to count the features in a slice of the segment. **/
    private List<PathQuery> getSliceQueries(Command command, int nSlices) {
        if (command.getSegment() == Segment.NEGATIVE_SEGMENT) {
            return Collections.emptyList();
        }
        List<Segment> slices = sliceUp(nSlices, command.getSegment());
        List<PathQuery> segmentQueries = new ArrayList<PathQuery>();
        for (Segment s: slices) {
            segmentQueries.add(getFeaturePathQuery(command, s));
        }
        return segmentQueries;
    }

    /**
     * Get a query for the features for a given command.
     * @param command The command to get features for.
     * @return A query for features.
     */
    protected Query getFeatureQuery(Command command) {
        return pathQueryToOSQ(getFeaturePathQuery(command, command.getSegment()));
    }

    /**
     * Produce a path query for features.
     * @param command The command
     * @param s The segment
     * @return A path query.
     */
    protected abstract PathQuery getFeaturePathQuery(Command command, Segment s);

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

    private int getNumberOfSlices(Command command) {
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

    /**
     * A Query that produces a single row: (featureDensity :: double, featureCount :: integer)
     * @param command The command to produce statistics for.
     * @return A query for the count and density.
     */
    protected abstract Query getStatsQuery(Command command);

    /**
     * Produce out out as defined in a map.
     * @param map The map to send as JSON.
     */
    protected void sendMap(Map<String, Object> map) {
        Iterator<Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Object> e = it.next();
            onData(e, it.hasNext());
        }
    }

    /**
     * @param command command
     * @return action
     */
    public String getIntro(Command command) {
        switch (command.getAction()) {
            case STATS:
            case DENSITIES:
                return null;
            case REFERENCE:
            case FEATURES:
                return "\"features\":[";
            default:
                throw new IllegalArgumentException("Unknown action: " + command.getAction());
        }
    }

    /**
     * @param command command
     * @return action
     */
    public String getOutro(Command command) {
        switch (command.getAction()) {
            case STATS:
            case DENSITIES:
                return null;
            case REFERENCE:
            case FEATURES:
                return "]";
            default:
                throw new IllegalArgumentException("Unknown action: " + command.getAction());
        }
    }

    /**
     * @param command command
     */
    public void run(Command command) {
        switch (command.getAction()) {
            case STATS:
                stats(command);
                break;
            case REFERENCE:
                reference(command);
                break;
            case FEATURES:
                features(command);
                break;
            case DENSITIES:
                densities(command);
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + command.getAction());
        }
    }

    /**
     * @param command command
     */
    public abstract void reference(Command command);

    /**
     * @param command command
     */
    public abstract void features(Command command);

    /**
     * @param datum data
     * @param hasMore true if has more
     */
    protected void onData(Map<String, Object> datum, boolean hasMore) {
        for (MapListener<String, Object> listener: listeners) {
            listener.add(datum, hasMore);
        }
    }

    /**
     * @param datum data
     * @param hasMore true if has more
     */
    protected void onData(Entry<String, Object> datum, boolean hasMore) {
        for (MapListener<String, Object> listener: listeners) {
            listener.add(datum, hasMore);
        }
    }

    /**
     * @param listener listener
     */
    public void addListener(MapListener<String, Object> listener) {
        listeners.add(listener);
    }
}
