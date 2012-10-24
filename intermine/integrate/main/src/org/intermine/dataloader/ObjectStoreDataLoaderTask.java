package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.model.FastPathObject;
import org.intermine.objectstore.ObjectStoreFactory;

/**
 * Uses an IntegrationWriter to load data from another ObjectStore.
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class ObjectStoreDataLoaderTask extends Task
{
    protected String integrationWriter;
    protected String source;
    protected String sourceName;
    protected String sourceType;
    protected boolean ignoreDuplicates;
    protected String queryClass = null;
    protected String allSources;

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
     * Set the source type, as used by primary key priority config.
     *
     * @param sourceType the name of the data source
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Set the value of ignoreDuplicates for the IntegrationWriter.
     *
     * @param ignoreDuplicates the value of ignoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
    }

    /**
     * If the name of a class is set will only load objects of that type.
     *
     * @param queryClass the name of a class to load
     */
    public void setQueryClass(String queryClass) {
        this.queryClass = queryClass;
    }

    /**
     * Set the list of data sources present in the project.xml, for the purposes of verifying the
     * priorities properties file.
     *
     * @param allSources a space-separated list of source names
     */
    public void setAllSources(String allSources) {
        this.allSources = allSources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (integrationWriter == null) {
            throw new BuildException("integrationWriter attribute is not set");
        }
        if (source == null) {
            throw new BuildException("source attribute is not set");
        }

        try {
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriter);
            PriorityConfig.verify(iw.getModel(), allSources);
            iw.setIgnoreDuplicates(ignoreDuplicates);
            if (queryClass != null) {
                Class<?> tmpQueryClass = Class.forName(queryClass);
                if (!FastPathObject.class.isAssignableFrom(tmpQueryClass)) {
                    throw new ClassCastException("Class " + queryClass + " is not a subclass of "
                            + "FastPathObject");
                }
                @SuppressWarnings("unchecked") Class<? extends FastPathObject> tmp2QueryClass =
                    (Class) tmpQueryClass;
                new ObjectStoreDataLoader(iw).process(ObjectStoreFactory.getObjectStore(source),
                        iw.getMainSource(sourceName, sourceType), iw.getSkeletonSource(sourceName,
                                sourceType), tmp2QueryClass);

            } else {
                new ObjectStoreDataLoader(iw).process(ObjectStoreFactory.getObjectStore(source),
                                                      iw.getMainSource(sourceName, sourceType),
                                                      iw.getSkeletonSource(sourceName, sourceType));
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}

