package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Helper class for widgets.  It's where the math is done for enrichment widgets
 * @author julie
  */
public final class WidgetUtil
{
    private WidgetUtil() {
        // don't
    }

    private static Map<String, List> statsCalcCache = new HashMap<String, List>();

    /**
     * Runs both queries and compares the results.
     * @param os the object store
     * @param ldr the loader
     * @param bag the bag we are analysing
     * @param maxValue maximum value to return - for display purposes only
     * @param errorCorrection which error correction algorithm to use, Bonferroni
     * or Benjamini Hochberg or none
     * @return array of three results maps
     */
    public static ArrayList statsCalc(ObjectStore os,
                                      EnrichmentWidgetLdr ldr,
                                      InterMineBag bag,
                                      Double maxValue,
                                      String errorCorrection) {

        ArrayList<Map> maps = new ArrayList<Map>();

        int populationTotal = calcTotal(os, ldr, true); // objects annotated in database
        int sampleTotal = calcTotal(os, ldr, false);    // objects annotated in bag
        int testCount = 0; // number of tests

        // sample query
        Query q = ldr.getSampleQuery(false);

        Results r = null;

        HashMap<String, Long> countMap = new HashMap();
        HashMap<String, String> idMap = new HashMap();
        HashMap<String, BigDecimal> resultsMap = new HashMap();
        Map dummy = new HashMap();
        Map<String, BigDecimal> sortedMap = new LinkedHashMap<String, BigDecimal>();

        // if the model has changed, the query might not be valid
        if (q != null && populationTotal > 0) {
            r = os.execute(q, 20000, true, true, true);

            Iterator iter = r.iterator();

            while (iter.hasNext()) {

                // extract results
                ResultsRow rr =  (ResultsRow) iter.next();

                // id of annotation item (eg. GO term)
                String id = (String) rr.get(0);

                // count of item
                Long count = (Long) rr.get(1);

                // id & count
                countMap.put(id, count);

                // id & label
                idMap.put(id, (String) rr.get(2));

            }

            // run population query
            List rAll = statsCalcCache.get(ldr.getPopulationQuery(false).toString());
            if (rAll == null) {
                rAll = os.execute(ldr.getPopulationQuery(false), 20000, true, true, true);
                rAll = new ArrayList(rAll);
                statsCalcCache.put(ldr.getPopulationQuery(false).toString(), rAll);
            }

            Iterator itAll = rAll.iterator();

            HypergeometricDistributionImpl h = new HypergeometricDistributionImpl(populationTotal,
                    sampleTotal, sampleTotal);

            // loop through results again to calculate p-values
            while (itAll.hasNext()) {

                ResultsRow rrAll =  (ResultsRow) itAll.next();

                String id = (String) rrAll.get(0);
                testCount++;

                if (countMap.containsKey(id)) {

                    Long countBag = countMap.get(id);
                    Long countAll = (java.lang.Long) rrAll.get(1);

                    h.setNumberOfSuccesses(countAll.intValue());
                    double p = h.upperCumulativeProbability(countBag.intValue());

                    try {
                        resultsMap.put(id, new BigDecimal(p));
                    } catch (Exception e) {
                        String msg = p + " isn't a double.  calculated for " + id + " using "
                            + " k: "  + countBag + ", n: " + sampleTotal + ", M: " + countAll
                            + ", N: " + populationTotal + ".  k query: "
                            + ldr.getSampleQuery(false).toString() + ".  n query: "
                            + ldr.getSampleQuery(true).toString() + ".  M query: "
                            + ldr.getPopulationQuery(false).toString() + ".  N query: "
                            + ldr.getPopulationQuery(true).toString();

                        throw new RuntimeException(msg, e);
                    }
                }
            }

            if (resultsMap.isEmpty()) {
                // no results
                dummy.put("widgetTotal", new Integer(0));
            } else {
                sortedMap = ErrorCorrection.adjustPValues(errorCorrection, resultsMap,
                        maxValue, testCount);
                dummy.put("widgetTotal", new Integer(sampleTotal));
            }
        } else {
            // no results
            dummy.put("widgetTotal", new Integer(0));
        }

        maps.add(0, sortedMap);
        maps.add(1, countMap);
        maps.add(2, idMap);
        maps.add(3, dummy);

        return maps;
    }

    private static int calcTotal(ObjectStore os, EnrichmentWidgetLdr ldr, boolean calcTotal) {
        Query q = new Query();
        if (calcTotal) {
            q = ldr.getPopulationQuery(true);
        } else {
            q = ldr.getSampleQuery(true);
        }
        if (q == null) {
            // bad query, model probably changed.  no results
            return 0;
        }
        Object[] o = os.executeSingleton(q).toArray();
        if (o.length == 0) {
            // no results
            return  0;
        }
        return  ((java.lang.Long) o[0]).intValue();
    }
}
