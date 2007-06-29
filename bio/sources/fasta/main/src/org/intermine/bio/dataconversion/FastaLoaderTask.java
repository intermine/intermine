package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.NoSuchElementException;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Organism;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;
import org.apache.log4j.Logger;
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
    protected static final Logger LOG = Logger.getLogger(FastaLoaderTask.class);

    private Integer fastaTaxonId;
    private String sequenceType = "dna";
    private String classAttribute = "identifier";
    private Organism org;
    private String className;
    private int storeCount = 0;
    /**
     * Append this suffix to the identifier of the LocatedSequenceFeatures that are stored.
     */
    private String idSuffix = "";

    //Set this if we want to do some testing...
    private File[] files = null;

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
     * LocatedSequenceFeatures.
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
     * The attribute of the class created to set with the identifying field.  If not set will
     * be 'identifier'.
     * @param classAttribute the class name
     */
    public void setClassAttribute(String classAttribute) {
        this.classAttribute = classAttribute;
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
    public void process() {
        long start = System.currentTimeMillis();
        try {
            Class orgClass = Organism.class;
            org = (Organism) getDirectDataLoader().createObject(orgClass);
            org.setTaxonId(fastaTaxonId);
            getDirectDataLoader().store(org);
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
    public void processFile(File file) throws BuildException {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            System.err .println("reading " + sequenceType + " sequence from: " + file);

            SequenceIterator iter =
                    (SequenceIterator) SeqIOTools.fileToBiojava("fasta", sequenceType, reader);

            if (!iter.hasNext()) {
                System.err .println("no fasta sequences found - exiting");
                return;
            }

            while (iter.hasNext()) {
                setSequence(org, iter.nextSequence());
            }
        } catch (BioException e) {
            throw new BuildException("sequence not in fasta format or wrong alphabet for: "
                    + file, e);
        } catch (NoSuchElementException e) {
            throw new BuildException("no fasta sequences in: " + file, e);
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + file, e);
        } catch (ObjectStoreException e) {
            throw new BuildException("ObjectStore problem while processing: " + file, e);
        }
    }

    /**
     * Create a FlyMine Sequence and an object of type className for the given BioJava Sequence.
     */
    private void setSequence(Organism org, Sequence bioJavaSequence) throws ObjectStoreException {
        Class sequenceClass = org.flymine.model.genomic.Sequence.class;
        org.flymine.model.genomic.Sequence flymineSequence =
            (org.flymine.model.genomic.Sequence) getDirectDataLoader().createObject(sequenceClass);

        flymineSequence.setResidues(bioJavaSequence.seqString());
        flymineSequence.setLength(bioJavaSequence.length());


        Class c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException("unknown class: " + className
                                       + " while creating new Sequence object");
        }
        InterMineObject imo = getDirectDataLoader().createObject(c);

        String attributeValue = bioJavaSequence.getName() + idSuffix;
        try {
            TypeUtil.setFieldValue(imo, classAttribute, attributeValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error setting: " + className + "."
                                               + classAttribute + " to: " + attributeValue
                                               + ". Does the attribute exist?");
        }
        TypeUtil.setFieldValue(imo, "sequence", flymineSequence);
        TypeUtil.setFieldValue(imo, "organism", org);
        if (TypeUtil.getSetter(c, "length") != null) {
            TypeUtil.setFieldValue(imo, "length", new Integer(flymineSequence.getLength()));
        }
        try {
            getDirectDataLoader().store(flymineSequence);
            getDirectDataLoader().store(imo);
            storeCount += 2;
        } catch (ObjectStoreException e) {
            throw new BuildException("store failed", e);
        }
    }
}

