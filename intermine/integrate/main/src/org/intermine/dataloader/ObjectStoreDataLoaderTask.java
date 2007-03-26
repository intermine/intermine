package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.objectstore.ObjectStoreFactory;

/**
 * Uses an IntegrationWriter to load data from another ObjectStore
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class ObjectStoreDataLoaderTask extends Task
{
    protected String integrationWriter;
    protected String source;
    protected String sourceName;
    protected boolean ignoreDuplicates;
    protected String queryClass = null;

    /**
     * Set the IntegrationWriter.
     *
     * @param integrationWriter the name of the IntegrationWriter
     */
    public void setIntegrationWriter(String integrationWriter) {
        this.integrationWriter = integrationWriter;
    }

    /**
     * Set the ObjectStore that is the data source.
     *
     * @param source the name of the objectstore that is the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Set the source name, as used by primary key priority config.
     *
     * @param sourceName the name of the data source
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Set the value of ignoreDuplicates for the IntegrationWriter
     * @param ignoreDuplicates the value of ignoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
    }

    /**
     * If the name of a class is set will only load objects of that type
     * @param queryClass the name of a class to load
     */
    public void setQueryClass(String queryClass) {
        this.queryClass = queryClass;
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (integrationWriter == null) {
            throw new BuildException("integrationWriter attribute is not set");
        }
        if (source == null) {
            throw new BuildException("source attribute is not set");
        }

        try {
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriter);
            iw.setIgnoreDuplicates(ignoreDuplicates);
            if (queryClass != null) {
                new ObjectStoreDataLoader(iw).process(ObjectStoreFactory.getObjectStore(source),
                                                      iw.getMainSource(sourceName),
                                                      iw.getSkeletonSource(sourceName),
                                                      Class.forName(queryClass));

            } else {
                new ObjectStoreDataLoader(iw).process(ObjectStoreFactory.getObjectStore(source),
                                                      iw.getMainSource(sourceName),
                                                      iw.getSkeletonSource(sourceName));
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}

