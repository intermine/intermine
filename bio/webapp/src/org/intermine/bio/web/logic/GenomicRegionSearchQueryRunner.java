package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.bio.web.model.ChromosomeInfo;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.bio.web.model.GenomicRegionSearchConstraint;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SOTerm;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;


/**
 * This class has all database query logics for genomic region search.
 *
 *private static final Logger LOG = Logger.getLogger(GenomicRegionSearchService.class);
 *@author Fengyuan Hu
 */
public class GenomicRegionSearchQueryRunner implements Runnable
{
    private HttpServletRequest request = null;
    private String spanUUIDString = null;
    private GenomicRegionSearchConstraint grsc = null;
    private Map<GenomicRegion, Query> queryMap = null;

    private static Map<String, Map<String, ChromosomeInfo>> chrInfoMap = null;

    private static final Logger LOG = Logger.getLogger(GenomicRegionSearchQueryRunner.class);

    /**
     * Constructor
     *
     * @param request HttpServletRequest
     * @param spanUUIDString UUID
     * @param grsc GenomicRegionSearchConstraint
     * @param queryMap map of span and its query
     */
    public GenomicRegionSearchQueryRunner(HttpServletRequest request, String spanUUIDString,
            GenomicRegionSearchConstraint grsc, Map<GenomicRegion, Query> queryMap) {

        this.request = request;
        this.spanUUIDString = spanUUIDString;
        this.grsc = grsc;
        this.queryMap = queryMap;
    }

    /**
     * Main body of db search
     */
    public void search() {

        // Use spanConstraintMap to check whether the spanUpload is duplicated, the map is saved in
        // the session
        @SuppressWarnings("unchecked")
        Map<GenomicRegionSearchConstraint, String> spanConstraintMap =
            (HashMap<GenomicRegionSearchConstraint, String>)  request
            .getSession().getAttribute("spanConstraintMap");

        if (spanConstraintMap == null) {
            spanConstraintMap = new HashMap<GenomicRegionSearchConstraint, String>();
        }

        if (spanConstraintMap.size() == 0) {
            spanConstraintMap.put(grsc, spanUUIDString);
        } else {
            if (spanConstraintMap.containsKey(grsc)) {
                spanUUIDString = spanConstraintMap.get(grsc);
                request.setAttribute("spanUUIDString", spanUUIDString);
            } else {
                spanConstraintMap.put(grsc, spanUUIDString);
            }
        }

        request.getSession().setAttribute("spanConstraintMap", spanConstraintMap);
        request.setAttribute("spanQueryTotalCount", grsc.getGenomicRegionList().size());

        (new Thread(this)).start();
    }

    @Override
    public void run() {
        // at r27699
        queryExecutor();
    }

    /**
     * The method to run all the queries.
     */
    private void queryExecutor() {

        // Use spanOverlapFullResultMap to store the data in the session
        Map<String, Map<GenomicRegion, List<List<String>>>> spanOverlapFullResultMap =
             (Map<String, Map<GenomicRegion, List<List<String>>>>) request
                            .getSession().getAttribute("spanOverlapFullResultMap");

        if (spanOverlapFullResultMap == null) {
            spanOverlapFullResultMap =
                new HashMap<String, Map<GenomicRegion, List<List<String>>>>();
        }

        // map of sequence feature statistics: key - class name. value - count of feature
        Map<String, Map<GenomicRegion, Map<String, Integer>>> spanOverlapFullStatMap =
             (Map<String, Map<GenomicRegion, Map<String, Integer>>>) request
                            .getSession().getAttribute("spanOverlapFullStatMap");

        if (spanOverlapFullStatMap == null) {
            spanOverlapFullStatMap =
                new HashMap<String, Map<GenomicRegion, Map<String, Integer>>>();
        }

        Map<GenomicRegion, List<List<String>>> spanOverlapResultDisplayMap = Collections
                .synchronizedMap(new LinkedHashMap<GenomicRegion, List<List<String>>>());

        Map<GenomicRegion, Map<String, Integer>> spanOverlapResultStatMap = Collections
        .synchronizedMap(new LinkedHashMap<GenomicRegion, Map<String, Integer>>());

        if (!spanOverlapFullResultMap.containsKey(spanUUIDString)
                && !spanOverlapFullStatMap.containsKey(spanUUIDString)) {

            spanOverlapFullResultMap.put(spanUUIDString, spanOverlapResultDisplayMap);
            request.getSession().setAttribute("spanOverlapFullResultMap", spanOverlapFullResultMap);

            spanOverlapFullStatMap.put(spanUUIDString, spanOverlapResultStatMap);
            request.getSession().setAttribute("spanOverlapFullStatMap", spanOverlapFullStatMap);

            try {
                ObjectStore os = SessionMethods.getInterMineAPI(
                        request.getSession()).getObjectStore();

                for (Entry<GenomicRegion, Query> e : queryMap.entrySet()) {
                    Results results = os.execute(e.getValue());

                    List<List<String>> spanResults = new ArrayList<List<String>>();

                    Map<String, Integer> spanStatMap = new HashMap<String, Integer>();
                    ValueComparator bvc =  new ValueComparator(spanStatMap);
                    @SuppressWarnings("unchecked")
                    TreeMap<String, Integer> sortedStatMap = new TreeMap<String, Integer>(bvc);

                    if (results == null || results.isEmpty()) {
                        spanOverlapResultDisplayMap.put(e.getKey(), null);
                    }
                    else {
                        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                            ResultsRow<?> row = (ResultsRow<?>) iter.next();

                            List<String> resultRow = new ArrayList<String>();

                            for (Object o : row) {
                                String item = new String();

                                // NULL for symbol or PID
                                o = o == null ? new String() : o;

                                if (o instanceof Class) {
                                    item = ((Class) o).getSimpleName();
                                    // add class stat to spanStatMap
                                    if (spanStatMap.containsKey(item)) {
                                        spanStatMap.put(item, spanStatMap.get(item) + 1);
                                    } else {
                                        spanStatMap.put(item, 1);
                                    }
                                } else {
                                    item = o.toString();
                                }

                                resultRow.add(item);
                            }
                            spanResults.add(resultRow);
                        }
                        spanOverlapResultDisplayMap.put(e.getKey(), spanResults);

                        sortedStatMap.putAll(spanStatMap);
                        spanOverlapResultStatMap.put(e.getKey(), sortedStatMap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Query the information of all the organisms and their chromosomes' names and length. The
     * results is stored in a Map. The result data will be used to validate users' span data.
     * For each span, its chromosome must match the chrPID and range must not go beyond the length.
     *
     * @param im - the InterMineAPI
     * @return chrInfoMap - a HashMap with orgName as key and its chrInfo accordingly as value
     */
    public static Map<String, Map<String, ChromosomeInfo>> getChromosomeInfo(InterMineAPI im) {

        return getChromosomeInfo(im, GenomicRegionSearchService.DEFAULT_REGION_INIT_BATCH_SIZE);
    }

    /**
     * Query the information of all the organisms and their chromosomes' names and length. The
     * results is stored in a Map. The result data will be used to validate users' span data.
     * For each span, its chromosome must match the chrPID and range must not go beyond the length.
     *
     * @param im - the InterMineAPI
     * @param batchSize - the query batch size to use
     * @return chrInfoMap - a HashMap with orgName as key and its chrInfo accordingly as value
     */
    public static Map<String, Map<String, ChromosomeInfo>> getChromosomeInfo(InterMineAPI im,
            int batchSize) {
        if (chrInfoMap != null) {
            return chrInfoMap;
        } else {
            long startTime = System.currentTimeMillis();

            // a Map contains orgName and its chrInfo accordingly
            // e.g. <D.Melanogaster, <X, (D.Melanogaster, X, x, 5000)>>
            chrInfoMap = new HashMap<String, Map<String, ChromosomeInfo>>();


            Query q = new Query();
            q.setDistinct(true);

            QueryClass qcChr = new QueryClass(Chromosome.class);
            QueryClass qcOrg = new QueryClass(Organism.class);

            q.addFrom(qcChr);
            q.addFrom(qcOrg);

            QueryField qfOrgName = new QueryField(qcOrg, "shortName");
            QueryField qfChrIdentifier = new QueryField(qcChr, "primaryIdentifier");
            QueryField qfChrLength = new QueryField(qcChr, "length");

            q.addToSelect(qfOrgName);
            q.addToSelect(qfChrIdentifier);
            q.addToSelect(qfChrLength);

            QueryObjectReference orgRef = new QueryObjectReference(qcChr, "organism");
            ContainsConstraint ccOrg = new ContainsConstraint(orgRef,
                    ConstraintOp.CONTAINS, qcOrg);
            q.setConstraint(ccOrg);

            Results results = im.getObjectStore().execute(q, batchSize, true, true, true);

            // a List contains all the chrInfo (organism, chrPID, length)
            List<ChromosomeInfo> chrInfoList = new ArrayList<ChromosomeInfo>();
            // a Set contains all the orgName
            Set<String> orgSet = new HashSet<String>();
            int entryCount = 0;

            for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
                entryCount++;
                ResultsRow<?> row = (ResultsRow<?>) iter.next();

                String orgName = (String) row.get(0);
                String chrIdentifier = (String) row.get(1);
                Integer chrLength = (Integer) row.get(2);

                // Add orgName to HashSet to filter out duplication
                orgSet.add(orgName);

                ChromosomeInfo chrInfo = new ChromosomeInfo();
                chrInfo.setOrgName(orgName);
                chrInfo.setChrPID(chrIdentifier);
                if (chrLength != null) {
                    chrInfo.setChrLength(chrLength);
                }
                // Add ChromosomeInfo to Arraylist
                chrInfoList.add(chrInfo);

            }


            // Iterate orgSet and chrInfoList to put data in chrInfoMap which
            // has the key as the
            // orgName and value as a ArrayList containing a list of chrInfo
            // which has the same
            // orgName
            for (String o : orgSet) {
                // a map to store chrInfo for the same organism
                Map<String, ChromosomeInfo> chrInfoSubMap = new HashMap<String, ChromosomeInfo>();

                for (ChromosomeInfo chrInfo : chrInfoList) {
                    if (o.equals(chrInfo.getOrgName())) {
                        chrInfoSubMap
                                .put(chrInfo.getChrPIDLowerCase(), chrInfo);
                        chrInfoMap.put(o, chrInfoSubMap);
                    }
                }
            }

            return chrInfoMap;
        }
    }

    /**
     * Query the information of all feature types and their according so terms.
     *
     * @param im - the InterMineAPI
     * @param classDescrs map of feature class/type to description
     * @param batchSize the query batch size to use
     * @return featureTypeToSOTermMap -
     *         a HashMap with featureType as key and its SO info accordingly as value
     */
    public static Map<String, List<String>> getFeatureAndSOInfo(
            InterMineAPI im, Map<String, String> classDescrs, int batchSize) {

        Map<String, List<String>> featureTypeToSOTermMap = new HashMap<String, List<String>>();

        Query q = new Query();
        q.setDistinct(true);

        QueryClass qcFeature = new QueryClass(SequenceFeature.class);
        QueryClass qcSOTerm = new QueryClass(SOTerm.class);

        QueryField qfFeatureClass = new QueryField(qcFeature, "class");
//        QueryField qfSOId = new QueryField(qcSOTerm, "identifier");
        QueryField qfSOName = new QueryField(qcSOTerm, "name");
//        QueryField qfSODescription = new QueryField(qcSOTerm, "description");

        q.addToSelect(qfFeatureClass);
//        q.addToSelect(qfSOId);
        q.addToSelect(qfSOName);
//        q.addToSelect(qfSODescription);

        q.addFrom(qcFeature);
        q.addFrom(qcSOTerm);

        // Make sure the class field isn't the first entry in the order by as it would be used for
        // a large offset constraint BUT the greater than operator is not supported for classes.
        q.addToOrderBy(qfSOName);

        // TODO missing values in the results...e.g. GoldenPathFragment
        QueryObjectReference soTerm = new QueryObjectReference(qcFeature,
                "sequenceOntologyTerm");
        ContainsConstraint ccSoTerm = new ContainsConstraint(soTerm,
                ConstraintOp.CONTAINS, qcSOTerm);
        q.setConstraint(ccSoTerm);

        Results results = im.getObjectStore().execute(q, batchSize, true, true, true);

        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            ResultsRow<?> row = (ResultsRow<?>) iter.next();

            List<String> soInfo = new ArrayList<String>();

            @SuppressWarnings("rawtypes")
            String ft = ((Class) row.get(0)).getSimpleName();
            String soName = (String) row.get(1);
//            String soDes = (String) row.get(2);

//            if (soDes == null) {
//                soDes = "description not avaliable";
//            }

            String soDes = (classDescrs.get(ft) == null) ? "description not avaliable"
                    : classDescrs.get(ft);

            soDes = soDes.replaceAll("'", "&apos;");
            soDes = soDes.replaceAll("\"", "&quot;");

            soInfo.add(soName);
            soInfo.add(soDes);

            if (!featureTypeToSOTermMap.containsKey(ft)) {
                featureTypeToSOTermMap.put(ft, soInfo);
            }
        }

        return featureTypeToSOTermMap;
    }

    /**
     * Query the information of all organisms and their taxon ids.
     *
     * @param im - the InterMineAPI
     * @param batchSize the query batch size
     * @return orgTaxonIdMap - a HashMap with organism  as key and its taxonId as value
     */
    public static Map<String, Integer> getTaxonInfo(InterMineAPI im, int batchSize) {
        long startTime = System.currentTimeMillis();

        Map<String, Integer> orgTaxonIdMap = new HashMap<String, Integer>();
        Query q = new Query();
        QueryClass organisms = new QueryClass(org.intermine.model.bio.Organism.class);
        q.addFrom(organisms);
        q.addToSelect(organisms);

        List<?> orgs = im.getObjectStore().executeSingleton(q, batchSize, true, true, true);
        for (Object o: orgs) {
            org.intermine.model.bio.Organism org = (org.intermine.model.bio.Organism) o;
            orgTaxonIdMap.put(org.getShortName(), org.getTaxonId());
        }

        return orgTaxonIdMap;
    }

    /**
     * query chromosome locations by a list of sequence features, return region string as
     * chr:start..end
     *
     * @param features list of SequenceFeature
     * @param im the InterMineAPI
     * @param profile Profile
     */
    public static void getRegionStringFromSequenceFeatureList(Collection<SequenceFeature> features,
            InterMineAPI im, Profile profile) {

        // TODO
    }

    /**
     * query chromosome locations by a ready-to-use pathquery, return region string as
     * chr:start..end
     *
     * @param query pathquery
     * @param im the InterMineAPI
     * @param profile Profile
     */
    public static void getRegionStringFromPathQuery(PathQuery query, InterMineAPI im,
            Profile profile) {

        //TODO
    }
}

/**
 * Comparator to sort a map on values (integer)
 * http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
 * @author Fengyuan Hu
 *
 */
class ValueComparator implements Comparator
{
    Map<String, Integer> base;

    /**
     * @param base the map itself
     */
    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    @Override
    public int compare(Object a, Object b) {
        if (base.get(a) < base.get(b)) {
            return 1;
        } else if (base.get(a) == base.get(b)) {
            return -1;
        } else {
            return -1;
        }
    }
}
