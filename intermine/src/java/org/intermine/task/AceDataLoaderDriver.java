/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.task;

import org.apache.tools.ant.BuildException;

import org.acedb.AceURL;

import org.flymine.dataloader.AceDataLoader;
import org.flymine.dataloader.IntegrationWriter;
import org.flymine.dataloader.IntegrationWriterFactory;

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
     * @param iwAlias the name of the IntegrationWriter to use
     * @param user the user name by which to log into AceDB
     * @param password the password for that user
     * @param host the host on which the AceDB server is running
     * @param port the port on which the AceDB server is listening
     * @throws BuildException if any error occurs
     */
    public void loadData(String iwAlias, String user, String password, String host, int port)
        throws BuildException {
        try {
            AceURL aceURL = new AceURL("acedb://" + user + ':' + password + '@'
                                       + host + ':' + port);
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(iwAlias, null);
            AceDataLoader dl = new AceDataLoader(iw);
            dl.processAce(aceURL);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

