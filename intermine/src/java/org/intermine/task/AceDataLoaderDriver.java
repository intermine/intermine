package org.flymine.task;

import org.apache.tools.ant.BuildException;

import org.acedb.AceURL;

import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.dataloader.AceDataLoader;
import org.flymine.dataloader.IntegrationWriter;
import org.flymine.dataloader.IntegrationWriterSingleSourceImpl;

/**
 * Class that actually loads Ace data
 *
 * @author Andrew Varley
 */
public class AceDataLoaderDriver
{
    /**
     * Load ace data from an AceDB server
     *
     * @param storeAlias the name of the ObjectStore to use
     * @param writerAlias the name of the ObjectStore to write data to
     * @param user the user name by which to log into AceDB
     * @param password the password for that user
     * @param host the host on which the AceDB server is running
     * @param port the port on which the AceDB server is listening
     * @throws BuildException if any error occurs
     */
    public void loadData(String storeAlias, String writerAlias, String user, String password
                         , String host, int port)
        throws BuildException {

        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore(storeAlias);
            ObjectStoreWriter writer = ObjectStoreWriterFactory
                .getObjectStoreWriter(writerAlias, os);

            IntegrationWriter iw = new IntegrationWriterSingleSourceImpl(null, writer);

            AceURL aceURL = new AceURL("acedb://" + user + ':' + password + '@'
                                       + host + ':' + port);

            AceDataLoader dl = new AceDataLoader(iw);
            dl.processAce(aceURL);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

