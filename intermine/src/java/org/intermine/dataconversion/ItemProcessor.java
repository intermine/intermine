package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.xml.full.Item;

/**
 * Abstract parent of classes that handle Items output from data conversion
 * @author Mark Woodbridge
 */
public abstract class ItemProcessor
{
    /**
    * Method called before any items are processed
    * @throws Exception if an error occurs during processing
    */
    public void preProcess() throws Exception {
    }

    /**
    * Method called to process an item
    * @param item the Item to be processed
    * @throws Exception if an error occurs during processing
    */
    public abstract void process(Item item) throws Exception;

    /**
    * Method called after all items have been processed
    * @throws Exception if an error occurs during processing
    */
    public void postProcess() throws Exception {
    }
}
