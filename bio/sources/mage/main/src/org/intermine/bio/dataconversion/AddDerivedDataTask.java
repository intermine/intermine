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

import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Add files of normalised data to a MAGE-ML file.
 *
 * @author Richard Smith
 */
public class AddDerivedDataTask extends Task
{
    //private static final Logger LOG = Logger.getLogger(AddDerivedDataTask.class);

    protected File srcFile, tgtFile;
    protected String extension;
    protected String fileName;

    /**
     * The original MAGE-ML file.
     *
     * @param srcFile original MAGE-ML file
     */
    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * File to write the updated MAGE-ML
     *
     * @param tgtFile updated MAGE-ML to write to
     */
    public void setTgtFile(File tgtFile) {
        this.tgtFile = tgtFile;
    }

    /**
     * Expect normalised data files to have the same name as corresponding
     * raw data but with an added extension.
     *
     * @param extension normalised data file extension
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * @param fileName supplyment filename to provide derivedBioAssayData
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (srcFile == null || tgtFile == null || (extension == null && fileName == null)) {
            throw new BuildException("Must set all parameters");
        }

        if (extension != null && fileName == null) {
            try {
                MageConverter.processDerivedBioAssays(new FileReader(srcFile), tgtFile,
                                                  extension);
            } catch (Exception e) {
                throw new BuildException(e);
            }
        } else if (extension == null && fileName != null) {
            try {
                MageConverter.processDerivedBioAssaysFromSup(new FileReader(srcFile), tgtFile,
                                                  fileName);
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
    }
}
