package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileInputStream;

import org.apache.tools.ant.BuildException;

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
     * @param iwAlias the name of the IntegrationWriter to use
     * @param file the file to load
     * @throws BuildException if any error occurs
     */
    public void loadData(String iwAlias, File file)
        throws BuildException {

        try {
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(iwAlias);
            XmlDataLoader dl = new XmlDataLoader(iw);
            dl.processXml(new FileInputStream(file));
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

