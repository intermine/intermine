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

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.dataloader.Source;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;
import org.flymine.model.genomic.Organism;

/**
 * A task that can read a set of FASTA files and create the corresponding Sequence objects in an
 * ObjectStore.
 *
 * @author Kim Rutherford
 * @author Peter Mclaren - modifications to use an IntegrationWriter - so we can run it as a source.
 */

public class FastaLoaderTask extends Task {

    protected String integrationWriterAlias;
    protected String sourceName;
    protected Integer fastaTaxonId;
    protected boolean ignoreDuplicates = false;
    protected List fileSets = new ArrayList();

    /**
     * Set the IntegrationWriter.
     *
     * @param integrationWriterAlias the name of the IntegrationWriter
     */
    public void setIntegrationWriterAlias(String integrationWriterAlias) {
        this.integrationWriterAlias = integrationWriterAlias;
    }

    /**
     * Set the source name, as used by primary key priority config.
     *
     * @param sourceName the name of the data source
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Set the value of ignoreDuplicates for the IntegrationWriter
     *
     * @param ignoreDuplicates the value of ignoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
    }

    /**
     * Set the Taxon Id of the Organism we are loading.
     *
     * @param fastaTaxonId the taxon id to set.
     */
    public void setFastaTaxonId(Integer fastaTaxonId) {
        this.fastaTaxonId = fastaTaxonId;
    }

    /**
     * Add a FileSet to read from
     *
     * @param fileSet the FileSet
     */
    public void addFileSet(FileSet fileSet) {
        fileSets.add(fileSet);
    }

    /**
     * @throws BuildException if an ObjectStore method fails
     */
    public void execute() throws BuildException {

        if (integrationWriterAlias == null) {
            throw new BuildException("FastaLoaderTask - integrationWriterAlias property not set");
        }

        if (sourceName == null) {
            throw new BuildException("FastaLoaderTask - sourceName property not set");
        }

        if (fastaTaxonId == null) {
            throw new BuildException("FastaLoaderTask - fastaTaxonId property not set");
        }

        if (fileSets.isEmpty()) {
            System.out.println("FastaLoaderTask - fileSets list is empty");
            return;
        }

        IntegrationWriter iw;
        Source source;
        Source skelSource;
        org.flymine.model.genomic.Organism org;

        try {
            iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriterAlias);
            iw.beginTransaction();
            iw.setIgnoreDuplicates(ignoreDuplicates);

            source = iw.getMainSource(sourceName);
            skelSource = iw.getSkeletonSource(sourceName);

            Class orgClass = org.flymine.model.genomic.Organism.class;
            InterMineObject newOrgObj = (InterMineObject)
                    DynamicUtil.createObject(Collections.singleton(orgClass));
            org = (org.flymine.model.genomic.Organism) newOrgObj;
            org.setTaxonId(fastaTaxonId);
            org.setId(getUniqueInternalId());
            iw.store(org, source, skelSource);

        } catch (ObjectStoreException e) {
            throw new BuildException(e);
        }

        for (Iterator fileSetIter = fileSets.iterator(); fileSetIter.hasNext();) {
            FileSet fileSet = (FileSet) fileSetIter.next();

            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File file = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file: " + file.getName());

                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));

                    SequenceIterator iter =
                            (SequenceIterator) SeqIOTools.fileToBiojava("fasta", "dna", reader);

                    while (iter.hasNext()) {
                        setSequence(org, iter.nextSequence(), iw, source, skelSource);
                    }
                } catch (BioException e) {
                    throw new BuildException("sequence not in fasta format or wrong alphabet for: "
                            + file, e);
                } catch (NoSuchElementException e) {
                    throw new BuildException("no fasta sequences in: " + file, e);
                } catch (FileNotFoundException e) {
                    throw new BuildException("problem reading file - file not found: " + file, e);
                }
            }
        }
        try {
            iw.commitTransaction();
            iw.close();
        } catch (ObjectStoreException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Create a FlyMine Sequence object for the InterMineObject of the given ID.
     */
    private void setSequence(Organism org, Sequence bioJavaSequence, IntegrationWriter iw,
                             Source source, Source skelSource) {
        Class sequenceClass = org.flymine.model.genomic.Sequence.class;
        InterMineObject newObject =
                (InterMineObject) DynamicUtil.createObject(Collections.singleton(sequenceClass));
        org.flymine.model.genomic.Sequence flymineSequence =
                (org.flymine.model.genomic.Sequence) newObject;
        flymineSequence.setResidues(bioJavaSequence.seqString());
        flymineSequence.setLength(bioJavaSequence.length());
        flymineSequence.setId(getUniqueInternalId());

        Class lsfClass = org.flymine.model.genomic.LocatedSequenceFeature.class;

        InterMineObject newLSFObject =
                (InterMineObject) DynamicUtil.createObject(Collections.singleton(lsfClass));
        org.flymine.model.genomic.LocatedSequenceFeature flymineLSF =
                (org.flymine.model.genomic.LocatedSequenceFeature) newLSFObject;
        flymineLSF.setIdentifier(bioJavaSequence.getName());
        flymineLSF.setSequence(flymineSequence);
        flymineLSF.setOrganism(org);
        flymineLSF.setId(getUniqueInternalId());

        try {
            iw.store(flymineSequence, source, skelSource);
            iw.store(flymineLSF, source, skelSource);

        } catch (ObjectStoreException e) {
            throw new BuildException("store failed", e);
        }
    }

    private synchronized Integer getUniqueInternalId() {
        return new Integer(id++);
    }

    private int id = 0;
}

