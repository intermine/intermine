package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.dataloader.BatchingFetcher;
import org.intermine.dataloader.DirectDataLoader;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.dataloader.IntegrationWriterAbstractImpl;
import org.intermine.dataloader.IntegrationWriterDataTrackingImpl;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.dataloader.ParallelBatchingFetcher;
import org.intermine.dataloader.Source;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;

/**
 * This task uses a DirectDataLoader to create objects and store them directly into an ObjectStore
 * using an IntegrationWriter.
 *
 * @author Kim Rutherford
 */

public abstract class DirectDataLoaderTask extends Task
{
    private String integrationWriterAlias;
    protected String sourceName;
    private String sourceType;
    private boolean ignoreDuplicates = false;
    private DirectDataLoader directDataLoader;
    private IntegrationWriter iw;

    private static final Logger LOG = Logger.getLogger(DirectDataLoaderTask.class);

    /**
     * Set the IntegrationWriter.
     *
     * @param integrationWriterAlias the name of the IntegrationWriter
     */
    public void setIntegrationWriterAlias(String integrationWriterAlias) {
        this.integrationWriterAlias = integrationWriterAlias;
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
     * Set the source type, as used by primary key priority config.
     *
     * @param sourceType the type of the data source
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Set the value of ignoreDuplicates for the IntegrationWriter
     *
     * @param ignoreDuplicates the value of ignoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
    }


    /**
     * Return the IntegrationWriter for this task.
     * @return the IntegrationWriter
     * @throws ObjectStoreException  if anything goes wrong
     */
    protected IntegrationWriter getIntegrationWriter() throws ObjectStoreException {
        if (iw == null) {
            if (integrationWriterAlias == null) {
                throw new RuntimeException("integrationWriterAlias property is null while "
                                           + "getting IntegrationWriter");
            }


            iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriterAlias);

            Source source = iw.getMainSource(sourceName, sourceType);
            if (iw instanceof IntegrationWriterDataTrackingImpl) {
                Properties props = PropertiesUtil.getPropertiesStartingWith(
                        "equivalentObjectFetcher");
                if (!("false".equals(props.getProperty("equivalentObjectFetcher.useParallel")))) {
                    LOG.info("Using ParallelBatchingFetcher - set the property "
                            + "\"equivalentObjectFetcher.useParallel\" to false to use the standard"
                            + " BatchingFetcher");
                    ParallelBatchingFetcher eof =
                            new ParallelBatchingFetcher(((IntegrationWriterAbstractImpl)
                                    getIntegrationWriter()).getBaseEof(),
                                    ((IntegrationWriterDataTrackingImpl) getIntegrationWriter())
                                    .getDataTracker(), source);
                    ((IntegrationWriterAbstractImpl) getIntegrationWriter()).setEof(eof);
                } else {
                    LOG.info("Using BatchingFetcher - set the property "
                            + "\"equivalentObjectFetcher.useParallel\" to true to use the "
                            + "ParallelBatchingFetcher");
                    BatchingFetcher eof =
                            new BatchingFetcher(((IntegrationWriterAbstractImpl)
                                    getIntegrationWriter()).getBaseEof(),
                                    ((IntegrationWriterDataTrackingImpl) getIntegrationWriter())
                                    .getDataTracker(), source);
                    ((IntegrationWriterAbstractImpl) getIntegrationWriter()).setEof(eof);
                }
            }
        }
        return iw;
    }

    /**
     * Return the DirectDataLoader for this Task.  Must be called only after execute() has been
     * called.
     * @return the DirectDataLoader
     * @throws ObjectStoreException if there is an ObjectStore problem when creating the
     * DirectDataLoader
     */
    public DirectDataLoader getDirectDataLoader() throws ObjectStoreException {
        if (directDataLoader == null) {
            directDataLoader = new DirectDataLoader(getIntegrationWriter(), sourceName, sourceType);
        }
        return directDataLoader;
    }

    /**
     * Called by execute() to process the data.  This implementation should call
     * DirectDataLoader.createObject() and then DirectDataLoader.store() while processing.
     */
    public abstract void process();

    /**
     * @throws BuildException if an ObjectStore method fails
     */
    public void execute() {

        if (integrationWriterAlias == null) {
            throw new BuildException("DirectLoaderTask - integrationWriterAlias property not set");
        }

        if (sourceName == null) {
            throw new BuildException("DirectLoaderTask - sourceName property not set");
        }

        if (sourceType == null) {
            throw new BuildException("DirectLoaderTask - sourceType property not set");
        }

        try {
            getIntegrationWriter().beginTransaction();
            getIntegrationWriter().setIgnoreDuplicates(ignoreDuplicates);

            process();
            directDataLoader.close();

            getIntegrationWriter().commitTransaction();
            getIntegrationWriter().close();
            directDataLoader.close();
        } catch (ObjectStoreException e) {
            throw new BuildException(e);
        }

    }

}
