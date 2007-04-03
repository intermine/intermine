package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;

/**
 * TestCase that specific data sources can use to test that translated/converted
 * items materialise correctly in given model.
 *
 * @author Richard Smith
 */
public abstract class TargetItemsTestCase extends ItemsTestCase
{
    protected String oswAlias = null;
    protected ObjectStoreWriter osw;

    /**
     * Create a new TargetItemsTestCase object.
     * @param arg the argument to pass the to super constructor
     */
    public TargetItemsTestCase(String arg, String oswAlias) {
        super(arg);
        this.oswAlias = oswAlias;
    }

    /**
     * Store a collection of items in a Map that can be used with a MockItemReader.
     * @param items the collection of items
     * @return the item map
     * @throws Exception if anything goes wrong
     */
    protected Map writeItems(Collection items) throws Exception {
        LinkedHashMap itemMap = new LinkedHashMap();
        ItemWriter iw = new MockItemWriter(itemMap);
        Iterator i = items.iterator();
        while (i.hasNext()) {
            iw.store(ItemHelper.convert((Item) i.next()));
        }
        return itemMap;
    }

    /**
     * Get the name of the Model that the target Items should conform to
     * @return the Model name
     */
    protected abstract String getModelName();
}
