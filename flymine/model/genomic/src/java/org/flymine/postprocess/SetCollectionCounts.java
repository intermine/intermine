package org.flymine.postprocess;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Transcript;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * SetCollectionCounts class
 *
 * @author Kim Rutherford
 */

public class SetCollectionCounts
{
    private ObjectStoreWriter osw;

    private ObjectStore os;
    
    /**
     * Construct with an ObjectStoreWriter, read and write from same ObjectStore
     * @param osw an ObjectStore to write to
     */
    public SetCollectionCounts(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

    /**
     * Set the count fields by looking at collection sizes (eg. set exonCount by getting 
     * exons.size()) 
     * @throws Exception
     */
    public void setCollectionCount() throws Exception {
        setCollectionCountField(Transcript.class, "exons", "exonCount");
    }

    void setCollectionCountField(Class c, String collectionName, String countFieldName)
        throws ObjectStoreException, IllegalAccessException {
        Query q = new Query();
        
        QueryClass qc = new QueryClass(c);
        q.addFrom(qc);
        q.addToSelect(qc);
        
        Results results = os.execute(q);
        
        Iterator resultsIter = results.iterator();
        
        // TODO XXX FIXME - this is very ineffecient, we should get all the collection size in one
        // query
        while (resultsIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resultsIter.next();
            
            InterMineObject o = (InterMineObject) rr.get(0);
            
            Collection collection = (Collection) TypeUtil.getFieldValue(o, collectionName);
            
            TypeUtil.setFieldValue(o, countFieldName, new Integer(collection.size()));
            
            osw.store(o);
        }
    }
}
