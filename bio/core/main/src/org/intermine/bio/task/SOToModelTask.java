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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.bio.ontology.OboParser;
import org.intermine.bio.ontology.OboToModel;
import org.intermine.util.StringUtil;

/**
 * A Task that reads a SO OBO files and writes a file mapping SO term names to FlyMine class names.
 * The each line of the output file contains a SO term name, then a space, then the corresponding
 * class name.
 * @author Kim Rutherford
 */

public class SOToModelTask extends Task
{
    private File soFile;
    private File soTermsInModelFile;
    private File outputFile;

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
    public void setSoTermsInModelFile(File soTermsInModelFile) {
        this.soTermsInModelFile = soTermsInModelFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (soTermsInModelFile == null) {
            throw new BuildException("No filename specififed for SO terms to add to the model,"
                    + " check the project.properties file.");
        }
        if (soFile == null) {
            throw new BuildException("No Sequence Ontlogy .obo filename specified, check the"
                    + " project.properties file");
        }

        Set<String> soTermsInModel;
        try {
            soTermsInModel = readSoTermsInModel();
        } catch (Exception e) {
            throw new BuildException(e);
        }

        OboParser oboParser = new OboParser();
        Collection<String> soTermNames;
        try {
            Map<String, String> soTermNameMap =
                oboParser.getTermIdNameMap(new BufferedReader(new FileReader(soFile)));
            soTermNames = soTermNameMap.values();

        } catch (IOException e) {
            throw new BuildException("error while reading SO file: " + soFile, e);
        }

        validateSoTermsInModel(soTermsInModel, soTermNames);

        try {
            OboToModel.createAndWriteModel(oboParser, soTermsInModel, outputFile, "genomic",
                    "org.intermine.model.bio");
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }


    private Set<String> readSoTermsInModel() throws IOException {
        Set<String> soTermsInModel = new HashSet<String>();
        FileReader fileReader = new FileReader(soTermsInModelFile);
        BufferedReader reader = new BufferedReader(fileReader);

        String line;
        while ((line = reader.readLine()) != null) {
            if (!StringUtils.isBlank(line)) {
                soTermsInModel.add(line.trim());
            }
        }
        return soTermsInModel;
    }

    private void validateSoTermsInModel(Collection<String> soTermsInModel,
            Collection<String> soTermNames) {
        List<String> invalidTermsConfigured = new ArrayList<String>();
        for (String soTermInModel : soTermsInModel) {
            if (!soTermNames.contains(soTermInModel)) {
                invalidTermsConfigured.add(soTermInModel);
            }
        }
        if (!invalidTermsConfigured.isEmpty()) {
            throw new BuildException("The following terms specified in "
                    + soTermsInModelFile.getPath() + " are not valid Sequence Ontology terms"
                    + " according to: " + soFile.getPath() + ": "
                    + StringUtil.prettyList(invalidTermsConfigured));
        }
    }
}
