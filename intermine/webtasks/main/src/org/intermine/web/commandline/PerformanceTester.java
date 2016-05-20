package org.intermine.web.commandline;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryHelper;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.query.MainHelper;
import org.intermine.api.search.SearchFilterEngine;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.SqlGenerator;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.SynchronisedIterator;

/**
 * Class to run a performance test on a production database by loading a set of template queries
 * from a userprofile database and running them in varying numbers of threads.
 *
 * @author Matthew Wakeling
 */
public final class PerformanceTester
{
    private PerformanceTester() {
        // Hidden constructor.
    }

    private static String superuser;
    private static ProfileManager pm = null;

    /**.
     * @param args number of threads you want to run
     * @throws Exception if something goes horribly wrong
     */
    public static void main(String[] args) throws Exception {
        superuser = PropertiesUtil.getProperties().getProperty("superuser.account");
        ObjectStore productionOs = ObjectStoreFactory.getObjectStore("os.production");
        ObjectStoreFactory.getObjectStore("os.userprofile-production");
        ObjectStoreWriter userProfileOs = ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.userprofile-production");
        Properties classKeyProps = new Properties();
        classKeyProps.load(PerformanceTester.class.getClassLoader()
                .getResourceAsStream("class_keys.properties"));
        BagQueryConfig bagQueryConfig = BagQueryHelper.readBagQueryConfig(productionOs.getModel(),
                PerformanceTester.class.getClassLoader()
                .getResourceAsStream("webapp/WEB-INF/bag-queries.xml"));
        Map<String, List<FieldDescriptor>> classKeys = ClassKeyHelper.readKeys(productionOs
                .getModel(), classKeyProps);

        pm = new ProfileManager(productionOs, userProfileOs);
        Profile p = pm.getProfile(superuser);

        Map<String, ApiTemplate> templates = p.getSavedTemplates();


        templates = new SearchFilterEngine().filterByTags(templates,
                Collections.singletonList(TagNames.IM_PUBLIC),
                TagTypes.TEMPLATE, superuser, new TagManagerFactory(userProfileOs).getTagManager());
        templates.remove("ESTclone_LocationDMorthologuePathway_new");
        templates.remove("ESTclone_LocationOverlappingGeneOrthologue_new");
        templates.remove("ESTclone_LocationOverlappingGeneStructure");
        templates.remove("Organism_interologues");

        int i = Integer.parseInt(args[0]);
        System .out.println("Running with " + i + " threads:");
        doRun(productionOs, classKeys, bagQueryConfig, templates, i);
    }

    private static void doRun(ObjectStore productionOs,
            Map<String, List<FieldDescriptor>> classKeys, BagQueryConfig bagQueryConfig,
            Map<String, ApiTemplate> templates, int threadCount) {
        long startTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, ApiTemplate>> iter
            = new SynchronisedIterator<Map.Entry<String, ApiTemplate>>(templates.entrySet()
                    .iterator());
        Set<Integer> threads = new HashSet<Integer>();

        synchronized (threads) {
            for (int i = 1; i < threadCount; i++) {
                Thread worker = new Thread(new Worker(productionOs, classKeys, bagQueryConfig,
                            threads, iter, i));
                threads.add(new Integer(i));
                worker.start();
            }
        }

        try {
            while (iter.hasNext()) {
                Map.Entry<String, ApiTemplate> entry = iter.next();
                doQuery(productionOs, classKeys, bagQueryConfig, entry.getKey(), entry.getValue(),
                        0);
            }
        } catch (NoSuchElementException e) {
            // This is fine - just a consequence of concurrent access to the iterator. It means the
            // end of the iterator has been reached, so there is no more work to do.
        }
        //System .out.println("Thread 0 finished");
        synchronized (threads) {
            while (threads.size() != 0) {
                //System .out.println(threads.size() + " threads left");
                try {
                    threads.wait();
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }
        System .out.println("Whole run took " + (System.currentTimeMillis() - startTime) + " ms");
    }


    private static void doQuery(ObjectStore productionOs,
            Map<String, List<FieldDescriptor>> classKeys, BagQueryConfig bagQueryConfig,
            String templateName, TemplateQuery templateQuery, int threadNo) {
        try {
            //Query q = TemplateHelper.getPrecomputeQuery(entry.getValue(), new ArrayList(), null);
            long queryStartTime = System.currentTimeMillis();
            TemplateManager templateManager = new TemplateManager(pm.getSuperuserProfile(),
                    productionOs.getModel());

            BagQueryRunner bqr = new BagQueryRunner(productionOs, classKeys, bagQueryConfig,
                    templateManager);
            Query q = MainHelper.makeQuery(templateQuery, new HashMap<String, InterMineBag>(),
                    new HashMap<String, QuerySelectable>(), bqr, null);
            String sqlString = SqlGenerator.generate(q, 0, Integer.MAX_VALUE,
                    ((ObjectStoreInterMineImpl) productionOs).getSchema(),
                    ((ObjectStoreInterMineImpl) productionOs).getDatabase(),
                    (Map<Object, String>) null);
            System .out.println("Thread " + threadNo + ": executing template " + templateName
                    + " with query " + q + ", SQL: " + sqlString);
            List<?> results = productionOs.execute(q, 0, 1000, false, false,
                    ObjectStore.SEQUENCE_IGNORE);
            long queryEndTime = System.currentTimeMillis();
            System .out.println("Thread " + threadNo + ": template " + templateName + " took "
                    + (queryEndTime - queryStartTime) + " ms");
            if (results.isEmpty()) {
                System .out.println("Thread " + threadNo + ": template " + templateName
                        + " returned 0 rows");
            } else if (results.size() < 1000) {
                System .out.println("Thread " + threadNo + ": template " + templateName
                        + " returned " + results.size()
                        + " rows");
            } else {
                int count = productionOs.count(q, ObjectStore.SEQUENCE_IGNORE);
                System .out.println("Thread " + threadNo + ": template " + templateName
                        + " returned " + count + " rows (took "
                        + (System.currentTimeMillis() - queryEndTime) + " ms for count)");
            }
        } catch (Exception e) {
            System .err.println("Thread " + threadNo + ": template " + templateName
                    + " could not be run.");
//            // need a number to compare between releases
//            System .out.println("Thread " + threadNo + ": template " + templateName
//                                + " returned 0 rows");
            e.printStackTrace(System.err);

        }
    }

    private static class Worker implements Runnable
    {
        private ObjectStore productionOs;
        private Map<String, List<FieldDescriptor>> classKeys;
        private BagQueryConfig bagQueryConfig;
        private Set<Integer> threads;
        private Iterator<Map.Entry<String, ApiTemplate>> iter;
        private int threadNo;

        /**
         *
         * @param productionOs
         * @param classKeys
         * @param bagQueryConfig
         * @param threads
         * @param iter
         * @param threadNo
         */
        public Worker(ObjectStore productionOs, Map<String, List<FieldDescriptor>> classKeys,
                BagQueryConfig bagQueryConfig, Set<Integer> threads,
                Iterator<Map.Entry<String, ApiTemplate>> iter, int threadNo) {
            this.productionOs = productionOs;
            this.classKeys = classKeys;
            this.bagQueryConfig = bagQueryConfig;
            this.threads = threads;
            this.iter = iter;
            this.threadNo = threadNo;
        }

        public void run() {
            try {
                while (iter.hasNext()) {
                    Map.Entry<String, ApiTemplate> entry = iter.next();
                    doQuery(productionOs, classKeys, bagQueryConfig, entry.getKey(),
                            entry.getValue(), threadNo);
                }
            } catch (NoSuchElementException e) {
                // Empty
            } finally {
                //System .out.println("Thread " + threadNo + " finished");
                synchronized (threads) {
                    threads.remove(new Integer(threadNo));
                    threads.notify();
                }
            }
        }
    }
}
