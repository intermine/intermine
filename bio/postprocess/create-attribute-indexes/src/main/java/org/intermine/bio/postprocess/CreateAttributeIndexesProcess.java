package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.task.CreateIndexesTask;

import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;


/**
 * Create indexes on a database holding objects conforming to a given model by
 * reading that model's primary key configuration information.
 * By default three types of index are created: for the specified primary key fields, for all N-1
 * relations, and for the indirection table columns of M-N relations.
 * Alternatively, if attributeIndexes is true, indexes are created for all non-primary key
 * attributes instead.
 * Note that all "id" columns are indexed automatically by virtue of InterMineTorqueModelOuput
 * specifying them as primary key columns.
 *
 * @author Mark Woodbridge
 * @author Kim Rutherford
 */
public class CreateAttributeIndexesProcess extends PostProcessor
{

    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public CreateAttributeIndexesProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     *
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess() throws ObjectStoreException {
        CreateIndexesTask cit = new CreateIndexesTask();
        cit.setAttributeIndexes(true);
        cit.setObjectStore(osw.getObjectStore());
        cit.execute();
    }
}
