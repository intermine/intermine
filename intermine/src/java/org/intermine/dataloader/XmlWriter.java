package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.io.File;

import org.flymine.FlyMineException;

/**
 * Marshal business objects to XML.  Write either individual objects
 * or a collection of objects.
 *
 * @author Richard Smith
 */

public interface XmlWriter
{

    /**
     * Marshal a single object to the given XML file.
     *
     * @param obj a business object to marshal
     * @param file file to write XML to
     * @throws FlyMineException if anything goes wrong
     */
    public void writeXml(Object obj, File file) throws FlyMineException;


    /**
     * Marshal a collection of business objects to the given XML file.
     *
     * @param col a collection of business objects
     * @param file file to write XML to
     * @throws FlyMineException if anything goes wrong
     */
    public void writeXml(Collection col, File file) throws FlyMineException;

}
