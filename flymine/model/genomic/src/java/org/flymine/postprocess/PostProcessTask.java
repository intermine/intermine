package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.intermine.dataloader.IntegrationWriter;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Transcript;
import org.flymine.model.genomic.Exon;

import org.apache.log4j.Logger;

/**
 * Run operations on genomic model database after DataLoading
 *
 * @author Richard Smith
 */
public class PostProcessTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PostProcessTask.class);

    protected String type, alias, integrationWriter;

    /**
     * Set the ObjectStoreWriter alias
     * @param alias name of the ObjectStoreWriter
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the type of post process operation.  Possible types:
     * @param type the type of post process operation
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set the alias of the integration writer, if necessary
     * @param integrationWriter the alias
     */
    public void setIntegrationWriter(String integrationWriter) {
        this.integrationWriter = integrationWriter;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (type == null) {
            throw new BuildException("type attribute is not set");
        }
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        ObjectStoreWriter osw = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(alias);
            if ("calculate-locations".equals(type)) {
                CalculateLocations cl = new CalculateLocations(osw);
                 LOG.info("Starting CalculateLocations.fixPartials()");
                 cl.fixPartials();
                 LOG.info("Starting CalculateLocations.createLocations()");
                 cl.createLocations();
            } else if ("create-references".equals(type)) {
                CreateReferences cr = new CreateReferences(osw);
                LOG.info("Starting CreateReferences.insertReferences()");
                cr.insertReferences();
                LOG.info("Finsihed create-references");
            } else if ("transfer-sequences".equals(type)) {
                TransferSequences ts = new TransferSequences(osw);
                LOG.info("Starting TransferSequences.transferToChromosome()");
                ts.transferToChromosome();
                LOG.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
                ts.transferToLocatedSequenceFeatures();
                LOG.info("Starting TransferSequences.transferToTranscripts()");
                ts.transferToTranscripts();
                LOG.info("Finished transfer-sequences");
            } else if ("make-spanning-locations".equals(type)) {
                CalculateLocations cl = new CalculateLocations(osw);
                LOG.info("Starting CalculateLocations.createSpanningLocations()");
                cl.createSpanningLocations(Transcript.class, Exon.class, "exons");
                cl.createSpanningLocations(Gene.class, Transcript.class, "transcripts");
                LOG.info("Finished calculate-locations");
            } else if ("update-publications".equals(type)) {
                if (integrationWriter == null) {
                    throw new BuildException("integrationWriter attribute is not set");
                }
                IntegrationWriter iw =
                    IntegrationWriterFactory.getIntegrationWriter("integration.production");
                iw.setIgnoreDuplicates(true);
                LOG.info("Starting update-publications");
                new UpdatePublications(iw).execute();
                LOG.info("Finished update-publications");
            } else if ("add-licences".equals(type)) {
                LOG.info("Starting add-licences");
                new AddLicences(osw).execute();
                LOG.info("Finished add-licences");
            } else {
                throw new BuildException("unknown type: " + type);
            }
            osw.close();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        } finally {
            osw.close();
        }
    }
}
