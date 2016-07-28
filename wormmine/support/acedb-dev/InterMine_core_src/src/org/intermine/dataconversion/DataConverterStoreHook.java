package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.xml.full.Item;

/**
 * A hook that is called immediately before each Item is stored by the DataConverter.
 * @author Kim Rutherford
 */
public interface DataConverterStoreHook
{
    /**
     * This method is called before storing an Item.  The method is able to create new Items and
     * call dataConverter.store(), but must not call store() on the "item" argument to the method.
     * @param dataConverter the DataConverter
     * @param item the Item
     */
    void processItem(DataConverter dataConverter, Item item);
}
