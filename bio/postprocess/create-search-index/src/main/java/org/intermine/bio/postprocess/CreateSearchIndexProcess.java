package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.lucene.KeywordSearch;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;


/**
 * Create a the Lucene keyword search index for a mine.
 * @author Alex Kalderimis
 */
public class CreateSearchIndexProcess extends PostProcessor
{
    /**
     * Create a new instance of CreateSearchIdexProcess
     *
     * @param osw object store writer
     */
    public CreateSearchIndexProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     */
    public void postProcess()
            throws ObjectStoreException {
        System .out.println("Creating lucene index for keyword search...");

        //read class keys to figure out what are keyFields during indexing
        Properties classKeyProperties = new Properties();
        try {
            classKeyProperties.load(getClass().getClassLoader().getResourceAsStream(
                "class_keys.properties"));
        } catch (IOException e) {
            throw new BuildException("Could not open the class keys");
        } catch (NullPointerException e) {
            throw new BuildException("Could not find the class keys");
        }
        Map<String, List<FieldDescriptor>> classKeys =
                ClassKeyHelper.readKeys(osw.getModel(), classKeyProperties);

        //index and save
        KeywordSearch.saveIndexToDatabase(osw, classKeys);
        KeywordSearch.deleteIndexDirectory();
    }
}
