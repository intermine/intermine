package org.flymine.task;

import java.lang.reflect.Method;
import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * Uses an ObjectStoreWriter to insert XML data from a file
 *
 * @author Andrew Varley
 */
public class InsertXmlDataTask extends ClassPathTask
{

    protected String store;
    protected File file;

    /**
     * Set the ObjectStore
     *
     * @param store the name of the ObjectStore
     */
    public void setObjectStore(String store) {
        this.store = store;
    }

    /**
     * Set the XML file to be inserted
     *
     * @param file the name of the file
     */
    public void setFile(File file) {
        this.file = file;
    }


    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.store == null) {
            throw new BuildException("objectstore attribute is not set");
        }
        if (this.file == null) {
            throw new BuildException("file attribute is not set");
        }

        try {
            Object driver = loadClass("org.flymine.task.XmlDataLoaderDriver");

            // Have to execute the loadData method by reflection as
            // cannot cast to something that this class (which may use
            // a different ClassLoader) can see

            Method method = driver.getClass().getMethod("loadData", new Class[] {String.class,
                                                                                 File.class });
            method.invoke(driver, new Object [] {store,
                                                 file });
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

}
