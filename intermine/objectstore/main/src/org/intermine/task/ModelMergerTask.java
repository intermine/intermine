package org.intermine.task;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.modelproduction.ModelMerger;
import org.intermine.modelproduction.xml.InterMineModelParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task to merge a single additions file into an intermine XML model.
 *
 * @see org.intermine.modelproduction.ModelMerger
 * @author Thomas Riley
 */

public class ModelMergerTask extends Task
{
    protected List<File> additionsFiles = new ArrayList<File>();
    protected File inputModelFile;
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
        additionsFiles.add(file);
    }

    /**
     * The files containing model additions.
     * @param files the additions files
     */
    public void setAdditionsFiles(List<File> files) {
        additionsFiles = files;
    }

    /**
     * Path of file to write resulting model to. May be the same as <code>inputModelFile</code>.
     * @param file path to write resulting model to
     */
    public void setOutputFile(File file) {
        outputModelFile = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws BuildException {
        Model mergedModel = null;
        try {
            InterMineModelParser parser = new InterMineModelParser();
            FileReader reader = new FileReader(inputModelFile);
            mergedModel = parser.process(reader);
            reader.close();
        } catch (Exception e) {
            throw new BuildException("failed to read model file: " + inputModelFile, e);
        }
        if (additionsFiles.size() == 0) {
            throw new BuildException("no addition files set");
        } else {
            for (File additionsFile: additionsFiles) {
                mergedModel = processFile(mergedModel, additionsFile);
            }
        }
        try {
            FileWriter writer = new FileWriter(outputModelFile);
            writer.write(mergedModel.toString());
            writer.close();
        } catch (IOException e) {
            throw new BuildException("failed to write model file: " + outputModelFile, e);
        }
    }

    private Model processFile(Model mergedModel, File newAdditionsFile) throws BuildException {
        try {
            InterMineModelParser parser = new InterMineModelParser();
            Set<ClassDescriptor> additionClds =
                parser.generateClassDescriptors(new FileReader(newAdditionsFile));
            System.err .println("merging model additions from: " + newAdditionsFile);
            Model newMergedModel = ModelMerger.mergeModel(mergedModel, additionClds);
            return newMergedModel;
        } catch (Exception e) {
            throw new BuildException("Exception while merging " + newAdditionsFile + " into "
                                     + inputModelFile, e);
        }
    }
}
