package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.model.GeneModelCache;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * TODO: merge this with OverlappingFeaturesDisplayer to reuse common functionality (preferrably wo/
 * creating another table type on a report page
 *
 * @author Richard Smith
 *
 */
public class RegulatoryRegionsDisplayer extends ReportDisplayer
{
    /** @var maximum amount of rows to show per table */
    private Integer maxCount = 30;

    protected static final Logger LOG = Logger.getLogger(RegulatoryRegionsDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public RegulatoryRegionsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        // TODO check if type is a gene model type

        // group other overlapping features by type, to display types and counts
        Map<String, Integer> regionCounts = new TreeMap<String, Integer>();
        Map<String, InlineResultsTable> regionTables = new TreeMap<String, InlineResultsTable>();

        SequenceFeature startRegion = (SequenceFeature) reportObject.getObject();

        Set<Integer> geneModelIds = GeneModelCache.getGeneModelIds(startRegion, im.getModel());
        try {
            Collection<InterMineObject> regulatoryRegions =
                (Collection<InterMineObject>) startRegion.getFieldValue("regulatoryRegions");
            for (InterMineObject region : regulatoryRegions) {
                if (!geneModelIds.contains(region.getId())) {
                    incrementCount(regionCounts, region);
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Error accessing regulatoryRegions collection for region: "
                    + startRegion.getPrimaryIdentifier() + ", " + startRegion.getId());
        }
        request.setAttribute("regionCounts", regionCounts);

        // resolve Collection from FieldDescriptor
        for (FieldDescriptor fd : reportObject.getClassDescriptor().getAllFieldDescriptors()) {
            if ("regulatoryRegions".equals(fd.getName()) && fd.isCollection()) {
                // fetch the collection
                Collection<?> collection = null;
                try {
                    collection = (Collection<?>)
                        reportObject.getObject().getFieldValue("regulatoryRegions");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                // get the types
                List<Class<?>> lt = PathQueryResultHelper.
                        queryForTypesInCollection(reportObject.getObject(), "regulatoryRegions",
                        im.getObjectStore());

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

            // separate objects into their types
            looptyloop:
                for (Class<?> c : lt) {
                    Iterator<?> resultsIter = collectionList.iterator();

                    // new collection of objects of only type "c"
                    List<InterMineObject> s = new ArrayList<InterMineObject>();

                    String type = null;
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
                            s.add(imObj);
                            // determine type
                            type = DynamicUtil.getSimpleClass(s.get(0)).getSimpleName();
                            // do we actually want any of this?
                            if (!regionCounts.containsKey(type)) {
                                continue looptyloop;
                            }
                        }
                    }

                    if (s.size() > 0) {
                        // one element list
                        ArrayList<Class<?>> lc = new ArrayList<Class<?>>();
                        lc.add(c);

                        // create an InlineResultsTable
                        InlineResultsTable t = new InlineResultsTable(s,
                                fd.getClassDescriptor().getModel(),
                                SessionMethods.getWebConfig(request), im.getClassKeys(), s.size(),
                                false, lc);

                        // name the table based on the first element contained
                        regionTables.put(type, t);
                    }
                }
            }
        }

        request.setAttribute("regionTables", regionTables);
    }

    private void incrementCount(Map<String, Integer> regionCounts, InterMineObject feature) {
        String type = DynamicUtil.getSimpleClass(feature).getSimpleName();
        Integer count = regionCounts.get(type);
        if (count == null) {
            count = new Integer(0);
            regionCounts.put(type, count);
        }
        regionCounts.put(type, new Integer(count.intValue() + 1));
    }

}
