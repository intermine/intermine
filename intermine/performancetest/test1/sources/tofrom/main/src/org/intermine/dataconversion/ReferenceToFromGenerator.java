package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;

import org.apache.log4j.Logger;

/**
 * DataConverter to produce random tgt-item data.
 *
 * @author Matthew Wakeling
 */
public class ReferenceToFromGenerator extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(ReferenceToFromGenerator.class);

    protected ItemFactory itemFactory;
    protected Map ids = new HashMap();
    private int count;

    /**
     * Constructor.
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public ReferenceToFromGenerator(ItemWriter writer)
        throws ObjectStoreException, MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("performancetest1"), "-1_");
    }

    /**
     * Setter for object count.
     *
     * @param count the number of objects to create
     */
    public void setCount(String count) {
        this.count = Integer.parseInt(count);
    }

    /**
     * Produce random data.
     *
     * {@inheritDoc}
     */
    public void process(Reader inputFile) throws Exception {
        Random random = new Random();
        Set doneValues = new HashSet();
        long time = System.currentTimeMillis();
        for (int i = 0; i < count / 2; i++) {
            Item itemTo = createItem("ReferenceTo1");
            Item itemFrom = createItem("ReferenceFrom1");
            Integer firstInt;
            do {
                firstInt = new Integer(random.nextInt());
            } while (doneValues.contains(firstInt));
            doneValues.add(firstInt);
            itemTo.setAttribute("att", "" + firstInt);
            itemFrom.setReference("ref", itemTo.getIdentifier());
            getItemWriter().store(ItemHelper.convert(itemTo));
            getItemWriter().store(ItemHelper.convert(itemFrom));
        }
        long now = System.currentTimeMillis();
        LOG.info("Finished generating " + count + " objects at " + ((60000L * count)
                    / (now - time)) + " objects per minute (" + (now - time)
                + " ms total)");
    }

    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        ids.put(className, new Integer(id.intValue() + 1));
        return id.toString();
    }

    private Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className), className, "");
    }
}

