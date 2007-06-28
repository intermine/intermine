package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataloader.DirectDataLoader;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.objectstore.ObjectStoreException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

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
    private boolean ignoreDuplicates = false;
    private DirectDataLoader directDataLoader;
    private IntegrationWriter iw;

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
     * @throws ObjectStoreException 
     */
    protected IntegrationWriter getIntegrationWriter() throws ObjectStoreException {
        if (iw == null) {
            if (integrationWriterAlias == null) {
                throw new RuntimeException("integrationWriterAlias property is null while "
                                           + "getting IntegrationWriter");
            } else {
                iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriterAlias);
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
            directDataLoader = new DirectDataLoader(getIntegrationWriter(), sourceName);
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
    public void execute() throws BuildException {

        if (integrationWriterAlias == null) {
            throw new BuildException("FastaLoaderTask - integrationWriterAlias property not set");
        }

        if (sourceName == null) {
            throw new BuildException("FastaLoaderTask - sourceName property not set");
        }

        try {
            getIntegrationWriter().beginTransaction();
            getIntegrationWriter().setIgnoreDuplicates(ignoreDuplicates);
            
            process();
            
            getIntegrationWriter().commitTransaction();
            getIntegrationWriter().close();
        } catch (ObjectStoreException e) {
            throw new BuildException(e);
        }

    }

}
