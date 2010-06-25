package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.objectstore.ObjectStoreWriter;

/**
 * Output files to send to other database providers to link in to
 * FlyMine.
 *
 * @author Richard Smith
 */
public class LinkInTask extends Task
{
    /**
     * @param os objectStore
     * @throws BuildException if something goes wrong
     */
    public static void execute(ObjectStoreWriter os) {
        try {
            CreateFlyBaseLinkIns.createLinkInFile(os);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

