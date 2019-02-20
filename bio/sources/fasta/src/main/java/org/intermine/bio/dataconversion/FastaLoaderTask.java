package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.biojava.nbio.core.exceptions.ParserException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.biojava.nbio.core.sequence.io.PlainFastaHeaderParser;
import org.biojava.nbio.core.sequence.template.Sequence;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.task.FileDirectDataLoaderTask;


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
    private Map<String, String> taxonIds = new HashMap<String, String>();

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
    public void setTaxonId(String fastaTaxonId) {
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
            getDirectDataLoader().close();
        } catch (ObjectStoreException e) {
            throw new BuildException("failed to store object", e);
        }
        long now = System.currentTimeMillis();
        LOG.info("Finished dataloading " + storeCount + " objects at " + ((60000L * storeCount)
                / (now - start)) + " objects per minute (" + (now - start)
                + " ms total) for source " + sourceName);
    }

    /**
     * Be sure to close the data loader so the last batch gets stored. only needed for tests
     * since the data loading task usually does that for hte live builds.
     * @throws ObjectStoreException if we can't store to db
     */
    public void close() throws ObjectStoreException {
        // store any data left over
        getDirectDataLoader().close();
    }

    /**
     * @throws BuildException if an ObjectStore method fails
     */
    @Override
    public void execute() {
        // don't configure dynamic attributes if this is a unit test!
        if (getProject() != null) {
            configureDynamicAttributes(this);
        }
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
            System.err .println("reading " + sequenceType + " sequence from: " + file);
            LOG.debug("FastaLoaderTask loading file " + file.getName());
            if ("dna".equalsIgnoreCase(sequenceType)) {
                FastaReader<DNASequence, NucleotideCompound> aFastaReader
                        = new FastaReader<DNASequence, NucleotideCompound>(file,
                        new PlainFastaHeaderParser<DNASequence, NucleotideCompound>(),
                        new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()));

                LinkedHashMap<String, DNASequence> b = aFastaReader.process();
                for (Entry<String, DNASequence> entry : b.entrySet()) {
                    Sequence bioJavaSequence = entry.getValue();
                    processSequence(getOrganism(bioJavaSequence), bioJavaSequence);
                }
            } else {
                LinkedHashMap<String, ProteinSequence> b =
                        FastaReaderHelper.readFastaProteinSequence(file);
                for (Entry<String, ProteinSequence> entry : b.entrySet()) {
                    Sequence bioJavaSequence = entry.getValue();
                    processSequence(getOrganism((ProteinSequence) bioJavaSequence),
                            bioJavaSequence);
                }
            }
        } catch (ParserException e) {
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
        // some fasta files are not filtered - they contain sequences from organisms not
        // specified in project.xml
        if (organism == null) {
            return;
        }

        org.intermine.model.bio.Sequence flymineSequence = getDirectDataLoader().createObject(
                org.intermine.model.bio.Sequence.class);

        String sequence = bioJavaSequence.getSequenceAsString();
        String md5checksum = Util.getMd5checksum(sequence);

        flymineSequence.setResidues(new PendingClob(sequence));
        flymineSequence.setLength(bioJavaSequence.getLength());
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
        String name = bioJavaSequence.getAccession().getID() + idSuffix;
        // getID does not seem to work properly
        // quick fix to get only the primaryidentifier
        if (name.contains(" ")) {
            String[] bits = name.split(" ");
            name = bits[0];
        }
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
    protected String getTaxonId(String name) {
        return taxonIds.get(name);
    }

    /**
     * some fasta files use organism name instead of taxonId, so we need a lookup map
     * taxons are taken from project.xml.  any entries in the fasta files from organisms not in
     * this list will be ignored
     */
    private void parseTaxonIds() {
        OrganismRepository repo = OrganismRepository.getOrganismRepository();
        String[] fastaTaxonIds = fastaTaxonId.split(" ");
        for (String taxonId : fastaTaxonIds) {
            OrganismData organismData = repo.getOrganismDataByTaxonInternal(taxonId);
            String name = organismData.getGenus() + " " + organismData.getSpecies();
            taxonIds.put(name, taxonId);
        }
    }

    /**
     * Get and store() the Organism object to reference when creating new objects.
     * @param bioJavaSequence the biojava sequence to be parsed
     * @throws ObjectStoreException if there is a problem
     * @return the new Organism
     *
     * there is a specialised method getOrganism in the subclass UniProtFastaLoaderTask
     * which is actually used.
     */
    protected Organism getOrganism(ProteinSequence bioJavaSequence)
            throws ObjectStoreException {
        if (org == null) {
            org = getDirectDataLoader().createObject(Organism.class);
            org.setTaxonId(fastaTaxonId);
            getDirectDataLoader().store(org);
        }
        return org;
    }

}

