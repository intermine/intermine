package org.flymine.task;

import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;

import org.xml.sax.InputSource;

import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ojb.ObjectStoreWriterOjbImpl;
import org.flymine.dataloader.XmlDataLoader;
import org.flymine.dataloader.IntegrationWriter;
import org.flymine.dataloader.IntegrationWriterSingleSourceImpl;

/**
 * Class that actually loads XML data
 *
 * @author Andrew Varley
 */
public class XmlDataLoaderDriver
{
    /**
     * Load XML data from a file
     *
     * @param store the name of the ObjectStore to write data to
     * @param file the file to load
     * @throws BuildException if any error occurs
     */
    public void loadData(String store, File file)
        throws BuildException {

        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore(store);
            ObjectStoreWriter writer = new ObjectStoreWriterOjbImpl(os);
            IntegrationWriter iw = new IntegrationWriterSingleSourceImpl(null, writer);
            XmlDataLoader dl = new XmlDataLoader(iw);
            dl.processXml(new InputSource(new FileReader(file)));
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

