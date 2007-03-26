package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Define methods called by MageDataTranslator when processing assays.
 * Gives the opportunity for specific funtionality to be added for
 * individual experiments.
 * @author Richard Smith
 */
public interface AssayHandler
{
    /**
     * Perform any experiment specific operations for assay.
     * @param assay the assay to operate on
     * @throws ObjectStoreException if problem accessing database
     */
    public void process(Item assay) throws ObjectStoreException;

    /**
     * Return an object by which the order of an assay can be determined
     * within a particular experiment.
     * @param assay the assay in question
     * @return an object by which this assay can be ordered
     * @throws ObjectStoreException if problem accessing database
     */
    public Object getAssayOrderable(Item assay) throws ObjectStoreException;
}
