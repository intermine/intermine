package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.task.CreateIndexesTask;
import org.intermine.task.DynamicAttributeTask;
import org.intermine.task.PrecomputeTask;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.autocompletion.AutoCompleter;

import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Transcript;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

/**
 * Run operations on genomic model database after DataLoading
 *
 * @author Richard Smith
 */
public class PostProcessOperationsTask extends DynamicAttributeTask
{
    private static final Logger LOG = Logger.getLogger(PostProcessOperationsTask.class);

    protected String operation, objectStoreWriter, ensemblDb, organisms = null;
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
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (operation == null) {
            throw new BuildException("operation attribute is not set");
        }
        long startTime = System.currentTimeMillis();
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
                configureDynamicAttributes(iu);
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
            } else if ("set-collection-counts".equals(operation)) {
                SetCollectionCounts setCounts = new SetCollectionCounts(getObjectStoreWriter());
                setCounts.setCollectionCount();

            } else if ("synonym-update".equals(operation)) {
                SynonymUpdater synonymUpdater = new SynonymUpdater(getObjectStoreWriter());
                synonymUpdater.update();
            } else if ("create-attribute-indexes".equals(operation)) {
                CreateIndexesTask cit = new CreateIndexesTask();
                cit.setAttributeIndexes(true);
                cit.setObjectStore(getObjectStoreWriter().getObjectStore());
                cit.execute();
            } else if ("summarise-objectstore".equals(operation)) {
                System.out .println("summarising objectstore ...");
                ObjectStore os = getObjectStoreWriter().getObjectStore();
                if (!(os instanceof ObjectStoreInterMineImpl)) {
                    throw new RuntimeException("cannot summarise ObjectStore - must be an "
                                               + "instance of ObjectStoreInterMineImpl");
                }
                String configFileName = "objectstoresummary.config.properties";
                ClassLoader classLoader = PostProcessOperationsTask.class.getClassLoader();
                InputStream configStream =
                    classLoader.getResourceAsStream(configFileName);
                if (configStream == null) {
                    throw new RuntimeException("can't find resource: " + configFileName);
                }
                Properties config = new Properties();
                config.load(configStream);
                ObjectStoreSummary oss = new ObjectStoreSummary(os, config);
                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
                MetadataManager.store(db, MetadataManager.OS_SUMMARY,
                                      PropertiesUtil.serialize(oss.toProperties()));
            } else if ("precompute-queries".equals(operation)) {
                PrecomputeTask.precompute(false, getObjectStoreWriter().getObjectStore(), 0);
            } else if ("create-lucene-index".equals(operation) || "create-autocomplete-index".equals(operation)) {
                System.out .println("create lucene index ...");
                ObjectStore os = getObjectStoreWriter().getObjectStore();
                if (!(os instanceof ObjectStoreInterMineImpl)) {
                    throw new RuntimeException("cannot summarise ObjectStore - must be an "
                                   + "instance of ObjectStoreInterMineImpl (create lucene index)");
                }
                String configFileName = "objectstoresummary.config.properties";
                ClassLoader classLoader = PostProcessOperationsTask.class.getClassLoader();
                InputStream configStream =
                    classLoader.getResourceAsStream(configFileName);
                if (configStream == null) {
                    throw new RuntimeException("can't find resource: " + configFileName);
                }

                Properties properties = new Properties();
                properties.load(configStream);

                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();

                AutoCompleter ac = new AutoCompleter(os, properties);
                if (ac.getBinaryIndexMap() != null) {
                    MetadataManager.storeBinary(db, MetadataManager.AUTOCOMPLETE_INDEX,
                                        ac.getBinaryIndexMap());
                }
            } else {
                throw new BuildException("unknown operation: " + operation);
            }
            LOG.info("PP - " + operation + " took "
                     + (System.currentTimeMillis() - startTime) + " ms.");
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
