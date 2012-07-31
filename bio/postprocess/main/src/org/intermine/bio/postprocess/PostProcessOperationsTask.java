package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;


import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.task.CreateIndexesTask;
import org.intermine.task.DynamicAttributeTask;
import org.intermine.task.PrecomputeTask;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.search.KeywordSearch;

/**
 * Run operations on genomic model database after DataLoading
 *
 * @author Richard Smith
 */
public class PostProcessOperationsTask extends DynamicAttributeTask
{
    private static final Logger LOGGER = Logger.getLogger(PostProcessOperationsTask.class);

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
    @Override
    public void execute() {
        if (operation == null) {
            throw new BuildException("operation attribute is not set");
        }
        long startTime = System.currentTimeMillis();
        try {
            if ("create-chromosome-locations-and-lengths".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOGGER.info("Starting CalculateLocations.setChromosomeLocationsAndLengths()");
                cl.setChromosomeLocationsAndLengths();
            } else if ("set-missing-chromosome-locations".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOGGER.info("Starting CalculateLocations.setMissingChromosomeLocations()");
                cl.setMissingChromosomeLocations();
            } else if ("create-references".equals(operation)) {
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOGGER.info("Starting CreateReferences.insertReferences()");
                cr.insertReferences();
            } else if ("create-symmetrical-relation-references".equals(operation)) {
                throw new BuildException("create-symmetrical-relation-references task is"
                        + " deprecated");
            } else if ("create-utr-references".equals(operation)) {
                CreateReferences cr = new CreateReferences(getObjectStoreWriter());
                LOGGER.info("Starting CreateReferences.createUtrRefs()");
                cr.createUtrRefs();
            } else if ("transfer-sequences".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                ts = new TransferSequences(getObjectStoreWriter());
                LOGGER.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
                ts.transferToLocatedSequenceFeatures();

                ts = new TransferSequences(getObjectStoreWriter());
                LOGGER.info("Starting TransferSequences.transferToTranscripts()");
                ts.transferToTranscripts();
            } else if ("transfer-sequences-located-sequence-feature".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOGGER.info("Starting TransferSequences.transferToLocatedSequenceFeatures()");
                ts.transferToLocatedSequenceFeatures();
            } else if ("transfer-sequences-transcripts".equals(operation)) {
                TransferSequences ts = new TransferSequences(getObjectStoreWriter());
                LOGGER.info("Starting TransferSequences.transferToTranscripts()");
                ts.transferToTranscripts();
            } else if ("make-spanning-locations".equals(operation)) {
                CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());
                LOGGER.info("Starting CalculateLocations.createSpanningLocations()");
                cl.createSpanningLocations("Transcript", "Exon", "exons");
                cl.createSpanningLocations("Gene", "Transcript", "transcripts");
            } else if ("create-intergenic-region-features".equals(operation)) {
                IntergenicRegionUtil ig = new IntergenicRegionUtil(getObjectStoreWriter());
                LOGGER.info("Starting IntergenicRegionUtil.createIntergenicRegionFeatures()");
                ig.createIntergenicRegionFeatures();
            } else if ("create-gene-flanking-features".equals(operation)) {
                CreateFlankingRegions cfr = new CreateFlankingRegions(getObjectStoreWriter());
                LOGGER.info("Starting CreateFlankingRegions.createFlankingFeatures()");
                cfr.createFlankingFeatures();
            } else if ("create-intron-features".equals(operation)) {
                IntronUtil iu = new IntronUtil(getObjectStoreWriter());
                configureDynamicAttributes(iu);
                LOGGER.info("Starting IntronUtil.createIntronFeatures()");
                iu.createIntronFeatures();
            } else if ("create-overlap-relations-flymine".equals(operation)) {
                LOGGER.info("Starting CalculateLocations.createOverlapRelations()");
                List<String> classNamesToIgnoreList = new ArrayList<String>();
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
                (new PrecomputeTask()).precompute(false, getObjectStoreWriter().getObjectStore(),
                        0);
            } else if ("create-lucene-index".equals(operation)
                       || "create-autocomplete-index".equals(operation)) {
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
            } else if ("create-search-index".equals(operation)) {
                System .out.println("Creating lucene index for keyword search...");

                ObjectStore os = getObjectStoreWriter().getObjectStore();
                if (!(os instanceof ObjectStoreInterMineImpl)) {
                    throw new RuntimeException("Got invalid ObjectStore - must be an "
                            + "instance of ObjectStoreInterMineImpl!");
                }

                ClassLoader classLoader = PostProcessOperationsTask.class.getClassLoader();

                /*
                String configFileName = "objectstoresummary.config.properties";
                InputStream configStream = classLoader.getResourceAsStream(configFileName);
                if (configStream == null) {
                    throw new RuntimeException("can't find resource: " + configFileName);
                }

                Properties properties = new Properties();
                properties.load(configStream);*/

                //read class keys to figure out what are keyFields during indexing
                InputStream is = classLoader.getResourceAsStream("class_keys.properties");
                Properties classKeyProperties = new Properties();
                classKeyProperties.load(is);
                Map<String, List<FieldDescriptor>> classKeys =
                    ClassKeyHelper.readKeys(os.getModel(), classKeyProperties);

                //index and save
                KeywordSearch.saveIndexToDatabase(os, classKeys);
                KeywordSearch.deleteIndexDirectory();
            } else if ("create-overlap-view".equals(operation)) {
                OverlapViewTask ovt = new OverlapViewTask(getObjectStoreWriter());
                ovt.createView();
            } else if ("create-bioseg-location-index".equals(operation)) {
                BiosegIndexTask bit = new BiosegIndexTask(getObjectStoreWriter());
                bit.createIndex();
            } else if ("link-ins".equals(operation)) {
                CreateFlyBaseLinkIns.createLinkInFile(getObjectStoreWriter().getObjectStore());
            } else if ("modmine-metadata-cache".equals(operation)) {
                CreateModMineMetaDataCache.createCache(getObjectStoreWriter().getObjectStore());
            } else {
                throw new BuildException("unknown operation: " + operation);
            }
            LOGGER.info("PP - " + operation + " took "
                     + (System.currentTimeMillis() - startTime) + " ms.");
        } catch (BuildException e) {
            LOGGER.error("Failed postprocess. Operation was: " + operation, e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed postprocess. Operation was: " + operation, e);
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
