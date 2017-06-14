package org.intermine.web.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.lucene.KeywordSearch;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

/**
 * Create a the Lucene keyword search index for a mine.
 * @author Alex Kalderimis
 *
 */
public class CreateSearchIndexTask extends Task
{

    protected String osAlias = null;
    protected ObjectStore os;
    private ClassLoader classLoader;

    /**
     * Set the alias of the main object store.
     * @param osAlias the object store alias
     */
    public void setOSAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the object store.
     * @param os The object store.
     */
    public void setObjectStore(ObjectStore os) {
        this.os = os;
    }

    private ObjectStore getObjectStore() throws Exception {
        if (os != null) {
            return os;
        }
        if (osAlias == null) {
            throw new BuildException("objectStoreWriter attribute is not set");
        }
        if (os == null) {
            System .out.println("Connecting to db: " + osAlias);
            os = ObjectStoreFactory.getObjectStore(osAlias);
        }
        return os;
    }

    /**
     * Set the class loader
     * @param loader The class loader.
     */
    public void setClassLoader(ClassLoader loader) {
        this.classLoader = loader;
    }

    private ClassLoader getClassLoader() {
        if (classLoader != null) {
            return classLoader;
        }
        return this.getClass().getClassLoader();
    }

    @Override
    public void execute() {
        System .out.println("Creating lucene index for keyword search...");

        ObjectStore objectStore;
        try {
            objectStore = getObjectStore();
        } catch (Exception e) {
            throw new BuildException(e);
        }
        if (!(objectStore instanceof ObjectStoreInterMineImpl)) {
            // Yes, yes, this is horrific...
            throw new RuntimeException("Got invalid ObjectStore - must be an "
                    + "instance of ObjectStoreInterMineImpl!");
        }

        //read class keys to figure out what are keyFields during indexing
        InputStream is = getClassLoader().getResourceAsStream("class_keys.properties");
        Properties classKeyProperties = new Properties();
        try {
            classKeyProperties.load(is);
        } catch (NullPointerException e) {
            throw new BuildException("Could not find the class keys");
        } catch (IOException e) {
            throw new BuildException("Could not read the class keys", e);
        }
        Map<String, List<FieldDescriptor>> classKeys =
            ClassKeyHelper.readKeys(objectStore.getModel(), classKeyProperties);

        //index and save
        KeywordSearch.saveIndexToDatabase(objectStore, classKeys);
        KeywordSearch.deleteIndexDirectory();
    }


}
