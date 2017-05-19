package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.Util;

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

    private String sequenceType = "dna";
    private String classAttribute = "primaryIdentifier";
    private Organism org;
    private String className;
    private int storeCount = 0;
    private String dataSourceName = null;
    private DataSource dataSource = null;
    private String fastaTaxonId = null;
    private Map<Integer, String> orgNames = new HashMap<Integer, String>(); // taxonid -> org name
    private Map<String, Integer> taxonIds = new HashMap<String, Integer>(); // org name -> taxonid
    protected String PIDPrefix = "";

    /**
     * Append this suffix to the identifier of the BioEnitys that are stored.
     */
    private String idSuffix = "";

    //Set this if we want to do some testing...
    private File[] files = null;

    private String dataSetTitle;

    private Map<String, DataSet> dataSets = new HashMap<String, DataSet>();

    /**
     * Set the Taxon Id of the Organism we are loading.  Can be space delimited list of taxonIds
     * @param fastaTaxonId the taxon id to set.
     */
    public void setFastaTaxonId(String fastaTaxonId) {
        this.fastaTaxonId = fastaTaxonId;
        parseTaxonIds();
    }
    
    /**
     * Set the sequence type to be passed to the FASTA parser.  The default is "dna".
     * @param sequenceType the sequence type
     */
    public void setSequenceType(String sequenceType) {
        if ("${fasta.sequenceType}".equals(sequenceType)) {
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
     * "org.intermine.model.bio.LocatedSequenceFeature" or "org.intermine.model.bio.Protein"
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
     * Datasource for any bioentities created
     * @param dataSourceName name of datasource for items created
     */
    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
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

    public void setPIDPrefix( String prefix ){
    	System.out.println("PrimaryIdentifier prefix set to "+prefix);
    	this.PIDPrefix = prefix;
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
    public void execute() {
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
     *
     * @param file the File to process.
     * @throws BuildException if the is a problem
     */
    @Override
    public void processFile(File file) {
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
                Sequence bioJavaSequence = iter.nextSequence();
                processSequence(getOrganism(bioJavaSequence), bioJavaSequence);
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
     * @param bioJavaSequence the biojava sequence to be parsed
     * @throws ObjectStoreException if there is a problem
     * @return the new Organism
     */
    protected Organism getOrganism(Sequence bioJavaSequence) throws ObjectStoreException {
        if (org == null) {
        	org = getDirectDataLoader().createObject(Organism.class);
        	Integer taxonId = new Integer(fastaTaxonId);
            org.setTaxonId(taxonId);
            org.setName(getName(taxonId));
            
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
        // some fasta files are not filtered - they contain sequences from organisms not
        // specified in project.xml
        if (organism == null) {
            return;
        }
        org.intermine.model.bio.Sequence flymineSequence = getDirectDataLoader().createObject(
                org.intermine.model.bio.Sequence.class);

        String sequence = bioJavaSequence.seqString();
        String md5checksum = Util.getMd5checksum(sequence);
        flymineSequence.setResidues(new PendingClob(sequence));
        flymineSequence.setLength(bioJavaSequence.length());
        flymineSequence.setMd5checksum(md5checksum);
        Class<? extends InterMineObject> imClass;
        Class<?> c;
        try {
            c = Class.forName(className);
            if (InterMineObject.class.isAssignableFrom(c)) {
                imClass = (Class<? extends InterMineObject>) c;
            } else {
                throw new RuntimeException("Feature className must be a valid class in the model"
                        + " that inherits from InterMineObject, but was: " + className);
            }
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException("unknown class: " + className
                                       + " while creating new Sequence object");
        }
        BioEntity imo = (BioEntity) getDirectDataLoader().createObject(imClass);

        String attributeValue = getIdentifier(bioJavaSequence);

        try {
            imo.setFieldValue(classAttribute, attributeValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error setting: " + className + "."
                                               + classAttribute + " to: " + attributeValue
                                               + ". Does the attribute exist?");
        }
        try {
            imo.setFieldValue("sequence", flymineSequence);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error setting: " + className + ".sequence to: "
                    + attributeValue + ". Does the attribute exist?");
        }
        imo.setOrganism(organism);
        try {
            imo.setFieldValue("length", new Integer(flymineSequence.getLength()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error setting: " + className + ".length to: "
                    + flymineSequence.getLength() + ". Does the attribute exist?");
        }

        try {
            imo.setFieldValue("md5checksum", md5checksum);
        } catch (Exception e) {
            // Ignore - we don't care if the field doesn't exist.
        }

        extraProcessing(bioJavaSequence, flymineSequence, imo, organism, getDataSet());

        if (StringUtils.isEmpty(dataSetTitle)) {
            throw new RuntimeException("DataSet title (fasta.dataSetTitle) not set");
        }

        DataSet dataSet = getDataSet();
        imo.addDataSets(dataSet);

        try {
            getDirectDataLoader().store(flymineSequence);
            getDirectDataLoader().store(imo);
            storeCount += 2;
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
        }
        DataSet dataSet = getDirectDataLoader().createObject(DataSet.class);
        dataSet.setName(dataSetTitle);
        if (dataSourceName != null) {
            dataSet.setDataSource(getDataSource());
        }
        getDirectDataLoader().store(dataSet);
        dataSets.put(dataSetTitle, dataSet);
        return dataSet;
    }


    /**
     * Do any extra processing needed for this record (extra attributes, objects, references etc.)
     * This method is called before the new objects are stored
     * @param bioJavaSequence the BioJava Sequence
     * @param flymineSequence the FlyMine Sequence
     * @param bioEntity the object that references the flymineSequence
     * @param organism the Organism object for the new InterMineObject
     * @param dataSet the DataSet object
     * @throws ObjectStoreException if a store() fails during processing
     */
    protected void  extraProcessing(Sequence bioJavaSequence, org.intermine.model.bio.Sequence
            flymineSequence, BioEntity bioEntity, Organism organism, DataSet dataSet)
        throws ObjectStoreException {
        // default - no extra processing
    }

    /**
     * For the given BioJava Sequence object, return an identifier to be used when creating
     * the corresponding BioEntity.
     * if | is present the middle bit is returned, eg sp|Q9V8R9-2|41_DROME
     * @param bioJavaSequence the Sequenece
     * @return an identifier
     */
    protected String getIdentifier(Sequence bioJavaSequence) {
        String name = bioJavaSequence.getName() + idSuffix;
        // description_line=sp|Q9V8R9-2|41_DROME
        if (name.contains("|")) {
            String[] bits = name.split("\\|");
            if (bits.length < 2) {
                return null;
            }
            name = bits[1];
        }
        return name;
    }

    private DataSource getDataSource() throws ObjectStoreException {
        if (StringUtils.isEmpty(dataSourceName)) {
            throw new RuntimeException("dataSourceName not set");
        }
        if (dataSource == null) {
            dataSource = getDirectDataLoader().createObject(DataSource.class);
            dataSource.setName(dataSourceName);
            getDirectDataLoader().store(dataSource);
            storeCount += 1;
        }
        return dataSource;
    }

    /**
     * @param name eg. Drosophila melanogaster
     * @return the taxonId
     */
    protected Integer getTaxonId(String name) {
        return taxonIds.get(name);
    }

    /**
     * Returns organism name associated with taxon id
     */
    protected String getName(Integer taxonId){
    	return orgNames.get(taxonId);
    }
    
    /**
     * some fasta files use organism name instead of taxonId, so we need a lookup map
     * taxons are taken from project.xml.  any entries in the fasta files from organisms not in
     * this list will be ignored
     */
    private void parseTaxonIds() {
        OrganismRepository repo = OrganismRepository.getOrganismRepository();
        String[] fastaTaxonIds = fastaTaxonId.split(" ");
        for (String taxonIdStr : fastaTaxonIds) {
            Integer taxonId = null;
            try {
                taxonId = Integer.valueOf(taxonIdStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("invalid taxonId: " + taxonIdStr);
            }
            OrganismData organismData = repo.getOrganismDataByTaxonInternal(taxonId.intValue());
            String name = organismData.getGenus() + " " + organismData.getSpecies();
            taxonIds.put(name, taxonId);
            orgNames.put(taxonId, name);
        }
    }
}

