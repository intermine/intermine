package org.intermine.bio.task;

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.bio.ontology.OboParser;
import org.intermine.util.TypeUtil;

/**
 * A Task that reads a SO OBO files and writes a file mapping SO term names to FlyMine class names.
 * The each line of the output file contains a SO term name, then a space, then the corresponding
 * class name.
 * @author Kim Rutherford
 */

public class SOClassNameMapMakerTask extends Task
{
    private File outputFile, soFile;

    /**
     * Create a new SOClassNameMapMakerTask object.
     */
    public SOClassNameMapMakerTask () {
        // empty
    }

    /**
     * Sets the File containing the SO OBO data.
     *
     * @param soFile an SO OBO file
     */
    public void setSoFile(File soFile) {
        this.soFile = soFile;
    }

    /**
     * Sets the value of outputFile.  The each line of the output file contains a SO term name, then
     * a space, then the corresponding class name.
     * @param outputFile an output
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        Reader reader;
        try {
            reader = new FileReader(soFile);
        } catch (FileNotFoundException e) {
            throw new BuildException("cannot file SO file: " + soFile, e);
        }
        
        OboParser oboParser = new OboParser();
        Map termIdNameMap;
        try {
            termIdNameMap = oboParser.getTermIdNameMap(reader);
        } catch (IOException e) {
            throw new BuildException("error while reading SO file: " + soFile, e);
        }
        
        Iterator termNameIter = termIdNameMap.values().iterator();
        
        try {
            FileWriter fw = new FileWriter(outputFile);
            PrintWriter pw = new PrintWriter(fw);

            while (termNameIter.hasNext()) {
                String termName = (String) termNameIter.next();
                pw.println(TypeUtil.javaiseClassName(termName) + " " + termName);
            }

            pw.close();
            fw.close();
        } catch (IOException e) {
            throw new BuildException("error while writing output file: " + outputFile, e);
        }
    }
}
