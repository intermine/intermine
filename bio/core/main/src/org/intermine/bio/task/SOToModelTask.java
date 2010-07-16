package org.intermine.bio.task;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.bio.ontology.OboToModel;

/**
 * A Task that reads a SO OBO files and writes so_additions.xml
 *
 * @author Kim Rutherford
 */

public class SOToModelTask extends Task
{
    private File soFile, soTermsInModelFile, outputFile;

    /**
     * Sets the File containing the SO OBO data.
     *
     * @param soFile an SO OBO file
     */
    public void setSoFile(File soFile) {
        this.soFile = soFile;
    }

    /**
     * @param soTermsInModelFile file containing list of SO terms needed for this model
     */
    public void setSoTermsInModelFile(File soTermsInModelFile) {
        this.soTermsInModelFile = soTermsInModelFile;
    }

    /**
     * @param outputFile the additions file - where data is going to be written to
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() {
        if (soTermsInModelFile == null) {
            throw new BuildException("No filename specified for SO terms to add to the model,"
                    + " check the project.properties file.");
        }
        if (soFile == null) {
            throw new BuildException("No Sequence Ontlogy .obo filename specified, check the"
                    + " project.properties file");
        }

        try {
            OboToModel.createAndWriteModel("so", soFile.getCanonicalPath(),
                    "org.intermine.model.bio", soTermsInModelFile, outputFile);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
