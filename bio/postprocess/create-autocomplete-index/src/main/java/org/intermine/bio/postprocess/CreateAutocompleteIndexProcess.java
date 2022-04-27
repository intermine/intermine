package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import org.intermine.web.autocompletion.AutoCompleter;
import org.apache.tools.ant.BuildException;
import org.intermine.objectstore.ObjectStore;
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
        try {
            ObjectStore os = osw.getObjectStore();
            AutoCompleter ac = new AutoCompleter(os);
            ac.buildIndex(os);
        } catch (NullPointerException e) {
            throw new BuildException("Could not find the class keys");

        } catch (IOException e) {
            throw new BuildException("Creating autocomplete index failed", e);

        } catch (ClassNotFoundException e) {
            throw new BuildException("Creating autocomplete index failed", e);

        } catch (Exception e) {
            throw new BuildException("Creating autocomplete index failed", e);
        }

    }
}
