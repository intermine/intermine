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

    protected String type, alias;

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
                LOG.info("Starting CalculateLocations.createSpanningLocations()");
                cl.createSpanningLocations(Transcript.class, Exon.class, "exons");
                cl.createSpanningLocations(Gene.class, Transcript.class, "transcripts");
                LOG.info("Finished calculate-locations");
            } else if ("create-references".equals(type)) {
                CreateReferences cr = new CreateReferences(osw);
                LOG.info("Starting CreateReferences.insertReferences()");
                cr.insertReferences();
                LOG.info("Finsihed create-references");
            } else if ("transfer-sequences".equals(type)) {
                TransferSequences ts = new TransferSequences(osw);
                LOG.info("Starting TransferSequences.transferToChromosome()");
                ts.transferToChromosome();
                LOG.info("Skipping TransferSequences.transferToLocatedSequenceFeatures()");
                LOG.info("Skipping TransferSequences.transferToTranscripts()");
//                 LOG.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
//                 ts.transferToLocatedSequenceFeatures();
//                 LOG.info("Starting TransferSequences.transferToTranscripts()");
//                 ts.transferToTranscripts();
//                 LOG.info("Finished transfer-sequences");
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
