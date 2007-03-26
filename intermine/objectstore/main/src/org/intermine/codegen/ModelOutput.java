package org.intermine.codegen;

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
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.intermine.metadata.*;

/**
 * Parent for classes that map InterMine metadata to other formats
 *
 * @author Mark Woodbridge
 */
public abstract class ModelOutput
{
    private static final Logger LOG = Logger.getLogger(ModelOutput.class);
    protected static final String INDENT = "    ";
    protected static final String ENDL = System.getProperty("line.separator");

    protected Model model;
    protected File file; //note: this is a directory

    /**
     * Constructor
     * @param model to model used for generation
     * @param file the destination directory for generated output
     * @throws Exception if problem occurs in loading model
     */
    public ModelOutput(Model model, File file) throws Exception {
        this.model = model;
        this.file = file;
    }

    /**
     * Perform the mapping
     */
    public abstract void process();

    /**
     * Generate the output for a Model
     * @param model the Model
     * @return the relevant String representation
     */
    protected abstract String generate(Model model);

    /**
     * Generate the output for a ClassDescriptor
     * @param cld the ClassDescriptor
     * @return the relevant String representation
     */
    protected abstract String generate(ClassDescriptor cld);

    /**
     * Generate the output for a AttributeDescriptor
     * @param attr the AttributeDescriptor
     * @return the relevant String representation
     */
    protected abstract String generate(AttributeDescriptor attr);

    /**
     * Generate the output for a ReferenceDescriptor
     * @param ref the ReferenceDescriptor
     * @return the relevant String representation
     */
    protected abstract String generate(ReferenceDescriptor ref);

    /**
     * Generate the output for a CollectionDescriptor
     * @param col the CollectionDescriptor
     * @return the relevant String representation
     */
    protected abstract String generate(CollectionDescriptor col);

    /**
     * Create a file, after removing it if it already exists
     * @param f the file
     */
    protected static void initFile(File f) {
        try {
            f.delete();
        } catch (SecurityException exp) {
            LOG.warn("Cannot delete: " + f.getPath());
        }
        LOG.debug("Generating " + f.getPath());
    }

    /**
     * Output a String to the specified File
     * @param f the output file
     * @param src the String to output
     */
    protected static void outputToFile(File f, String src) {
        BufferedWriter fos = null;
        try {
            fos = new BufferedWriter(new FileWriter (f, true));
            fos.write (src);
        } catch (IOException exp) {
            // ignore
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException exp) {
                LOG.warn("FAILED: " + f.getPath());
            }
        }
    }
}


