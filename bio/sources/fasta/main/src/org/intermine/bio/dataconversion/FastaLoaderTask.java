package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Synonym;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;

/**
 * A task that can read a set of FASTA files and create the corresponding Sequence objects in an
 * ObjectStore.
 *
 * @author Kim Rutherford
 * @author Peter Mclaren
 */

public class FastaLoaderTask extends FileDirectDataLoaderTask
{
    private static final Logger LOG = Logger.getLogger(FastaLoaderTask.class);

    private Integer fastaTaxonId;
    private String sequenceType = "dna";
    private String classAttribute = "primaryIdentifier";
    private Organism org;
    private String className;
    private int storeCount = 0;
    private String synonymSource = null;
    private DataSource dataSource = null;

    /**
     * Append this suffix to the identifier of the BioEnitys that are stored.
     */
    private String idSuffix = "";

    //Set this if we want to do some testing...
    private File[] files = null;

    private String dataSetTitle;

    private Map<String, DataSet> dataSets = new HashMap<String, DataSet>();

    /**
     * Set the Taxon Id of the Organism we are loading.
     *
     * @param fastaTaxonId the taxon id to set.
     */
    public void setFastaTaxonId(Integer fastaTaxonId) {
        this.fastaTaxonId = fastaTaxonId;
    }

    /**
     * Set the sequence type to be passed to the FASTA parser.  The default is "dna".
     * @param sequenceType the sequence type
     */
    public void setSequenceType(String sequenceType) {
        if (sequenceType.equals("${fasta.sequenceType}")) {
            this.sequenceType = "dna";
        } else {
            this.sequenceType = sequenceType;
        }
    }

    /**
     * Set the suffix to add to identifiers from the FASTA file when creating
     * BioEnitys.
     * @param idSuffix the suffix
     */
    public void setIdSuffix(String idSuffix) {
        this.idSuffix = idSuffix;
    }

    /**
     * The class name to use for objects created during load.  Generally this is
     * "org.flymine.model.genomic.LocatedSequenceFeature" or "org.flymine.model.genomic.Protein"
     * @param className the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Return the class name set with setClassName().
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * The attribute of the class created to set with the identifying field.  If not set will
     * be 'primaryIdentifier'.
     * @param classAttribute the class name
     */
    public void setClassAttribute(String classAttribute) {
        this.classAttribute = classAttribute;
    }

    /**
     * If a value is specified a Synonym will be created for the feature with this attribute used
     * as the name of the DataSource.
     * @param synonymSource the name of the synonym DataSource
     */
    public void setSynonymSource(String synonymSource) {
        this.synonymSource = synonymSource;
    }

    /**
     * If a value is specified this title will used when a DataSet is created.
     * @param dataSetTitle the title of the DataSets of any new features
     */
    public void setDataSetTitle(String dataSetTitle) {
        this.dataSetTitle = dataSetTitle;
    }

    /**
     * Directly set the array of files to read from.  Use this for testing with junit.
     * @param files the File objects
     */
    protected void setFileArray(File[] files) {
        this.files = files;
    }

    /**
     * Process and load all of the fasta files.
     */
    @Override
    public void process() {
        long start = System.currentTimeMillis();
        try {
            storeCount++;
            super.process();
            getIntegrationWriter().commitTransaction();
            getIntegrationWriter().beginTransaction();
        } catch (ObjectStoreException e) {
            throw new BuildException("failed to store object", e);
        }
        long now = System.currentTimeMillis();
        LOG.info("Finished dataloading " + storeCount + " objects at " + ((60000L * storeCount)
                    / (now - start)) + " objects per minute (" + (now - start)
                + " ms total) for source " + sourceName);
    }

    /**
     * @throws BuildException if an ObjectStore method fails
     */
    @Override
    public void execute() throws BuildException {
        if (fastaTaxonId == null) {
            throw new RuntimeException("fastaTaxonId needs to be set");
        }
        if (className == null) {
            throw new RuntimeException("className needs to be set");
        }
        if (files != null) {
            // setFiles() is used only for testing
            for (int i = 0; i < files.length; i++) {
                processFile(files[i]);
            }
        } else {
            // this will call processFile() for each file
            super.execute();
        }
    }


    /**
     * Handles each fasta file. Factored out so we can supply files for testing.
     * @param file the File to process.
     * @throws BuildException if the is a problem
     */
    @Override
    public void processFile(File file) throws BuildException {
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);

            System.err .println("reading " + sequenceType + " sequence from: " + file);

            SequenceIterator iter =
                    (SequenceIterator) SeqIOTools.fileToBiojava("fasta", sequenceType, reader);

            if (!iter.hasNext()) {
                System.err .println("no fasta sequences found - exiting");
                return;
            }

            while (iter.hasNext()) {
                processSequence(getOrganism(), iter.nextSequence());
            }

            reader.close();
            fileReader.close();
        } catch (BioException e) {
            throw new BuildException("sequence not in fasta format or wrong alphabet for: "
                    + file, e);
        } catch (NoSuchElementException e) {
            throw new BuildException("no fasta sequences in: " + file, e);
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + file, e);
        } catch (ObjectStoreException e) {
            throw new BuildException("ObjectStore problem while processing: " + file, e);
        } catch (IOException e) {
            throw new BuildException("error while closing FileReader for: " + file, e);
        }
    }

    /**
     * Get and store() the Organism object to reference when creating new objects.
     * @throws ObjectStoreException if there is a problem
     * @return the new Organism
     */
    protected Organism getOrganism() throws ObjectStoreException {
        if (org == null) {
            org = (Organism) getDirectDataLoader().createObject(Organism.class);
            org.setTaxonId(fastaTaxonId);
            getDirectDataLoader().store(org);
        }
        return org;
    }

    /**
     * Create a FlyMine Sequence and an object of type className for the given BioJava Sequence.
     * @param organism the Organism to reference from new objects
     * @param bioJavaSequence the Sequence object
     * @throws ObjectStoreException if store() fails
     */
    private void processSequence(Organism organism, Sequence bioJavaSequence)
        throws ObjectStoreException {
        Class<?> sequenceClass = org.flymine.model.genomic.Sequence.class;
        org.flymine.model.genomic.Sequence flymineSequence =
            (org.flymine.model.genomic.Sequence) getDirectDataLoader().createObject(sequenceClass);

        flymineSequence.setResidues(bioJavaSequence.seqString());
        flymineSequence.setLength(bioJavaSequence.length());

        Class<?> c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException("unknown class: " + className
                                       + " while creating new Sequence object");
        }
        BioEntity imo = (BioEntity) getDirectDataLoader().createObject(c);

        String attributeValue = getIdentifier(bioJavaSequence);

        try {
            TypeUtil.setFieldValue(imo, classAttribute, attributeValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error setting: " + className + "."
                                               + classAttribute + " to: " + attributeValue
                                               + ". Does the attribute exist?");
        }
        TypeUtil.setFieldValue(imo, "sequence", flymineSequence);
        imo.setOrganism(organism);
        if (TypeUtil.getSetter(c, "length") != null) {
            TypeUtil.setFieldValue(imo, "length", new Integer(flymineSequence.getLength()));
        }

        extraProcessing(bioJavaSequence, flymineSequence, imo, organism, getDataSource());

        Synonym synonym = null;
        if (!StringUtils.isEmpty(synonymSource)) {
            synonym = (Synonym) getDirectDataLoader().createObject(Synonym.class);
            synonym.setValue(attributeValue);
            synonym.setType(classAttribute);
            synonym.setSubject(imo);
            synonym.setSource(getDataSource());
        }

        if (StringUtils.isEmpty(dataSetTitle)) {
            throw new RuntimeException("DataSet title (fasta.dataSetTitle) not set");
        }

        DataSet dataSet = getDataSet();
        imo.addDataSets(dataSet);
        if (synonym != null) {
            synonym.addDataSets(dataSet);
        }

        try {
            getDirectDataLoader().store(flymineSequence);
            getDirectDataLoader().store(imo);
            storeCount += 2;
            if (synonym != null) {
                getDirectDataLoader().store(synonym);
                storeCount += 1;
            }
        } catch (ObjectStoreException e) {
            throw new BuildException("store failed", e);
        }
    }

    /**
     * Return the DataSet to add to each object.
     * @return the DataSet
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    public DataSet getDataSet() throws ObjectStoreException {
        if (dataSets.containsKey(dataSetTitle)) {
            return dataSets.get(dataSetTitle);
        } else {
            DataSet dataSet = (DataSet) getDirectDataLoader().createObject(DataSet.class);
            dataSet.setTitle(dataSetTitle);
            getDirectDataLoader().store(dataSet);
            dataSets.put(dataSetTitle, dataSet);
            return dataSet;
        }
    }

    /**
     * Do any extra processing needed for this record (extra attributes, objects, references etc.)
     * This method is called before the new objects are stored
     * @param bioJavaSequence the BioJava Sequence
     * @param flymineSequence the FlyMine Sequence
     * @param interMineObject the object that references the flymineSequence
     * @param organism the Organism object for the new InterMineObject
     * @param dataSrc the DataSource object
     * @throws ObjectStoreException if a store() fails during processing
     */
    @SuppressWarnings("unused")
    protected void extraProcessing(Sequence bioJavaSequence,
                                   org.flymine.model.genomic.Sequence flymineSequence,
                                   BioEntity interMineObject, Organism organism,
                                   DataSource dataSrc)
        throws ObjectStoreException {
        // default - no extra processing
    }

    /**
     * For the given BioJava Sequence object, return an identifier to be used when creating
     * the corresponding BioEntity.
     * @param bioJavaSequence the Sequenece
     * @return an identifier
     */
    protected String getIdentifier(Sequence bioJavaSequence) {
        return bioJavaSequence.getName() + idSuffix;
    }

    private DataSource getDataSource() throws ObjectStoreException {
        if (StringUtils.isEmpty(synonymSource)) {
            throw new RuntimeException("synonymSource not set");
        }
        if (dataSource == null) {
            dataSource = (DataSource) getDirectDataLoader().createObject(DataSource.class);
            dataSource.setName(synonymSource);
            getDirectDataLoader().store(dataSource);
            storeCount += 1;
        }
        return dataSource;
    }
}

