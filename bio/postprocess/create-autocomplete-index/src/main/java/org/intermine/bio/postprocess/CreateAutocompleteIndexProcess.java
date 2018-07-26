package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;



import java.util.Properties;

import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.sql.Database;
import java.sql.SQLException;
import org.apache.tools.ant.BuildException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;


/**
 * Create a the autocomplete
 * @author Alex Kalderimis
 */
public class CreateAutocompleteIndexProcess extends PostProcessor
{
    /**
     * Create a new instance of CreateAutocompleteIndexProcess
     *
     * @param osw object store writer
     */
    public CreateAutocompleteIndexProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     */
    public void postProcess()
            throws ObjectStoreException {

        System.out .println("create autocomplete index ...");

        try {

            ObjectStore os = osw.getObjectStore();
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();

            try {
                AutoCompleter ac = new AutoCompleter(os);
                ac.buildIndex(os);

            } catch (IOException e) {
                e.printStackTrace();

            } catch (ObjectStoreException e) {
                e.printStackTrace();

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                
            }

        System.out.println("Creating auto complete index has completed");

        } catch (NullPointerException e) {
            throw new BuildException("Could not find the class keys");
        }

    }
}
