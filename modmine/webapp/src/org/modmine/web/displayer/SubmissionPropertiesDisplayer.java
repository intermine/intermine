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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.GeneModelCache;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.proxy.LazyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.InlineResultsTableRow;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Join submission properties to one table.
 * Refer to OverlappingFeaturesDisplayer and RegulatoryRegionsDisplayer.
 *
 * @author Fengyuan Hu
 *
 */
public class SubmissionPropertiesDisplayer extends CustomDisplayer {

	protected static final Logger LOG = Logger.getLogger(SubmissionPropertiesDisplayer.class);

	/** @var maximum amount of rows to show per table */
    private Integer maxCount = 30;

	public SubmissionPropertiesDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
		super(config, im);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void display(HttpServletRequest request, ReportObject reportObject) {

		// group properties by class, to display classes and counts
        Map<String, Integer> propertyCounts = new TreeMap<String, Integer>();
        Map<String, InlineResultsTable> propertyTables = new TreeMap<String, InlineResultsTable>();

        // store the class names of properties
        Set<String> propertyClassNameSet = new HashSet<String>();

        Submission sub = (Submission) reportObject.getObject();

        Set<Integer> geneModelIds = GeneModelCache.getGeneModelIds(sub, im.getModel());

        // for properties
        try {
            @SuppressWarnings("unchecked")
			Collection<InterMineObject> properties =
                (Collection<InterMineObject>) sub.getFieldValue("properties");
            for (InterMineObject p : properties) {
                if (!geneModelIds.contains(p.getId())) {
                    incrementCount(propertyCounts, p);
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error("Error accessing properties collection for submission: "
                    + sub.getdCCid() + ", " + sub.getId());
        }


        // for developmental stage use
        for (String key : propertyCounts.keySet()) {
        	propertyClassNameSet.add(key.toLowerCase());
        }

        // for experimentalFactors
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
        }


        request.setAttribute("propertyCounts", propertyCounts);

        request.setAttribute("propertyTables", propertyTables);
	}

    private void incrementCount(Map<String, Integer> propertyCounts, InterMineObject property) {
        String className = DynamicUtil.getSimpleClass(property).getSimpleName();

        Integer count = propertyCounts.get(className);
        if (count == null) {
            count = new Integer(0);
            propertyCounts.put(className, count);
        }
        propertyCounts.put(className, new Integer(count.intValue() + 1));
    }

}
