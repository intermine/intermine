package org.flymine.task;

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
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.LocatedSequenceFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;

/**
 * A task that can read a set of FASTA files and create the corresponding Sequence objects in an
 * ObjectStore.
 *
 * @author Kim Rutherford
 */

public class FastaReadTask extends FileReadTask
{
    /**
     * If set, append this suffix to identifiers from the FASTA file when looking for the object in
     * the ObjectStore
     */
    private String idSuffix = null;

    /**
     * Set the suffix to add to identifiers from the FASTA file when looking for the object in the
     * ObjectStore.
     * @param idSuffix the suffix
     */
    public void setIdSuffix(String idSuffix) {
        this.idSuffix = idSuffix;
    }

    /**
     * Create a FlyMine Sequence object for the InterMineObject of the given ID.
     */
    private void setSequence(Integer flymineId, Sequence bioJavaSequence) {
        LocatedSequenceFeature lsf;

        try {
            lsf = (LocatedSequenceFeature) getObjectStoreWriter().getObjectById(flymineId);
        } catch (ObjectStoreException e) {
            throw new BuildException("internal error: object with id " + flymineId
                                     + " not found", e);
        }

        Class sequenceClass = org.flymine.model.genomic.Sequence.class;
        InterMineObject newObject = 
            (InterMineObject) DynamicUtil.createObject(Collections.singleton(sequenceClass));
        org.flymine.model.genomic.Sequence flymineSequence =
            (org.flymine.model.genomic.Sequence) newObject;

        flymineSequence.setResidues(bioJavaSequence.seqString());
        flymineSequence.setLength(bioJavaSequence.length());

        lsf.setSequence(flymineSequence);
        lsf.setLength(new Integer(bioJavaSequence.length()));

        try {
            getObjectStoreWriter().store(flymineSequence);
            getObjectStoreWriter().store(lsf);
        } catch (ObjectStoreException e) {
            throw new BuildException("store failed", e);
        }
    }

    /**
     * Query all objects of the class given by the className parameter that have a reference to the
     * organism given by the organismAbbreviation parameter.  Create a new Sequence object for each
     * object by looking up the sequence by identifier in the FASTA files.
     * New Sequence objects will be created for objects of class given by the className parameter
     * that have an identifier attribute that matches the identifier of a sequence in the FASTA
     * file.  The class must be a sub-class of LocatedSequenceFeature.
     * @throws BuildException if an ObjectStore method fails
     */
    public void execute() throws BuildException {        
        if (getOrganismAbbreviation() == null) {
            throw new BuildException("organismAbbreviation not set");
        }

        if (getOswAlias() == null) {
            throw new BuildException("oswAlias not set");
        }

        if (getClassName() == null) {
            throw new BuildException("className not set");
        }
        
        if (getKeyFieldName() == null) {
            throw new BuildException("keyFieldName not set");
        }

        ObjectStore os = getObjectStoreWriter().getObjectStore();
        
        Map idMap = new HashMap(getIdMap(os));
        
        Iterator fileSetIter = getFileSets().iterator();
        
        while (fileSetIter.hasNext()) {
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
                        Sequence seq = iter.nextSequence();
                        String identifier = seq.getName();
                        if (idSuffix != null) {
                            identifier += idSuffix;
                        }
                        Integer flymineId = (Integer) idMap.get(identifier);
                        
                        if (flymineId == null) {
                            System.err .println("Identifier not found in FlyMine: "
                                                + seq.getName());
                        } else {
                            setSequence(flymineId, seq);
                            // remove now so that at the end of the run we know which IDs don't have
                            // sequence in the FASTA files
                            idMap.remove(identifier);
                        }
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
        
        Iterator iter = idMap.keySet().iterator();
            
        while (iter.hasNext()) {
            System.err .println("identifier not found in FASTA file: " + iter.next());
        }
    }
}
