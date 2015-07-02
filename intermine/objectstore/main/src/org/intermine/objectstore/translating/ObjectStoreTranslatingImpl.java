package org.intermine.objectstore.translating;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreAbstractImpl;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.sql.DatabaseConnectionException;

/**
 * ObjectStore that transparently translates incoming queries and outgoing objects
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ObjectStoreTranslatingImpl extends ObjectStoreAbstractImpl
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreTranslatingImpl.class);
    private ObjectStore os;
    private Translator translator;
    private Map<Query, Query> queryCache = Collections.synchronizedMap(
            new WeakHashMap<Query, Query>());

    /**
     * Constructor
     * @param model the Model that this ObjectStore appears to use
     * @param os the underlying ObjectStore
     * @param translator the Translator used to translate queries and objects
     */
    public ObjectStoreTranslatingImpl(Model model, ObjectStore os, Translator translator) {
        super(model);
        this.os = os;
        this.translator = translator;
        translator.setObjectStore(this);
    }

    /**
     * Gets an ObjectStore for the given underlying properties.
     *
     * @param osAlias the alias of this objectstore
     * @param props the properties used to configure the objectstore
     * @return the ObjectStore
     * @throws IllegalArgumentException if props or model are invalid
     * @throws ObjectStoreException if there is any problem with the instance
     */
    public static ObjectStoreTranslatingImpl getInstance(String osAlias, Properties props)
        throws ObjectStoreException {
        String subAlias = props.getProperty("os");
        if (subAlias == null) {
            throw new IllegalArgumentException("No 'os' property specified for Translating"
                    + " ObjectStore (check properties file)");
        }
        String translatorClass = props.getProperty("translatorClass");
        if (translatorClass == null) {
            throw new IllegalArgumentException("No 'translatorClass' property specified for"
                    + " Translating ObjectStore (check properties file)");
        }
        ObjectStore sub;
        try {
            sub = ObjectStoreFactory.getObjectStore(subAlias);
        } catch (DatabaseConnectionException e) {
            throw new ObjectStoreException("Failed to get database connection when creating"
                    + " ObjectStore", e);
        } catch (Exception e) {
            // preserve ObjectStoreExceptions for more useful message
            Throwable t = e.getCause();
            if (t instanceof ObjectStoreException) {
                throw (ObjectStoreException) t;
            } else {
                throw new IllegalArgumentException("Unable to get sub-ObjectStore for Translating"
                    + " ObjectStore (check properties file)");
            }
        }
        Model classpathModel;
        try {
            classpathModel = getModelFromClasspath(osAlias, props);
        } catch (MetaDataException metaDataException) {
            throw new ObjectStoreException("Cannot load model", metaDataException);
        }
        Translator t;
        try {
            Class<?> c = Class.forName(translatorClass);
            Constructor<?> con = c.getConstructor(new Class[] {Model.class, ObjectStore.class});
            t = (Translator) con.newInstance(new Object[] {classpathModel, sub});
        } catch (Exception e) {
            // preserve ObjectStoreExceptions for more useful message
            Throwable thr = e.getCause();
            if (thr instanceof ObjectStoreException) {
                throw (ObjectStoreException) thr;
            } else {
                IllegalArgumentException e2 = new IllegalArgumentException("Cannot find "
                        + "specified Translator class for Translating ObjectStore (check "
                        + "properties file)");
                e2.initCause(e);
                throw e2;
            }
        }

        return new ObjectStoreTranslatingImpl(classpathModel, sub, t);
    }

    /**
     * Return the Translator that was passed to the constructor.
     * @return the Translator
     */
    public Translator getTranslator() {
        return translator;
    }

    private long timeSpentQuery = 0;
    private long timeSpentExecute = 0;
    private long timeSpentTranslate = 0;
    private int queryCount = 0;
    private int objectCount = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResultsRow<Object>> execute(Query q, int start, int limit, boolean optimise,
            boolean explain, Map<Object, Integer> sequence) throws ObjectStoreException {
        //if (start == 0) {
        //    LOG.error("Fetching batch 0 for query " + q.toString());
        //}
        long time1 = System.currentTimeMillis();
        Query q2 = translateQuery(q);
        //LOG.error("Translated query " + q + " to " + q2);
        long time2 = System.currentTimeMillis();
        timeSpentQuery += time2 - time1;
        List<ResultsRow<Object>> results = new ArrayList<ResultsRow<Object>>();
        List<ResultsRow<Object>> origResults = os.execute(q2, start, limit, optimise, explain,
                sequence);
        time1 = System.currentTimeMillis();
        timeSpentExecute += time1 - time2;

        try {
            for (ResultsRow<Object> origRow : origResults) {
                ResultsRow<Object> row = new ResultsRow<Object>();
                for (Object o : origRow) {
                    if (o instanceof InterMineObject) {
                        Object imo = translator.translateFromDbObject(o);
                        row.add(imo);
                        if (imo instanceof InterMineObject) {
                            cacheObjectById(((InterMineObject) imo).getId(), (InterMineObject) imo);
                        }
                        objectCount++;
                    } else {
                        row.add(o);
                    }
                }
                results.add(row);
            }
        } catch (MetaDataException e) {
            throw new ObjectStoreException(e);
        }
        time2 = System.currentTimeMillis();
        timeSpentTranslate += time2 - time1;
        queryCount++;
        if (queryCount % 10000 == 0) {
            LOG.info("Translated " + queryCount + " queries, " + objectCount + " objects. Time"
                    + " spent: Translate query: " + timeSpentQuery + ", Execute: "
                    + timeSpentExecute + ", Translate objects: " + timeSpentTranslate);
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(translateQuery(q));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count(Query q, Map<Object, Integer> sequence) throws ObjectStoreException {
        return os.count(translateQuery(q), sequence);
    }

    private Query translateQuery(Query q) throws ObjectStoreException {
        Query retval = queryCache.get(q);
        if (retval == null) {
            retval = translator.translateQuery(q);
            queryCache.put(q, retval);
        }
        return retval;
    }

    @Override
    public <T extends InterMineObject> T getObjectByExample(T o, Set<String> fieldNames) {
        throw new UnsupportedOperationException("getObjectByExample not supported by"
                + "ObjectStoreTranslatingImpl");
    }

    @Override
    public <T extends InterMineObject> Collection<T> getObjectsByExample(T o, Set<String> fs) {
        throw new UnsupportedOperationException("getObjectsByExample not supported by"
                    + "ObjectStoreTranslatingImpl");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiConnection() {
        return os.isMultiConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Object> getComponentsForQuery(Query q) {
        return Collections.emptySet();
    }

    private int internalGetObjectByIdCount = 0;
    /**
     * {@inheritDoc}
     */
    @Override
    public InterMineObject internalGetObjectById(Integer id,
            Class<? extends InterMineObject> clazz) throws ObjectStoreException {
        InterMineObject retval = super.internalGetObjectById(id, clazz);
        //Exception e = new Exception("internalGetObjectById called for "
        //        + retval.getClass().toString() + " with id " + id);
        //e.fillInStackTrace();
        //java.io.StringWriter sw = new java.io.StringWriter();
        //java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        //e.printStackTrace(pw);
        //pw.flush();
        //LOG.error(sw.toString());
        synchronized (cache) {
            Exception e = new Exception();
            e.fillInStackTrace();
            LOG.warn("Probable inefficiency: internalGetObjectById called "
                    + (retval == null ? "" : "to fetch a " + Util.getFriendlyName(retval
                            .getClass())) + " with id " + id + ", clazz " + clazz.toString()
                    + ", cache size = " + cache.size() + " - maybe you should use"
                    + " ObjectStoreFastCollectionsForTranslatorImpl", e);
        }
        internalGetObjectByIdCount++;
        if (internalGetObjectByIdCount % 1000 == 0) {
            LOG.info("internalGetObjectById run " + internalGetObjectByIdCount + " times");
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSerial() throws ObjectStoreException {
        return os.getSerial();
    }
}
