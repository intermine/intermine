package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Transcript;

import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * Run operations on genomic model database after DataLoading
 *
 * @author Richard Smith
 */
public class PostProcessTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PostProcessTask.class);

    protected String operation, objectStore, objectStoreWriter, ensemblDb;
    protected File outputFile;
    protected ObjectStoreWriter osw;

    /**
     * Sets the value of operation
     *
     * @param operation the operation to perform eg. 'Download publications'
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * Sets the value of objectStore
     *
     * @param objectStore an objectStore alias for operations that require one
     */
    public void setObjectStore(String objectStore) {
        this.objectStore = objectStore;
    }

    /**
     * Sets the value of objectStoreWriter
     *
     * @param objectStoreWriter an objectStoreWriter alias for operations that require one
     */
    public void setObjectStoreWriter(String objectStoreWriter) {
        this.objectStoreWriter = objectStoreWriter;
    }

    /**
     * Sets the value of outputFile
     *
     * @param outputFile an output file for operations that require one
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Sets the value of ensemblDb
     *
     * @param ensemblDb a database alias
     */
    public void setEnsemblDb(String ensemblDb) {
        this.ensemblDb = ensemblDb;
    }

    private ObjectStoreWriter getObjectStoreWriter() throws Exception {
        if (objectStoreWriter == null) {
            throw new BuildException("objectStoreWriter attribute is not set");
        }
        if (osw == null) {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(objectStoreWriter);
        }
        return osw;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (operation == null) {
            throw new BuildException("operation attribute is not set");
        }
        try {
            if ("calculate-locations".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOG.info("Starting CalculateLocations.fixPartials()");
                cl.fixPartials();
                LOG.info("Starting CalculateLocations.createLocations()");
                cl.createLocations();
            } else if ("create-chromosome-locations-and-lengths".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOG.info("Starting CalculateLocations.setChromosomeLocationsAndLengths()");
                cl.setChromosomeLocationsAndLengths();
            } else if ("create-oligo-locations".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOG.info("Starting CalculateLocations.createOligoLocations()");
                cl.createOligoLocations();
            } else if ("create-references".equals(operation)) {
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOG.info("Starting CreateReferences.insertReferences()");
                cr.insertReferences();
            } else if ("create-symmetrical-relation-references".equals(operation)) {
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOG.info("Starting CreateReferences.insertSymmetricalRelationReferences()");
                cr.insertSymmetricalRelationReferences();
            } else if ("fetch-contig-sequences-human".equals(operation)) {
                if (ensemblDb == null) {
                    throw new BuildException("ensemblDb attribute is not set");
                }
                Database db = DatabaseFactory.getDatabase(ensemblDb);
                StoreSequences ss = new StoreSequences(getObjectStoreWriter(), db);
                LOG.info("Starting StoreSequences.storeContigSequences()");
                ss.storeContigSequences();
            } else if ("transfer-sequences-chromosome".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToChromosome()");
                ts.transferToChromosome();
            } else if ("transfer-sequences".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToChromosome()");
                ts.transferToChromosome();
                LOG.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
                ts.transferToLocatedSequenceFeatures();
                LOG.info("Starting TransferSequences.transferToTranscripts()");
                ts.transferToTranscripts();
            } else if ("make-spanning-locations".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOG.info("Starting CalculateLocations.createSpanningLocations()");
                cl.createSpanningLocations(Transcript.class, Exon.class, "exons");
                cl.createSpanningLocations(Gene.class, Transcript.class, "transcripts");
            } else if ("update-publications".equals(operation)) {
                if (objectStore == null) {
                    throw new BuildException("objectStore attribute is not set");
                }
                if (outputFile == null) {
                    throw new BuildException("outputFile attribute is not set");
                }
                LOG.info("Starting update-publications");
                Writer writer = new BufferedWriter(new FileWriter(outputFile));
                new UpdatePublications(ObjectStoreFactory.getObjectStore(objectStore), writer)
                    .execute();
                writer.close();
            } else if ("add-licences".equals(operation)) {
                LOG.info("Starting add-licences");
                new AddLicences(getObjectStoreWriter()).execute();
            } else if ("update-orthologues".equals(operation)) {
                UpdateOrthologues uo = new UpdateOrthologues(getObjectStoreWriter());
                LOG.info("Starting UpdateOrthologues.process()");
                uo.process();
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOG.info("Starting CreateReferences.populateOrthologuesCollection()");
                cr.populateOrthologuesCollection();
            } else if ("homophila-post-process".equals(operation)) {
                HomophilaPostProcess hpp = new HomophilaPostProcess(getObjectStoreWriter());
                hpp.connectDrosophilaGenesToHumanDiseases();
            } else if ("set-collection-counts".equals(operation)) {
                SetCollectionCounts setCounts = new SetCollectionCounts(getObjectStoreWriter());
                setCounts.setCollectionCount();
            } else {
                throw new BuildException("unknown operation: " + operation);
            }
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
    }
}
