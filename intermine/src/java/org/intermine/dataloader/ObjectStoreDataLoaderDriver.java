package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.datatracking.Source;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;

import org.apache.tools.ant.BuildException;

/**
 * Class that actually loads ObjectStore data
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class ObjectStoreDataLoaderDriver
{
    /**
     * Load ObjectStore data from a file
     *
     * @param iwAlias the name of the IntegrationWriter to use
     * @param sourceAlias the ObjectStore from which to read
     * @param sourceName the name of the data source, as used by primary key priority config
     * @param ignoreDuplicates tell the IntegrationWriter whether to ignore duplicate objects
     * @throws BuildException if any error occurs
     */
    public void loadData(String iwAlias, String sourceAlias, String sourceName,
                         boolean ignoreDuplicates)
        throws BuildException {

        try {
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(iwAlias);
            iw.setIgnoreDuplicates(ignoreDuplicates);
            ObjectStore os = ObjectStoreFactory.getObjectStore(sourceAlias);
            ObjectStoreDataLoader dl = new ObjectStoreDataLoader(iw);
            Source source = iw.getMainSource(sourceName);
            Source skelSource = iw.getSkeletonSource(sourceName);
            dl.process(os, source, skelSource);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

