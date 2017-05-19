package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;

import org.intermine.metadata.Model;

/**
 * Parent class for DataConverters that read from directories
 * @author Julie Sullivan
 */
public abstract class DirectoryConverter extends DataConverter
{
    /**
     * Constructor
     * @param writer the Writer used to output the resultant items
     * @param model the data model
     */
    public DirectoryConverter(ItemWriter writer, Model model) {
        super(writer, model);
    }

    /**
     * Perform the conversion files in the directory
     * @param dataDir directory containing files to convert
     * @throws Exception if an error occurs during processing
     */
    public abstract void process(File dataDir) throws Exception;

    /**
     * Perform any necessary clean-up after post-conversion
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        // empty
    }
}
