package org.intermine.biomart.model;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author "Xavier Watkins"
 *
 */
public class Initialiser extends Task
{

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        // TODO Read from the xml file and generate the model.
        super.execute();
    }

}
