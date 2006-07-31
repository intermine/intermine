package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.NoSuchElementException;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.task.FileDirectDataLoaderTask;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Organism;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

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
    private Integer fastaTaxonId;

    private Organism org;

    /**
     * Append this suffix to the identifier of the LocatedSequenceFeatures that are stored.
     */
    private String idSuffix = null;

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
     * Set the suffix to add to identifiers from the FASTA file when creating
     * LocatedSequenceFeatures.
     * @param idSuffix the suffix
     */
    public void setIdSuffix(String idSuffix) {
        this.idSuffix = idSuffix;
    }

    //Use this for testing with junit.
    protected void setFileArray(File[] files) {
        this.files = files;
    }

    public void process() {
        try {
            Class orgClass = Organism.class;
            org = (Organism) getDirectDataLoader().createObject(orgClass);
            org.setTaxonId(fastaTaxonId);
            getDirectDataLoader().store(org);
        } catch (ObjectStoreException e) {
            throw new BuildException("failed to store Organism object", e);
        }
        super.process();
    }
    
    /**
     * @throws BuildException if an ObjectStore method fails
     */
    public void execute() throws BuildException {
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
     * */
    public void processFile(File file) throws BuildException {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            SequenceIterator iter =
                    (SequenceIterator) SeqIOTools.fileToBiojava("fasta", "dna", reader);

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
            throw new BuildException("ObjectSTore problem while processing: " + file);
        }
    }

    /**
     * Create a FlyMine Sequence object for the InterMineObject of the given ID.
     * @throws ObjectStoreException 
     */
    private void setSequence(Organism org, Sequence bioJavaSequence) throws ObjectStoreException {
        Class sequenceClass = org.flymine.model.genomic.Sequence.class;
        org.flymine.model.genomic.Sequence flymineSequence =
            (org.flymine.model.genomic.Sequence) getDirectDataLoader().createObject(sequenceClass);

        flymineSequence.setResidues(bioJavaSequence.seqString());
        flymineSequence.setLength(bioJavaSequence.length());


        Class lsfClass = org.flymine.model.genomic.LocatedSequenceFeature.class;
        LocatedSequenceFeature flymineLSF = 
            (LocatedSequenceFeature) getDirectDataLoader().createObject(lsfClass);

        flymineLSF.setIdentifier(bioJavaSequence.getName() + idSuffix);
        flymineLSF.setSequence(flymineSequence);
        flymineLSF.setOrganism(org);

        try {
            getDirectDataLoader().store(flymineSequence);
            getDirectDataLoader().store(flymineLSF);

        } catch (ObjectStoreException e) {
            throw new BuildException("store failed", e);
        }
    }
}

