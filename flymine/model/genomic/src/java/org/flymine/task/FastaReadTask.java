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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Results;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.LocatedSequenceFeature;

/**
 * A task that can read a set of FASTA files and create the corresponding Sequence objects in an
 * ObjectStore.
 *
 * @author Kim Rutherford
 */

public class FastaReadTask extends Task
{
    private String className, organismAbbreviation, alias;
    private FileSet fileSet;
    private Map idMap = new HashMap();
    private ObjectStoreWriter osw;
    
    /**
     * Set the class name.  New Sequence objects will be created for objects of this class that have
     * an identifier attribute that matches the identifier of a sequence in the fasta file.  The
     * class must be a sub-class of LocatedSequenceFeature.
     * @param className the class of object to create sequences for
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Set the organism abbreviation.  Only objects that have a reference to this organism will have
     * thier sequences set.
     * @param organismAbbreviation the organism of the objects to set
     */
    public void setOrganismAbbreviation(String organismAbbreviation) {
        this.organismAbbreviation = organismAbbreviation;
    }

    /**
     * The ObjectStoreWriter alias to use when querying and creating objects.
     * @param alias the ObjectStoreWriter alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    private ObjectStoreWriter getObjectStoreWriter() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }
        if (osw == null) {
            try {
                osw = ObjectStoreWriterFactory.getObjectStoreWriter(alias);
            } catch (ObjectStoreException e) {
                throw new BuildException("error getting ObjectStoreWriter", e);
            }
        }
        return osw;
    }

    /**
     * Set the FileSet to read the FASTA files from.
     * @param fileSet the FileSet
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * Build a Map from identifier to InterMine ID for all the objects of type className and
     * organism given by organismAbbreviation.
     */
    private void buildIdMap(ObjectStore os) throws BuildException {
        Query q = new Query();
        q.setDistinct(true);
        String fullClassName = os.getModel().getPackageName() + "." + className;
        Class c;
        try {
            c = Class.forName(fullClassName);
        } catch (ClassNotFoundException e) {
            throw new BuildException("cannot find class for: " + fullClassName);
        }
        QueryClass qcObj = new QueryClass(c);
        QueryField qfObjId = new QueryField(qcObj, "id");
        QueryField qfObjIdentifier = new QueryField(qcObj, "identifier");
        q.addFrom(qcObj);
        q.addToSelect(qfObjId);
        q.addToSelect(qfObjIdentifier);

        QueryClass qcOrg = new QueryClass(Organism.class);

        QueryObjectReference ref = new QueryObjectReference(qcObj, "organism");
        ContainsConstraint cc = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qcOrg);

        SimpleConstraint sc = new SimpleConstraint(new QueryField(qcOrg, "abbreviation"),
                                                   ConstraintOp.EQUALS,
                                                   new QueryValue(organismAbbreviation));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc);
        cs.addConstraint(sc);

        q.setConstraint(cs);

        q.addFrom(qcOrg);

        Results res = new Results(q, os, os.getSequence());

        res.setBatchSize(10000);

        Iterator iter = res.iterator();

        while (iter.hasNext()) {
            List row = (List) iter.next();

            idMap.put(row.get(1), row.get(0));
        }
    }
    
    /**
     * Create a FlyMine Sequence object for the InterMineObject of the given ID and 
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

        lsf.setSequence(flymineSequence);

        try {
            osw.store(flymineSequence);
            osw.store(lsf);
        } catch (ObjectStoreException e) {
            throw new BuildException("store failed", e);
        }
    }

    /**
     * Query all objects of the class given by the className parameter that have a reference to the
     * organism given by the organismAbbreviation parameter.  Create a new Sequence object for each
     * object by looking up the sequence by identifier in the FASTA files.
     * @see Task#execute()
     */
    public void execute() throws BuildException {        
        if (organismAbbreviation == null) {
            throw new BuildException("organismAbbreviation not set");
        }

        if (alias == null) {
            throw new BuildException("alias not set");
        }

        if (className == null) {
            throw new BuildException("className not set");
        }

        ObjectStore os = getObjectStoreWriter().getObjectStore();

        buildIdMap(os);

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

                    Integer flymineId = (Integer) idMap.get(seq.getName());

                    if (flymineId == null) {
                        System.err .println("Identifier not found in FlyMine: " + seq.getName());
                    } else {
                        setSequence(flymineId, seq);
                        // remove now so that at the end of the run we know which IDs don't have
                        // sequence in the FASTA files
                        idMap.remove(flymineId);
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

        
        int count = 0;
        Iterator iter = idMap.keySet().iterator();

        while (iter.hasNext()) {
            System.err .println("identifier not found in FASTA file: " + iter.next());
            count++;
        }
    }
    
}
