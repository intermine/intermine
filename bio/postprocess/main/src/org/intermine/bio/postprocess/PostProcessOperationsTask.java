package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Transcript;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.task.CreateIndexesTask;

/**
 * Run operations on genomic model database after DataLoading
 *
 * @author Richard Smith
 */
public class PostProcessOperationsTask extends Task
{
    private static final Logger LOG = Logger.getLogger(PostProcessOperationsTask.class);

    /**
     * The category to pass to ObjectStoreInterMineImpl.precompute().
     */
    public static final String PRECOMPUTE_CATEGORY = "precompute";

    protected String operation, objectStoreWriter, ensemblDb;
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
     * @see Task#execute()
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
            } else if ("create-references".equals(operation)) {
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOG.info("Starting CreateReferences.insertReferences()");
                cr.insertReferences();
            } else if ("create-symmetrical-relation-references".equals(operation)) {
                throw new BuildException("create-symmetrical-relation-references task is"
                        + " deprecated");
            } else if ("create-utr-references".equals(operation)) {
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOG.info("Starting CreateReferences.createUtrRefs()");
                cr.createUtrRefs();
            } else if ("fetch-ensembl-contig-sequences".equals(operation)) {
                if (ensemblDb == null) {
                    throw new BuildException("ensemblDb attribute is not set");
                }
                Database db = DatabaseFactory.getDatabase(ensemblDb);
                StoreSequences ss = new StoreSequences(getObjectStoreWriter(), db);
                LOG.info("Starting StoreSequences.storeContigSequences()");
                ss.storeContigSequences();
            } else if ("fetch-contig-sequences-ensembl".equals(operation)) {
                if (ensemblDb == null) {
                    throw new BuildException("ensemblDb attribute is not set");
                }
                Database db = DatabaseFactory.getDatabase(ensemblDb);
                StoreSequences ss = new StoreSequences(getObjectStoreWriter(), db);
                LOG.info("Starting StoreSequences.storeContigSequences() for ensemblDb:"
                        + ensemblDb);
                ss.storeContigSequences();
            } else if ("transfer-sequences".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToChromosome()");
                ts.transferToChromosome();

                ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
                ts.transferToLocatedSequenceFeatures();

                ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToTranscripts()");
                ts.transferToTranscripts();
            } else if ("transfer-sequences-chromosome".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToChromosome()");
                ts.transferToChromosome();
            } else if ("transfer-sequences-located-sequence-feature".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
                ts.transferToLocatedSequenceFeatures();
            } else if ("transfer-sequences-transcripts".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOG.info("Starting TransferSequences.transferToTranscripts()");
                ts.transferToTranscripts();
            } else if ("make-spanning-locations".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOG.info("Starting CalculateLocations.createSpanningLocations()");
                cl.createSpanningLocations(Transcript.class, Exon.class, "exons");
                cl.createSpanningLocations(Gene.class, Transcript.class, "transcripts");
            } else if ("create-intergenic-region-features".equals(operation)) {
                IntergenicRegionUtil ig = new IntergenicRegionUtil(getObjectStoreWriter());
                LOG.info("Starting IntergenicRegionUtil.createIntergenicRegionFeatures()");
                ig.createIntergenicRegionFeatures();
            } else if ("create-intron-features".equals(operation)) {
                IntronUtil iu = new IntronUtil(getObjectStoreWriter());
                LOG.info("Starting IntronUtil.createIntronFeatures()");
                iu.createIntronFeatures();
            } else if ("create-overlap-relations-flymine".equals(operation)) {
                LOG.info("Starting CalculateLocations.createOverlapRelations()");
                List classNamesToIgnoreList = new ArrayList();
                String ignoreFileName = "overlap.config";
                ClassLoader classLoader = PostProcessOperationsTask.class.getClassLoader();
                InputStream classesToIgnoreStream =
                    classLoader.getResourceAsStream(ignoreFileName);
                if (classesToIgnoreStream == null) {
                    throw new RuntimeException("can't find resource: " + ignoreFileName);
                }
                BufferedReader classesToIgnoreReader =
                    new BufferedReader(new InputStreamReader(classesToIgnoreStream));
                String line = classesToIgnoreReader.readLine();
                while (line != null) {
                    classNamesToIgnoreList.add(line);
                    line = classesToIgnoreReader.readLine();
                }

                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                cl.createOverlapRelations(classNamesToIgnoreList, false);
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
            } else if ("set-collection-counts".equals(operation)) {
                SetCollectionCounts setCounts = new SetCollectionCounts(getObjectStoreWriter());
                setCounts.setCollectionCount();

            } else if ("synonym-update".equals(operation)) {
                SynonymUpdater synonymUpdater = new SynonymUpdater(getObjectStoreWriter());
                synonymUpdater.update();
            } else if ("create-attribute-indexes".equals(operation)) {
                CreateIndexesTask cit = new CreateIndexesTask();
                cit.setObjectStore(getObjectStoreWriter().getObjectStore());
                cit.execute();
            } else {
                throw new BuildException("unknown operation: " + operation);
            }
        } catch (BuildException e) {
            LOG.error("Failed postprocess. Operation was: " + operation, e);
            throw e;
        } catch (Exception e) {
            LOG.error("Failed postprocess. Operation was: " + operation, e);
            throw new BuildException("Operation was:" + operation, e);
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
