package org.flymine.objectstore.translating;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.flymine.metadata.Model;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.objectstore.query.ResultsRow;

/**
 * ObjectStore that transparently translates incoming queries and outgoing objects
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ObjectStoreTranslatingImpl extends ObjectStoreAbstractImpl
{
    private ObjectStore os;
    private Translator translator;
    
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
     * @param props the properties used to configure the objectstore
     * @param model the metadata associated with this objectstore
     * @return the ObjectStore
     * @throws IllegalArgumentException if props or model are invalid
     * @throws ObjectStoreException if there is any problem with the instance
     */
    public static ObjectStoreTranslatingImpl getInstance(Properties props, Model model)
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
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to get sub-ObjectStore for Translating"
                    + " ObjectStore (check properties file)");
        }
        Translator t;
        try {
            Class c = Class.forName(translatorClass);
            Constructor con = c.getConstructor(new Class[] {Model.class});
            t = (Translator) con.newInstance(new Object[] {model});
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot find specified Translator class for"
                    + " Translating ObjectStore (check properties file)");
        }
        return new ObjectStoreTranslatingImpl(model, sub, t);
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean)
     */
    public List execute(Query q, int start, int limit, boolean optimise, int sequence)
    throws ObjectStoreException {
        Query q2 = translator.translateQuery(q);
        List results = new ArrayList();
        Iterator resIter = os.execute(q2, start, limit, optimise, sequence).iterator();
        while (resIter.hasNext()) {
            ResultsRow row = new ResultsRow();
            Iterator rowIter = ((ResultsRow) resIter.next()).iterator();
            while (rowIter.hasNext()) {
                Object o = rowIter.next();
                if (o instanceof FlyMineBusinessObject) {
                    FlyMineBusinessObject fmbo =
                        translator.translateFromDbObject((FlyMineBusinessObject) o);
                    row.add(fmbo);
                    cacheObjectById(fmbo.getId(), fmbo);
                } else {
                    row.add(o);
                }
            }
            results.add(row);
        }
        
        return results;
    }
    
    /**
     * @see ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(translator.translateQuery(q));
    }
    
    /**
     * @see ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return os.count(translator.translateQuery(q), sequence);
    }
    
    /**
     * @see ObjectStore#getObjectByExample
     */
    public FlyMineBusinessObject getObjectByExample(FlyMineBusinessObject o, Set fieldNames)
        throws ObjectStoreException {
        throw new UnsupportedOperationException("getObjectByExample not supported by"
        + "ObjectStoreTranslatingImpl");
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return os.isMultiConnection();
    }

    /**
     * @see ObjectStore#getSequence
     */
    public int getSequence() {
        return os.getSequence();
    }
}
