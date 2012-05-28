package org.intermine.webservice.server.query.result;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HistogrammingTest
{
    private static ObjectStoreWriter osw;
    private static Random generator = new Random(System.currentTimeMillis());
    private static final Logger LOG = Logger.getLogger(HistogrammingTest.class);
    private static Map<Integer, Long> expected = new TreeMap<Integer, Long>();
    private static final int MAX_SCALE =  5000;
    private static final int MIN_BIN   =   250;

    private static int made = 0;

    @BeforeClass
    public static void loadData() {
       made = 0;
       try {
           osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
           Model m = osw.getModel();
           osw.beginTransaction();
           int scale = MAX_SCALE;
           try {
               for (int j = 20; j <= 90; j += 10) {
                   int count = generator.nextInt(scale) + MIN_BIN;//10 * (j % 100);
                   expected.put(j, Long.valueOf(count));
                   for (int i = 0; i < count; i++) {
                       Types thing = DynamicUtil.createObject(Types.class);
                       thing.setIntType(generator.nextInt(10) + j);
                       thing.setDoubleType((generator.nextGaussian() * 5d) + 30d);
                       thing.setName("histothing" + String.format("%06d", made++));
                       osw.store(thing);
                   }
               }
               osw.commitTransaction();
           } catch (ObjectStoreException e) {
               osw.abortTransaction();
               throw e;
           }
       } catch (ObjectStoreException e) {
           LOG.error(e);
       }
       showHistogram("Expected", expected);
       System.out.printf("Made %d things\n", made);
    }

    private static void showHistogram(String label, Map<Integer, Long> data) {
        System.out.println();
        System.out.printf("%s\n===============\n", label);
        Long max = Collections.max(data.values());
        double[] heights = new double[data.size()];
        String[] groupStarts = new String[data.size()];
        int idx = 0;
        for (Entry<Integer, Long> pair: data.entrySet()) {
            heights[idx]     = Double.valueOf(pair.getValue()) / Double.valueOf(max) * 10;
            groupStarts[idx] = String.format("%2d ", pair.getKey());
            idx++;
        }
        for (int row = 10; row >= 0; row--) {
            System.out.printf("%5d", max / 10 * row);
            for (int col = 0; col < heights.length; col++) {
                if (heights[col] >= row) {
                    System.out.print(" x ");
                } else {
                    System.out.print("   ");
                }
            }
            System.out.println();
        }
        System.out.print("     ");
        for (int col = 0; col < groupStarts.length; col++) {
            System.out.print(groupStarts[col]);
        }

        System.out.println();
    }
    
    @AfterClass
    public static void shutdown() {
        int deleted = 0;
        if (osw != null) {
            try {
                osw.beginTransaction();
                PathQuery pq = new PathQuery(osw.getModel());
                pq.addView("Types.id");
                pq.addConstraint(Constraints.eq("Types.name", "histo*"));

                Query q = MainHelper.makeQuery(
                        pq, new HashMap(), new HashMap(), null, new HashMap());

                Results res = osw.execute(q, 50000, true, false, true);
                for (Object row: res) {
                    Types thing = (Types) ((List) row).get(0);
                    osw.delete(thing);
                    deleted++;
                }
                
                osw.commitTransaction();
            } catch (ObjectStoreException e) {
                LOG.warn(e);
            }
            try {
                osw.close();
            } catch (ObjectStoreException e) {
                LOG.warn(e);
            }
        }
        System.out.printf("\n[CLEAN UP] Deleted %d things\n", deleted);
    }
    
    @Test
    public void testDoubles() throws Exception {
        Model m = osw.getModel();

        Long start = System.currentTimeMillis();

        PathQuery pq = new PathQuery(m);

        pq.addViews("Types.name", "Types.intType", "Types.doubleType");
        pq.addConstraint(Constraints.eq("Types.name", "histo*"));
       
        Query q = MainHelper.makeSummaryQuery(pq, "Types.doubleType",
                new HashMap(), new HashMap(), null);

        Results res = osw.execute(q, 100000, true, false, false);

        Long sum = 0L;
        Map<Integer, Long> actual = new TreeMap<Integer, Long>();

        for (Object o: res) {
            //System.out.println("ROW:" + o);
            List row = (List) o;
            Integer bucket  = (Integer) row.get(5);
            Long count = ((BigDecimal) row.get(6)).longValue();
            actual.put(bucket, count);
            sum += count;
        }
        Long postExecution = System.currentTimeMillis();
        showHistogram("Actual", actual);
        System.out.printf(
                "MIN:        %.03f\nMAX:        %.03f\nAVG:        %.03f\nSTD-DEV:    %.03f\n",
                ((List) res.get(0)).subList(0, 4).toArray());
        System.out.println("TOTAL THINGS: " + sum);
        System.out.printf("Query composition and execution took %.4f seconds", 
                Double.valueOf(postExecution - start) / 1000);

        res = osw.execute(getCheckQuery());
        Long countFromOSQ = null;
        for (Object o: res) {
            List row = (List) o;
            countFromOSQ = (Long) row.get(0);
        }
        //int scale = generator.nextInt(MAX_SCALE - MIN_SCALE) + MIN_SCALE;

        assertEquals("Sum of buckets and total count agrees",
                sum, countFromOSQ);
        assertEquals("Sum of buckets agrees with what we inserted",
                made, sum.intValue());
    }
    
    @Test
    public void test() throws Exception {
        Model m = osw.getModel();

        Long start = System.currentTimeMillis();

        PathQuery pq = new PathQuery(m);

        pq.addViews("Types.name", "Types.intType", "Types.doubleType");
        pq.addConstraint(Constraints.eq("Types.name", "histo*"));
       
        Query q = MainHelper.makeSummaryQuery(pq, "Types.intType",
                new HashMap(), new HashMap(), null);

        Results res = osw.execute(q, 100000, true, false, false);

        Long sum = 0L;
        Map<Integer, Long> actual = new TreeMap<Integer, Long>();

        for (Object o: res) {
            //System.out.println("ROW:" + o);
            List row = (List) o;
            Integer bucket  = (Integer) row.get(5);
            Integer min     = (Integer) row.get(0);
            Integer max     = (Integer) row.get(1);
            Integer buckets = (Integer) row.get(4);
            Integer width   = (max - min) / (buckets - 1);
            Integer group   = min + ((bucket - 1) * width);
            Long count = ((BigDecimal) row.get(6)).longValue();
            actual.put(group, count);
            sum += count;
        }
        Long postExecution = System.currentTimeMillis();
        showHistogram("Actual", actual);
        System.out.printf(
                "MIN:        %d\nMAX:        %d\nAVG:        %.03f\nSTD-DEV:    %.03f\n",
                ((List) res.get(0)).subList(0, 4).toArray());
        System.out.println("TOTAL THINGS: " + sum);
        System.out.printf("Query composition and execution took %.4f seconds", 
                Double.valueOf(postExecution - start) / 1000);

        res = osw.execute(getCheckQuery());
        Long countFromOSQ = null;
        for (Object o: res) {
            List row = (List) o;
            countFromOSQ = (Long) row.get(0);
        }
        //int scale = generator.nextInt(MAX_SCALE - MIN_SCALE) + MIN_SCALE;

        assertEquals("Sum of buckets and total count agrees",
                sum, countFromOSQ);
        assertEquals("Sum of buckets agrees with what we inserted",
                made, sum.intValue());
    }
    
    private Query getCheckQuery() {
        Query cq = new Query();
        QueryClass qc = new QueryClass(Types.class);
        cq.addFrom(qc);
        cq.addToSelect(new QueryFunction());
        cq.setConstraint(new SimpleConstraint(new  QueryField(qc, "name"),
                ConstraintOp.MATCHES, new QueryValue("histo%")));
        return cq;
    }

}
