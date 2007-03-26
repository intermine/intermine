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

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.SAXParser;

import org.flymine.model.genomic.Organism;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.xml.sax.InputSource;

/**
 * A task that can read a FlyBase XML file dumped from their "Annotated Genomics Sequence search"
 * and fill in information about regulatory regions.
 * See /shared/data/flybase/proteinbindingsites/README
 *
 * @author Kim Rutherford
 * @author Peter Mclaren
 */

public class FlyBaseProteinBindingSiteLoaderTask extends FileDirectDataLoaderTask
{
    protected static final Logger LOG = Logger.getLogger(FlyBaseProteinBindingSiteLoaderTask.class);

    private Integer taxonId;

    private Organism org;

    //Set this if we want to do some testing...
    private File[] files = null;

    /**
     * Set the Taxon Id of the Organism we are loading.
     *
     * @param taxonId the taxon id to set.
     */
    public void setTaxonId(Integer taxonId) {
        this.taxonId = taxonId;
    }

    //Use this for testing with junit.
    protected void setFileArray(File[] files) {
        this.files = files;
    }

    public void process() {
        try {
            Class orgClass = Organism.class;
            org = (Organism) getDirectDataLoader().createObject(orgClass);
            org.setTaxonId(taxonId);
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
            // setFileArray() is used only for testing
            for (int i = 0; i < files.length; i++) {
                processFile(files[i]);
            }
        } else {
            // this will call processFile() for each file
            super.execute();
        }
    }


    /**
     * Handles each file.  Factored out so we can supply files for testing.
     */
    public void processFile(File file) throws BuildException {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            try {
                FBProteinBindingSiteHandler profileHandler =
                    new FBProteinBindingSiteHandler(getDirectDataLoader());
                SAXParser.parse(new InputSource(reader), profileHandler);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + file, e);
        }
    }
}

