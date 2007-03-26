package org.intermine.task;

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
import java.io.FileWriter;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.metadata.Model;
import org.intermine.modelproduction.ModelMerger;
import org.intermine.modelproduction.xml.InterMineModelParser;

/**
 * Task to merge a single additions file into an intermine XML model.
 * 
 * @see org.intermine.modelproduction.ModelMerger
 * @author Thomas Riley
 */

public class ModelMergerTask extends Task
{
    protected File inputModelFile;
    protected File additionsFile;
    protected File outputModelFile;
    
    /**
     * Set the model to add additions to.
     * @param file path to model file
     */
    public void setInputModelFile(File file) {
        inputModelFile = file;
    }
    
    /**
     * The file containing model additions.
     * @param file the additions file
     */
    public void setAdditionsFile(File file) {
        additionsFile = file;
    }
    
    /**
     * Path of file to write resulting model to. May be the same as <code>inputModelFile</code>.
     * @param file path to write resulting model to
     */
    public void setOutputFile(File file) {
        outputModelFile = file;
    }

    /**
     * Merge additions into input model and write resulting model to output file.
     * 
     * @see Task#execute()
     */
    public void execute() throws BuildException {
        try {
            InterMineModelParser parser = new InterMineModelParser();
            FileReader reader = new FileReader(inputModelFile);
            Model model = parser.process(reader);
            reader.close();
            Set additionClds = parser.generateClassDescriptors(new FileReader(additionsFile));
            Model merged = ModelMerger.mergeModel(model, additionClds);
            FileWriter writer = new FileWriter(outputModelFile);
            writer.write(merged.toString());
            writer.close();
        } catch (Exception e) {
            throw new BuildException("Exception while merging " + inputModelFile, e);
        }
    }
}
