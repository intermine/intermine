package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.model.GeneModelCache;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.DisplayCollection;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for features overlapping a particular SequenceFeature using the overlappingFeatures
 * collection.  Features are divided by type with a count provided.  For gene model components other
 * objects in the gene model are excluded by id - e.g. for an exon this won't display the transcript
 * and gene that the exon is a member of because they will always overlap.
 * @author Richard Smith
 *
 */
public class OverlappingFeaturesDisplayer extends CustomDisplayer
{

    protected static final Logger LOG = Logger.getLogger(OverlappingFeaturesDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public OverlappingFeaturesDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        // TODO check if type is a gene model type

        // group other overlapping features by type, to display types and counts
        Map<String, Integer> featureCounts = new HashMap<String, Integer>();
        Map<String, InlineResultsTable> featureTables = new HashMap<String, InlineResultsTable>();

        // resolve Collection from FieldDescriptor
        for (FieldDescriptor fd : reportObject.getClassDescriptor().getAllFieldDescriptors()) {
            if ("overlappingFeatures".equals(fd.getName()) && fd.isCollection()) {
                // fetch the collection
                Collection<?> collection = (Collection<?>)
                    reportObject.getFieldValue("overlappingFeatures");

                // get the types
                List<Class<?>> lt = PathQueryResultHelper.
                queryForTypesInCollection(reportObject.getObject(), "overlappingFeatures",
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
                Map<String, InlineResultsTable> m = new HashMap<String, InlineResultsTable>();
                for (Class<?> c : lt) {
                    Iterator<?> resultsIter = collectionList.iterator();

                    // new collection of objects of only type "c"
                    Set<InterMineObject> s = new HashSet<InterMineObject>();

                    // loop through each row object
                    while (resultsIter.hasNext()) {
                        Object o = resultsIter.next();
                        if (o instanceof ProxyReference) {
                            // special case for ProxyReference from DisplayReference objects
                            o = ((ProxyReference) o).getObject();
                        }
                        // cast
                        InterMineObject imObj = (InterMineObject) o;
                        // type match?
                        if (c.equals(DynamicUtil.getSimpleClass(imObj).getName())) {
                            s.add(imObj);
                        }
                    }

                    // create a DisplayCollection


                    m.put(c.toString(), t);
                }
            }
        }

        SequenceFeature startFeature = (SequenceFeature) reportObject.getObject();

        Set<Integer> geneModelIds = GeneModelCache.getGeneModelIds(startFeature, im.getModel());
        try {
            Collection<InterMineObject> overlappingFeatures =
                (Collection<InterMineObject>) startFeature.getFieldValue("overlappingFeatures");
            for (InterMineObject feature : overlappingFeatures) {
                if (!geneModelIds.contains(feature.getId())) {
                    incrementCount(featureCounts, feature);
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Error accessing overlappingFeatures collection for feature: "
                    + startFeature.getPrimaryIdentifier() + ", " + startFeature.getId());
        }
        request.setAttribute("featureCounts", featureCounts);
    }

    private void incrementCount(Map<String, Integer> featureCounts, InterMineObject feature) {
        String type = DynamicUtil.getSimpleClass(feature).getSimpleName();
        Integer count = featureCounts.get(type);
        if (count == null) {
            count = new Integer(0);
            featureCounts.put(type, count);
        }
        featureCounts.put(type, new Integer(count.intValue() + 1));
    }

}
