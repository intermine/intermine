package org.intermine.bio.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.bio.ontology.SequenceOntology;
import org.intermine.bio.ontology.SequenceOntologyFactory;
import org.intermine.metadata.Model;

/**
 * A Task that reads a SO OBO files and writes so_additions.xml
 *
 * @author Kim Rutherford
 */

public class SOToModelTask extends Task
{
    private File soFile, soTermListFile, outputFile;

    /**
     * Sets the File containing the SO OBO data.
     *
     * @param soFile the SO OBO file
     */
    public void setSoFile(File soFile) {
        this.soFile = soFile;
    }

    /**
     * Set the file containing a list of SO terms to be added to the data model.
     * @param soTermListFile file containing list of SO terms
     */
    public void setSoTermListFile(File soTermListFile) {
        this.soTermListFile = soTermListFile;
    }

    /**
     * Set the output file to write generated additions XML to.
     * @param outputFile the additions file that will be generated
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (soTermListFile == null || !soTermListFile.exists()) {
            throw new BuildException("Could not find file containing SO terms to add to the model,"
                    + " check the project.properties file. Property was: " + soTermListFile);
        }
        if (soFile == null || !soFile.exists()) {
            throw new BuildException("Could not find Sequence Ontology .obo file, check the"
                    + " project.properties file. Property was: " + soFile);
        }

        try {
            SequenceOntology so = SequenceOntologyFactory.getSequenceOntology(soFile,
                    soTermListFile);
            Model model = null;
            PrintWriter out = null;
            try {
                model = so.getModel();
                out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            } catch (IOException e) {
                throw new RuntimeException("Couldn't create new model file", e);
            }
            out.println(model.toAdditionsXML());
            out.flush();
            out.close();
            System.out .println("Wrote " + outputFile.getPath());
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
