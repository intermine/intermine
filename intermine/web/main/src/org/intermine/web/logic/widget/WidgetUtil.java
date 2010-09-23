package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.SortableMap;

/**
 * Helper class for widgets.  It's where the math is done for enrichment widgets
 * @author julie
  */
public final class WidgetUtil
{
    private WidgetUtil() {
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

        // sample query
        Query q = ldr.getSampleQuery(false);

        Results r = null;

        HashMap<String, Long> countMap = new HashMap();
        HashMap<String, String> idMap = new HashMap();
        HashMap<String, BigDecimal> resultsMap = new HashMap();
        Map dummy = new HashMap();
        SortableMap sortedMap = new SortableMap();

        // if the model has changed, the query might not be valid
        if (q != null) {
            r = os.execute(q, 20000, true, true, true);

            Iterator iter = r.iterator();

            while (iter.hasNext()) {

                // extract results
                ResultsRow rr =  (ResultsRow) iter.next();

                // id of item
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

            // loop through results again to calculate p-values
            while (itAll.hasNext()) {

                ResultsRow rrAll =  (ResultsRow) itAll.next();

                String id = (String) rrAll.get(0);

                if (countMap.containsKey(id)) {

                    Long countBag = countMap.get(id);
                    Long countAll = (java.lang.Long) rrAll.get(1);

                    // (k,n,M,N)
                    double p = Hypergeometric.calculateP(countBag.intValue(), sampleTotal,
                                                         countAll.intValue(), populationTotal);

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

            Map<String, BigDecimal> adjustedResultsMap = new HashMap<String, BigDecimal>();

            if (resultsMap.isEmpty()) {
                // no results
                dummy.put("widgetTotal", new Integer(0));
            } else {
                if (!"None".equals(errorCorrection)) {
                    adjustedResultsMap = calcErrorCorrection(errorCorrection, maxValue, resultsMap);
                } else {
                    // TODO move this to the ErrorCorrection class
                    BigDecimal max = new BigDecimal(maxValue.doubleValue());
                    for (String id : resultsMap.keySet()) {
                        BigDecimal pvalue = resultsMap.get(id);
                        if (pvalue.compareTo(max) <= 0) {
                            adjustedResultsMap.put(id, pvalue);
                        }
                    }
                }
                sortedMap = new SortableMap(adjustedResultsMap);
                sortedMap.sortValues();
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

    /**
     * See online help docs for detailed description of what error correction is and why we need it.
     * Briefly, in all experiments certain things happen that look interesting but really just
     * happened by chance.  We need to account for this phenomenon to ensure our numbers are
     * interesting behaviour and not just random happenstance.
     *
     * To do this we take all of our p-values and adjust them.  Here we are using on of our two
     * methods available - which one we use is determined by the user.
     * @param errorCorrection which multiple hypothesis test correction to use - Bonferroni or
     * BenjaminiHochberg
     * @param maxValue maximum value we're interested in - used for display purposes only
     * @param resultsMap map containing unadjusted p-values
     * @return map of all the adjusted p-values
     */
    public static Map<String, BigDecimal> calcErrorCorrection(String errorCorrection,
                                                 Double maxValue,
                                                 HashMap<String, BigDecimal> resultsMap) {
        ErrorCorrection e = null;
        if ("Bonferroni".equals(errorCorrection)) {
            e = new Bonferroni(resultsMap);
        } else {
            e = new BenjaminiHochberg(resultsMap);
        }
        e.calculate(maxValue);
        return e.getAdjustedMap();
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
