package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * @author contrino
 *
 */
public class CategoryExperiments
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);
    private static Map<String, List<DisplayExperiment>> catExperimentCache = null;

    /**
     * Get an ordered map [category:experiments]
     * @param servletContext from the controller
     * @param os the objectStore
     * @return the ordered map category/experiments
     * @throws ObjectStoreException if error reading database
     */
    public static synchronized Map<String, List<DisplayExperiment>>
    getCategoryExperiments(ServletContext servletContext, ObjectStore os)
        throws ObjectStoreException {
        if (catExperimentCache == null) {
            readCategoryExperiments(servletContext, os);
        }
        return catExperimentCache;
    }

    /**
     * Build an ordered map from category to experiments.
     * @param os the production ObjectStore
     * @param servletContext from the controller
     * @return an ordered map from category to experiments
     */
    public static Map<String, List<DisplayExperiment>>
    readCategoryExperiments(ServletContext servletContext, ObjectStore os) {
        long startTime = System.currentTimeMillis();
        catExperimentCache
            = new LinkedHashMap<String, List<DisplayExperiment>>();

        Properties propCat = new Properties();
        Properties propOrd = new Properties();

        InputStream is =
            servletContext.getResourceAsStream("/WEB-INF/experimentCategory.properties");
        if (is == null) {
            LOG.error("Unable to find /WEB-INF/experimentCategory.properties!");
        } else {
            try {
                propCat.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InputStream is2 =
            servletContext.getResourceAsStream("/WEB-INF/categoryOrder.properties");
        if (is == null) {
            LOG.error("Unable to find /WEB-INF/category.properties!");
        } else {
            try {
                propOrd.load(is2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, List<DisplayExperiment>> catExpUnordered =
            new HashMap<String, List<DisplayExperiment>>();


        for (DisplayExperiment de : MetadataCache.getExperiments(os)) {
            //            for (DisplayExperiment de : exp) {
            String cats = propCat.getProperty(de.getName());
            if (cats == null) {
                LOG.error("Experiment **" + de.getName() + "** is missing category: "
                        + "please edit "
                        + "webapp/resources/webapp/WEB-INF/experimentCategory.properties");
            } else {
                // an experiment can be associated to more than 1 category
                String[] cat = cats.split("#");
                for (String c : cat) {
                    List<DisplayExperiment> des = catExpUnordered.get(c);
                    if (des == null) {
                        des = new ArrayList<DisplayExperiment>();
                        catExpUnordered.put(c, des);
                    }
                    des.add(de);
                }
            }
        }

        for (Integer i = 1; i <= propOrd.size(); i++) {
            String ordCat = propOrd.getProperty(i.toString());
//            LOG.info("OC: " + ordCat + "|" + catExpUnordered.get(ordCat));
            catExperimentCache.put(ordCat, catExpUnordered.get(ordCat));
        }

        long totalTime = System.currentTimeMillis() - startTime;
        LOG.info("Made categories map of size " + catExperimentCache.size()
                + " in " + totalTime + " ms.");
        return catExperimentCache;
    }

}
