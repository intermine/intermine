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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
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
     * @param os the underlying ObjectStore
     * @param translator the Translator used to translate queries and objects
     */
    public ObjectStoreTranslatingImpl(ObjectStore os, Translator translator) {
        super(os.getModel());
        this.os = os;
        this.translator = translator;
    }
    
    /**
     * @see ObjectStore#execute(Query, int, int, boolean)
     */
    public List execute(Query q, int start, int limit, boolean optimise)
    throws ObjectStoreException {
        checkStartLimit(start, limit);
        
        ResultsInfo estimate = estimate(q);
        if (estimate.getComplete() > maxTime) {
            throw new ObjectStoreException("Estimated time to run query ("
                                           + estimate.getComplete()
                                           + ") greater than permitted maximum ("
                                           + maxTime + ")");
        }
        
        Query q2 = translator.translateQuery(q);
        List results = new ArrayList();
        Iterator resIter = os.execute(q2, start, limit, optimise).iterator();
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
    public int count(Query q) throws ObjectStoreException {
        return os.count(translator.translateQuery(q));
    }
    
    /**
     * @see ObjectStore#getObjectByExample
     */
    public FlyMineBusinessObject getObjectByExample(FlyMineBusinessObject o, Set fieldNames)
        throws ObjectStoreException {
        throw new UnsupportedOperationException("getObjectByExample not supported by"
        + "ObjectStoreTranslatingImpl");
    }
}
