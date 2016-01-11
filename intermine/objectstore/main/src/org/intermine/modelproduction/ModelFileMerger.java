package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.List;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ModelParser;
import org.intermine.metadata.ModelParserException;

/**
 * Methods for merging a model from a list of files.
 * @author Alex Kalderimis
 *
 */
public final class ModelFileMerger
{

    private ModelFileMerger() {
        // Hidden
    }

    /**
     * Merges a model from a core model file and a list of additions files.
     *
     * @param inputModelFile The core.xml file for the model.
     * @param additionsFiles a list of genomic additions files.
     * @param parser A parser to read the models.
     * @return The merged Model
     * @throws MetaDataException if the models are incorrect.
     */
    public static Model mergeModelFromFiles(
            File inputModelFile,
            List<File> additionsFiles,
            ModelParser parser)
        throws MetaDataException {
        Model mergedModel = null;
        try {
            FileReader reader = new FileReader(inputModelFile);
            mergedModel = parser.process(reader);
            reader.close();
        } catch (Exception e) {
            throw new MetaDataException("failed to read model file: " + inputModelFile, e);
        }
        if (additionsFiles.size() == 0) {
            throw new MetaDataException("no addition files set");
        } else {
            for (File additionsFile: additionsFiles) {
                try {
                    mergedModel = processFile(mergedModel, additionsFile, parser);
                } catch (Exception e) {
                    throw new MetaDataException("Exception while merging " + additionsFile
                            + " into " + inputModelFile, e);
                }
            }
        }
        return mergedModel;
    }

    /**
     * Merges the additions from an additions file into an existing model.
     * @param mergedModel The existing model.
     * @param newAdditionsFile a file containing genomic additions.
     * @return The existing model with the additions merged in.
     * @throws ModelParserException
     * @throws ModelMergerException
     * @throws FileNotFoundException
     */
    private static Model processFile(Model mergedModel, File newAdditionsFile, ModelParser parser)
        throws ModelParserException, ModelMergerException, FileNotFoundException {
        Set<ClassDescriptor> additionClds =
            parser.generateClassDescriptors(new FileReader(newAdditionsFile),
                    mergedModel.getPackageName());
        System.err .println("merging model additions from: " + newAdditionsFile);
        Model newMergedModel = ModelMerger.mergeModel(mergedModel, additionClds);
        return newMergedModel;
    }
}
