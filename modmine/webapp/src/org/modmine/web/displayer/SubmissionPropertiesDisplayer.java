package org.modmine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Antibody;
import org.intermine.model.bio.Array;
import org.intermine.model.bio.CellLine;
import org.intermine.model.bio.DevelopmentalStage;
import org.intermine.model.bio.Strain;
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionProperty;
import org.intermine.model.bio.Tissue;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Join submission properties to one table.
 * Refer to OverlappingFeaturesDisplayer and RegulatoryRegionsDisplayer.
 *
 * @author Fengyuan Hu
 *
 */
public class SubmissionPropertiesDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(SubmissionPropertiesDisplayer.class);

//private static final Set<String> PROPERTY_CLASSNAME_SET = new HashSet<String>(
//new LinkedHashSet<String>(Arrays.asList("Antibody", "CellLine", "DevelopmentalStage",
//"Strain", "Tissue", "Array", "GrowthTemperature", "SubmissionProperty")));

    /** @var maximum amount of rows to show per table */
//    private Integer maxCount = 30;

    /**
     * @param config ReportDisplayerConfig
     * @param im InterMineAPI
     */
    public SubmissionPropertiesDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        Submission sub = (Submission) reportObject.getObject();

        //== Organism ==
        Map<Integer, String> organismMap = new HashMap<Integer, String>();
        organismMap.put(sub.getOrganism().getId(), sub.getOrganism().getShortName());
        request.setAttribute("organismMap", organismMap);

        //== Antibody ==
        Set<Antibody> antibodies = new HashSet<Antibody>();
        for (Antibody a : sub.getAntibodies()) {
            if (!"not applicable".equals(a.getName())) {
                antibodies.add(a);
            }
        }
        request.setAttribute("antibodyInfoList", antibodies);

        //== CellLine ==
        Map<Integer, String> cellLineMap = new HashMap<Integer, String>();

        for (CellLine c : sub.getCellLines()) {
            if (!"not applicable".equals(c.getName())) {
                cellLineMap.put(c.getId(), c.getName());
            }
        }

        request.setAttribute("cellLineMap", cellLineMap);

        //== DevelopmentalStage ==
        Map<Integer, String> developmentalStageMap = new HashMap<Integer, String>();

        for (DevelopmentalStage d : sub.getDevelopmentalStages()) {
            if (!"not applicable".equals(d.getName())) {
                developmentalStageMap.put(d.getId(), d.getName());
            }
        }

        request.setAttribute("developmentalStageMap", developmentalStageMap);

        //== Strain ==
        Map<Integer, String> strainMap = new HashMap<Integer, String>();

        for (Strain s : sub.getStrains()) {
            if (!"not applicable".equals(s.getName())) {
                strainMap.put(s.getId(), s.getName());
            }
        }

        request.setAttribute("strainMap", strainMap);

        //== Tissue ==
        Map<Integer, String> tissueMap = new HashMap<Integer, String>();

        for (Tissue t : sub.getTissues()) {
            if (!"not applicable".equals(t.getName())) {
                tissueMap.put(t.getId(), t.getName());
            }
        }

        request.setAttribute("tissueMap", tissueMap);

        //== Array ==
        Map<Integer, String> arrayMap = new HashMap<Integer, String>();

        for (Array a : sub.getArrays()) {
            if (!"not applicable".equals(a.getName())) {
                arrayMap.put(a.getId(), a.getName());
            }
        }

        request.setAttribute("arrayMap", arrayMap);

        //== SubmissionProperty ==
        Map<String, Map<Integer, String>> submissionPropertyMap =
            new HashMap<String, Map<Integer, String>>();

        for (SubmissionProperty sp : sub.getProperties()) {
            if ("SubmissionPropertyShadow".equals(sp.getClass().getSimpleName())) {
                if (!submissionPropertyMap.containsKey(sp.getType())) {
                    Map<Integer, String> propertyMap = new HashMap<Integer, String>();
                    propertyMap.put(sp.getId(), sp.getName());
                    submissionPropertyMap.put(sp.getType(), propertyMap);
                } else {
                    submissionPropertyMap.get(sp.getType()).put(sp.getId(), sp.getName());
                }
            }
        }

        request.setAttribute("submissionPropertyMap", submissionPropertyMap);

        /*
        // group properties by class, to display classes and counts
        Map<String, Integer> propertyCounts = new TreeMap<String, Integer>();
        Map<String, InlineResultsTable> propertyTables = new TreeMap<String, InlineResultsTable>();

        Set<Integer> geneModelIds = GeneModelCache.getGeneModelIds(sub, im.getModel());

        // for properties
        try {
//            @SuppressWarnings("unchecked")
            Collection<InterMineObject> properties =
                (Collection<InterMineObject>) sub.getFieldValue("properties");
            for (InterMineObject p : properties) {
                String className = DynamicUtil.getSimpleClass(p).getSimpleName();

                if (!geneModelIds.contains(p.getId())) {
                    incrementCount(propertyCounts, className);
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Error accessing properties collection for submission: "
                    + sub.getdCCid() + ", " + sub.getId());
        }
        */

        /**
        // for developmental stage use
        for (String key : propertyCounts.keySet()) {
            propertyClassNameSet.add(key.toLowerCase());
        }
        **/

        /**
        // experimentalFactors = submissionProperties
        try {
            @SuppressWarnings("unchecked")
            Collection<InterMineObject> experimentalFactors =
                (Collection<InterMineObject>) sub.getFieldValue("experimentalFactors");
            for (InterMineObject ef : experimentalFactors) {
                if (!geneModelIds.contains(ef.getId())) {
                    incrementCount(propertyCounts, ef);
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Error accessing properties collection for submission: "
                    + sub.getdCCid() + ", " + sub.getId());
        }
        **/

        /*
        // resolve Collection from FieldDescriptor
        for (FieldDescriptor fd : reportObject.getClassDescriptor().getAllFieldDescriptors()) {

            // Case : properties
            if ("properties".equals(fd.getName()) && fd.isCollection()) {
                Collection<?> collection = null;
                try {
                    collection = (Collection<?>)
                    reportObject.getObject().getFieldValue("properties");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                // make collection into a list
                List<?> collectionList;
                if (collection instanceof List<?>) {
                    collectionList = (List<?>) collection;
                } else {
                    if (collection instanceof LazyCollection<?>) {
                        collectionList = ((LazyCollection<?>) collection).asList();
                    } else {
                        collectionList = new ArrayList(collection);
                    }
                }

                // get the classes
                List<Class<?>> lt = PathQueryResultHelper.
                queryForTypesInCollection(reportObject.getObject(), "properties",
                        im.getObjectStore());

                looptyloop:
                    for (Class<?> c : lt) {
                        Iterator<?> resultsIter = collectionList.iterator();

                        // new collection of objects of only type "c"
                        List<InterMineObject> cl = new ArrayList<InterMineObject>();

                        String className = null;
                        Integer count = this.maxCount;
                        // loop through each row object
                        while (resultsIter.hasNext() && count > 0) {
                            Object o = resultsIter.next();
                            if (o instanceof ProxyReference) {
                                // special case for ProxyReference from DisplayReference objects
                                o = ((ProxyReference) o).getObject();
                            }
                            // cast
                            InterMineObject imObj = (InterMineObject) o;
                            // type match?
                            Class<?> imObjClass = DynamicUtil.getSimpleClass(imObj);
                            if (c.equals(imObjClass)) {
                                count--;
                                cl.add(imObj);
                                // determine type
                                className = DynamicUtil.getSimpleClass(cl.get(0)).getSimpleName();

                                // do we actually want any of this? <-- what's this for?
                                if (!propertyCounts.containsKey(className)) {
                                    continue looptyloop;
                                }
                            }
                        }

                        if (cl.size() > 0) {
                            // one element list
                            ArrayList<Class<?>> lc = new ArrayList<Class<?>>();
                            lc.add(c);

                            // create an InlineResultsTable
                            InlineResultsTable t = new InlineResultsTable(cl, fd
                                    .getClassDescriptor().getModel(),
                                    SessionMethods.getWebConfig(request),
                                    im.getClassKeys(), cl.size(), false, lc);

                            // name the table based on the first element contained
                            propertyTables.put(className, t);
                        }
                    }
            }
            */

            /**
            // Case : experimentalFactors
            if ("experimentalFactors".equals(fd.getName()) && fd.isCollection()) {
                Collection<?> collection = null;
                try {
                    collection = (Collection<?>)
                    reportObject.getObject().getFieldValue("experimentalFactors");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                // make collection into a list
                List<?> collectionList;
                if (collection instanceof List<?>) {
                    collectionList = (List<?>) collection;
                } else {
                    if (collection instanceof LazyCollection<?>) {
                        collectionList = ((LazyCollection<?>) collection).asList();
                    } else {
                        collectionList = new ArrayList(collection);
                    }
                }

                // get the classes
                List<Class<?>> lt = PathQueryResultHelper.
                queryForTypesInCollection(reportObject.getObject(), "experimentalFactors",
                        im.getObjectStore());

                LOG.info("experimentalFactors size >>>>> " + lt);

                Class<?> c = lt.get(0);

                Iterator<?> resultsIter = collectionList.iterator();

                // new collection of objects of only type "c"
                List<InterMineObject> cl = new ArrayList<InterMineObject>();

                String className = null;
                Integer count = this.maxCount;
                // loop through each row object
                while (resultsIter.hasNext() && count > 0) {
                    Object o = resultsIter.next();
                    if (o instanceof ProxyReference) {
                        // special case for ProxyReference from DisplayReference objects
                        o = ((ProxyReference) o).getObject();
                    }
                    // cast
                    InterMineObject imObj = (InterMineObject) o;
                    // type match?
                    Class<?> imObjClass = DynamicUtil.getSimpleClass(imObj);
                    if (c.equals(imObjClass)) {
                        count--;
                        cl.add(imObj);
                        // determine type
                        className = DynamicUtil.getSimpleClass(cl.get(0)).getSimpleName();
                    }
                }

                if (cl.size() > 0) {
                    // one element list
                    ArrayList<Class<?>> lc = new ArrayList<Class<?>>();
                    lc.add(c);

                    // create an InlineResultsTable
                    InlineResultsTable t = new InlineResultsTable(cl, fd
                            .getClassDescriptor().getModel(),
                            SessionMethods.getWebConfig(request),
                            im.getClassKeys(), cl.size(), false, lc);

                    // The tricky part, to remove the duplicated items between
                    // properties and experimentalStage
                    List<Object> toRemove = new ArrayList<Object>();
                    for (Object r : t.getResultElementRows()) {
                            for(Object o : ((InlineResultsTableRow)r).getItems()){
                            if (propertyClassNameSet.contains(StringUtil.join(
                                    Arrays.asList(((ResultElement) o)
                                            .getField().toString().split(" ")),
                                    "").toLowerCase())) {
                                    toRemove.add(r);
                                    break;
                                }
                            }
                    }

                    for (Object r : toRemove) {
                        t.getResultElementRows().remove(r);
                    }

                    propertyCounts.put(className, propertyCounts.get(className) - toRemove.size());

                    // name the table based on the first element contained
                    propertyTables.put(className, t);
                }
            }
            **/
        /*
        }

        Map<String, Integer> additionalPropertyFields = new HashMap<String, Integer>();
        for (String className : PROPERTY_CLASSNAME_SET) {
            if (!propertyCounts.keySet().contains(className)) {
                additionalPropertyFields.put(className, 0);
            }
        }
        propertyCounts.putAll(additionalPropertyFields);


        request.setAttribute("propertyCounts", propertyCounts);

        request.setAttribute("propertyTables", propertyTables);
        */
    }

    /*
    private void incrementCount(Map<String, Integer> propertyCounts, String className) {

        Integer count = propertyCounts.get(className);
        if (count == null) {
            count = new Integer(0);
            propertyCounts.put(className, count);
        }
        propertyCounts.put(className, new Integer(count.intValue() + 1));
    }
    */
}