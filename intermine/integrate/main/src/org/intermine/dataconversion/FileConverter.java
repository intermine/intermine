package org.intermine.dataconversion;

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
import java.io.Reader;

import org.intermine.metadata.Model;

/**
 * Parent class for DataConverters that read from files
 * @author Mark Woodbridge
 */
public abstract class FileConverter extends DataConverter
{
    private File currentFile;

    /**
     * Constructor
     * @param writer the Writer used to output the resultant items
     * @param model the data model
     */
    public FileConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }
    
    /**
     * Perform the currentFile conversion
     * @param reader BufferedReader used as input
     * @throws Exception if an error occurs during processing
     */
    public abstract void process(Reader reader) throws Exception;

    /**
     * Perform any necessary clean-up after post-conversion
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        // empty
    }

    /**
     * Set the current File that is being processed.  Called by FileConverterTask.execute().
     * @param currentFile the current File that is being processed
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
    
    /**
     * Return the File that is currently being converted.
     * @return the current File
     */
    public File getCurrentFile() {
        return currentFile;
    }
}
